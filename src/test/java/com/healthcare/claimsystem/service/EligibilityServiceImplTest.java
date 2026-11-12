package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.eligibility.EligibilityRequest;
import com.healthcare.claimsystem.dto.eligibility.EligibilityResponse;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PolicyExclusion;
import com.healthcare.claimsystem.entity.enums.Gender;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.rules.AdjudicationCalculation;
import com.healthcare.claimsystem.rules.InsuranceRulesEngine;
import com.healthcare.claimsystem.service.impl.EligibilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EligibilityServiceImplTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private InsurancePolicyRepository policyRepository;
    @Mock
    private PolicyCoverageRepository coverageRepository;
    @Mock
    private PolicyExclusionRepository exclusionRepository;
    @Mock
    private InsuranceRulesEngine rulesEngine;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private EligibilityServiceImpl eligibilityService;

    private Patient patient;
    private InsurancePolicy policy;
    private PolicyCoverage coverage;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        patient = new Patient();
        patient.setId(1L);
        patient.setPatientCode("PAT-1");
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1985, 1, 1));
        patient.setGender(Gender.MALE);
        patient.setPhone("9876543210");
        patient.setActive(true);

        policy = new InsurancePolicy();
        policy.setId(2L);
        policy.setPolicyNumber("POL-1");
        policy.setPatient(patient);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(today.minusDays(100));
        policy.setEndDate(today.plusDays(200));
        policy.setSumInsured(new BigDecimal("100000.00"));
        policy.setUtilizedAmount(new BigDecimal("0.00"));
        policy.setDeductibleAmount(new BigDecimal("1000.00"));
        policy.setCoPaymentPercentage(new BigDecimal("10.00"));
        policy.setWaitingPeriodDays(30);

        coverage = new PolicyCoverage();
        coverage.setId(3L);
        coverage.setPolicy(policy);
        coverage.setCoverageType(TreatmentCategory.HOSPITALIZATION);
        coverage.setCoverageLimit(new BigDecimal("50000.00"));
        coverage.setUtilizedAmount(new BigDecimal("0.00"));
        coverage.setActive(true);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(policyRepository.findByPolicyNumber("POL-1")).thenReturn(Optional.of(policy));
        when(coverageRepository.findByPolicyIdAndCoverageType(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.of(coverage));
        when(exclusionRepository.findByPolicyIdAndTreatmentCategory(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.empty());
        when(rulesEngine.calculate(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(new AdjudicationCalculation(
                        new BigDecimal("10000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("900.00"),
                        new BigDecimal("8100.00"),
                        new BigDecimal("1900.00"),
                        true,
                        List.of()
                ));
    }

    @Test
    void returnsEligibleForActivePolicy() {
        EligibilityResponse response = eligibilityService.verify(request(today, new BigDecimal("10000.00")));

        assertThat(response.eligible()).isTrue();
        assertThat(response.reasons()).isEmpty();
        assertThat(response.remainingCoverage()).isEqualByComparingTo("100000.00");
        assertThat(response.categoryRemaining()).isEqualByComparingTo("50000.00");
        assertThat(response.estimatedInsurerPayable()).isEqualByComparingTo("8100.00");
        assertThat(response.estimatedPatientResponsibility()).isEqualByComparingTo("1900.00");
    }

    @Test
    void returnsIneligibleWhenPolicyIsExpired() {
        policy.setStatus(PolicyStatus.EXPIRED);

        EligibilityResponse response = eligibilityService.verify(request(today, null));

        assertThat(response.eligible()).isFalse();
        assertThat(response.reasons()).anyMatch(reason -> reason.contains("Policy status is expired"));
    }

    @Test
    void returnsIneligibleWhenCategoryIsExcluded() {
        PolicyExclusion exclusion = new PolicyExclusion();
        exclusion.setId(4L);
        exclusion.setPolicy(policy);
        exclusion.setTreatmentCategory(TreatmentCategory.HOSPITALIZATION);
        exclusion.setReason("Hospitalization is excluded");
        when(exclusionRepository.findByPolicyIdAndTreatmentCategory(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.of(exclusion));

        EligibilityResponse response = eligibilityService.verify(request(today, null));

        assertThat(response.eligible()).isFalse();
        assertThat(response.reasons()).contains("Hospitalization is excluded");
    }

    @Test
    void returnsIneligibleDuringWaitingPeriod() {
        EligibilityResponse response = eligibilityService.verify(request(policy.getStartDate().plusDays(10), null));

        assertThat(response.eligible()).isFalse();
        assertThat(response.reasons()).anyMatch(reason -> reason.contains("waiting period"));
    }

    @Test
    void rejectsPolicyOwnershipMismatch() {
        Patient otherPatient = new Patient();
        otherPatient.setId(99L);
        otherPatient.setPatientCode("PAT-99");
        otherPatient.setFirstName("Jane");
        otherPatient.setLastName("Doe");
        otherPatient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        otherPatient.setGender(Gender.FEMALE);
        otherPatient.setPhone("9876543211");
        otherPatient.setActive(true);
        when(patientRepository.findById(99L)).thenReturn(Optional.of(otherPatient));

        EligibilityRequest request = new EligibilityRequest(
                99L,
                "POL-1",
                TreatmentCategory.HOSPITALIZATION,
                null,
                today
        );

        assertThatThrownBy(() -> eligibilityService.verify(request))
                .hasMessageContaining("does not belong");
    }

    private EligibilityRequest request(LocalDate serviceDate, BigDecimal estimatedCost) {
        return new EligibilityRequest(
                1L,
                "POL-1",
                TreatmentCategory.HOSPITALIZATION,
                estimatedCost,
                serviceDate
        );
    }
}
