package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationDecisionRequest;
import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationResponse;
import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PreAuthorization;
import com.healthcare.claimsystem.entity.enums.Gender;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationDecision;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.InvalidStatusTransitionException;
import com.healthcare.claimsystem.repository.HospitalRepository;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.repository.PreAuthorizationRepository;
import com.healthcare.claimsystem.repository.UserRepository;
import com.healthcare.claimsystem.service.impl.PreAuthorizationServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreAuthorizationServiceImplTest {

    @Mock
    private PreAuthorizationRepository preAuthorizationRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private InsurancePolicyRepository policyRepository;
    @Mock
    private HospitalRepository hospitalRepository;
    @Mock
    private PolicyCoverageRepository coverageRepository;
    @Mock
    private PolicyExclusionRepository exclusionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReferenceNumberGenerator referenceNumberGenerator;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PreAuthorizationServiceImpl preAuthorizationService;

    private PreAuthorization preAuthorization;
    private PolicyCoverage coverage;

    @BeforeEach
    void setUp() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setPatientCode("PAT-1");
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1985, 1, 1));
        patient.setGender(Gender.MALE);
        patient.setPhone("9876543210");
        patient.setActive(true);

        InsurancePolicy policy = new InsurancePolicy();
        policy.setId(2L);
        policy.setPolicyNumber("POL-1");
        policy.setPatient(patient);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(LocalDate.now().minusDays(100));
        policy.setEndDate(LocalDate.now().plusDays(200));
        policy.setSumInsured(new BigDecimal("100000.00"));
        policy.setUtilizedAmount(new BigDecimal("0.00"));
        policy.setDeductibleAmount(new BigDecimal("1000.00"));
        policy.setCoPaymentPercentage(new BigDecimal("10.00"));
        policy.setWaitingPeriodDays(30);

        Hospital hospital = new Hospital();
        hospital.setId(3L);
        hospital.setHospitalCode("HSP-1");
        hospital.setName("City Hospital");
        hospital.setLicenseNumber("LIC-1");
        hospital.setEmail("hospital@example.com");
        hospital.setPhone("9876543211");
        hospital.setAddress("Address");
        hospital.setContactPerson("Contact");
        hospital.setActive(true);

        coverage = new PolicyCoverage();
        coverage.setId(4L);
        coverage.setPolicy(policy);
        coverage.setCoverageType(TreatmentCategory.HOSPITALIZATION);
        coverage.setCoverageLimit(new BigDecimal("50000.00"));
        coverage.setUtilizedAmount(new BigDecimal("0.00"));
        coverage.setActive(true);

        preAuthorization = new PreAuthorization();
        preAuthorization.setId(5L);
        preAuthorization.setReferenceNumber("PA-1");
        preAuthorization.setPatient(patient);
        preAuthorization.setPolicy(policy);
        preAuthorization.setHospital(hospital);
        preAuthorization.setTreatingDoctor("Dr. Smith");
        preAuthorization.setDiagnosisCode("J18.9");
        preAuthorization.setProcedureCode("PROC-1");
        preAuthorization.setTreatmentCategory(TreatmentCategory.HOSPITALIZATION);
        preAuthorization.setTreatmentDescription("Treatment");
        preAuthorization.setEstimatedCost(new BigDecimal("10000.00"));
        preAuthorization.setRequestedDate(LocalDate.now());
        preAuthorization.setStatus(PreAuthorizationStatus.PENDING);

        when(preAuthorizationRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(preAuthorization));
        when(preAuthorizationRepository.save(any(PreAuthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(coverageRepository.findByPolicyIdAndCoverageType(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.of(coverage));
        when(exclusionRepository.findByPolicyIdAndTreatmentCategory(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.empty());
    }

    @Test
    void approvesPreAuthorizationWithEstimatedCostWhenAmountIsOmitted() {
        PreAuthorizationResponse response = preAuthorizationService.decide(
                5L,
                new PreAuthorizationDecisionRequest(
                        PreAuthorizationDecision.APPROVE,
                        null,
                        "Approved",
                        LocalDate.now().plusDays(30)
                )
        );

        assertThat(response.status()).isEqualTo(PreAuthorizationStatus.APPROVED);
        assertThat(response.approvedAmount()).isEqualByComparingTo("10000.00");
        assertThat(response.validUntil()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(response.decisionDate()).isNotNull();
    }

    @Test
    void rejectsPartialApprovalWithoutAmount() {
        assertThatThrownBy(() -> preAuthorizationService.decide(
                5L,
                new PreAuthorizationDecisionRequest(
                        PreAuthorizationDecision.PARTIAL_APPROVE,
                        null,
                        "Partially approved",
                        LocalDate.now().plusDays(30)
                )
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Approved amount is required");
    }

    @Test
    void rejectsPartialApprovalNotBelowEstimatedCost() {
        assertThatThrownBy(() -> preAuthorizationService.decide(
                5L,
                new PreAuthorizationDecisionRequest(
                        PreAuthorizationDecision.PARTIAL_APPROVE,
                        new BigDecimal("10000.00"),
                        "Partially approved",
                        LocalDate.now().plusDays(30)
                )
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("less than the estimated cost");
    }

    @Test
    void rejectsRejectionWithoutRemarks() {
        assertThatThrownBy(() -> preAuthorizationService.decide(
                5L,
                new PreAuthorizationDecisionRequest(
                        PreAuthorizationDecision.REJECT,
                        null,
                        " ",
                        null
                )
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void rejectsDecisionOnAlreadyDecidedRequest() {
        preAuthorization.setStatus(PreAuthorizationStatus.APPROVED);

        assertThatThrownBy(() -> preAuthorizationService.decide(
                5L,
                new PreAuthorizationDecisionRequest(
                        PreAuthorizationDecision.APPROVE,
                        null,
                        null,
                        LocalDate.now().plusDays(30)
                )
        )).isInstanceOf(InvalidStatusTransitionException.class);
    }
}
