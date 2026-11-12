package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.policy.CoverageRequest;
import com.healthcare.claimsystem.dto.policy.PolicyRenewRequest;
import com.healthcare.claimsystem.dto.policy.PolicyResponse;
import com.healthcare.claimsystem.dto.policy.PolicyStatusRequest;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.enums.Gender;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.DuplicateResourceException;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.service.impl.PolicyServiceImpl;
import com.healthcare.claimsystem.util.ReferenceNumberGenerator;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PolicyServiceImplTest {

    @Mock
    private InsurancePolicyRepository policyRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PolicyCoverageRepository coverageRepository;
    @Mock
    private PolicyExclusionRepository exclusionRepository;
    @Mock
    private ReferenceNumberGenerator referenceNumberGenerator;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PolicyServiceImpl policyService;

    private InsurancePolicy policy;
    private Patient patient;

    @BeforeEach
    void setUp() {
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
        policy.setProviderName("Provider");
        policy.setPolicyType(PolicyType.INDIVIDUAL);
        policy.setStatus(PolicyStatus.EXPIRED);
        policy.setStartDate(LocalDate.now().minusDays(400));
        policy.setEndDate(LocalDate.now().minusDays(1));
        policy.setSumInsured(new BigDecimal("100000.00"));
        policy.setUtilizedAmount(new BigDecimal("0.00"));
        policy.setDeductibleAmount(new BigDecimal("1000.00"));
        policy.setCoPaymentPercentage(new BigDecimal("10.00"));
        policy.setWaitingPeriodDays(30);
        policy.setPremiumAmount(new BigDecimal("10000.00"));
        policy.setRenewalCount(0);

        when(policyRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(InsurancePolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coverageRepository.findByPolicyIdOrderByCoverageTypeAsc(2L)).thenReturn(List.of());
        when(exclusionRepository.findByPolicyIdOrderByTreatmentCategoryAsc(2L)).thenReturn(List.of());
    }

    @Test
    void renewsExpiredPolicyAndReactivatesIt() {
        LocalDate newEndDate = LocalDate.now().plusDays(365);

        PolicyResponse response = policyService.renew(2L, new PolicyRenewRequest(
                newEndDate,
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00")
        ));

        assertThat(response.status()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(response.endDate()).isEqualTo(newEndDate);
        assertThat(response.sumInsured()).isEqualByComparingTo("105000.00");
        assertThat(response.premiumAmount()).isEqualByComparingTo("12000.00");
        assertThat(response.renewalCount()).isEqualTo(1);
        assertThat(response.lastRenewedAt()).isNotNull();
    }

    @Test
    void rejectsCancellationWithoutReason() {
        assertThatThrownBy(() -> policyService.changeStatus(2L, new PolicyStatusRequest(PolicyStatus.CANCELLED, " ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void rejectsActivationOfExpiredPolicyWithoutRenewal() {
        assertThatThrownBy(() -> policyService.changeStatus(2L, new PolicyStatusRequest(PolicyStatus.ACTIVE, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("renewed");
    }

    @Test
    void rejectsDuplicateCoverage() {
        when(policyRepository.findById(2L)).thenReturn(Optional.of(policy));
        when(coverageRepository.existsByPolicyIdAndCoverageType(2L, TreatmentCategory.SURGERY)).thenReturn(true);

        assertThatThrownBy(() -> policyService.addCoverage(2L, new CoverageRequest(
                TreatmentCategory.SURGERY,
                new BigDecimal("10000.00"),
                true
        ))).isInstanceOf(DuplicateResourceException.class);
    }
}
