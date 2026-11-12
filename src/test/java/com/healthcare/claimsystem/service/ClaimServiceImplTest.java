package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.claim.ClaimAdjudicationRequest;
import com.healthcare.claimsystem.dto.claim.ClaimRequest;
import com.healthcare.claimsystem.dto.claim.ClaimResponse;
import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.enums.AdjudicationDecision;
import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.Gender;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.DuplicateResourceException;
import com.healthcare.claimsystem.exception.InvalidStatusTransitionException;
import com.healthcare.claimsystem.repository.ClaimRepository;
import com.healthcare.claimsystem.repository.HospitalRepository;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.repository.PreAuthorizationRepository;
import com.healthcare.claimsystem.repository.UserRepository;
import com.healthcare.claimsystem.rules.AdjudicationCalculation;
import com.healthcare.claimsystem.rules.InsuranceRulesEngine;
import com.healthcare.claimsystem.service.impl.ClaimServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private InsurancePolicyRepository policyRepository;
    @Mock
    private HospitalRepository hospitalRepository;
    @Mock
    private PreAuthorizationRepository preAuthorizationRepository;
    @Mock
    private PolicyCoverageRepository coverageRepository;
    @Mock
    private PolicyExclusionRepository exclusionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InsuranceRulesEngine rulesEngine;
    @Mock
    private ReferenceNumberGenerator referenceNumberGenerator;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ClaimServiceImpl claimService;

    private Patient patient;
    private InsurancePolicy policy;
    private Hospital hospital;
    private PolicyCoverage coverage;
    private Claim claim;
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

        hospital = new Hospital();
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

        claim = new Claim();
        claim.setId(10L);
        claim.setClaimNumber("CLM-1");
        claim.setClaimFingerprint("fingerprint");
        claim.setPatient(patient);
        claim.setPolicy(policy);
        claim.setHospital(hospital);
        claim.setTreatingDoctor("Dr. Smith");
        claim.setDiagnosisCode("J18.9");
        claim.setProcedureCode("PROC-1");
        claim.setTreatmentCategory(TreatmentCategory.HOSPITALIZATION);
        claim.setAdmissionDate(today.minusDays(10));
        claim.setDischargeDate(today.minusDays(9));
        claim.setClaimDate(today.minusDays(8));
        claim.setClaimedAmount(new BigDecimal("10000.00"));
        claim.setInvoiceNumber("INV-1");
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setDeductibleApplied(new BigDecimal("0.00"));
        claim.setCoPaymentAmount(new BigDecimal("0.00"));
        claim.setNonCoveredAmount(new BigDecimal("0.00"));
        claim.setApprovedAmount(new BigDecimal("0.00"));
        claim.setPatientResponsibility(new BigDecimal("0.00"));

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(policyRepository.findById(2L)).thenReturn(Optional.of(policy));
        when(hospitalRepository.findById(3L)).thenReturn(Optional.of(hospital));
    }

    @Test
    void rejectsDuplicateClaimFingerprint() {
        Claim existing = new Claim();
        existing.setId(99L);
        existing.setClaimNumber("CLM-EXISTING");
        when(claimRepository.existsByHospitalIdAndInvoiceNumber(3L, "INV-2")).thenReturn(false);
        when(claimRepository.findByClaimFingerprint(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> claimService.submit(requestWithInvoice("INV-2")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CLM-EXISTING");
        verify(claimRepository, never()).saveAndFlush(any(Claim.class));
    }

    @Test
    void submitsValidClaim() {
        when(claimRepository.existsByHospitalIdAndInvoiceNumber(3L, "INV-2")).thenReturn(false);
        when(claimRepository.findByClaimFingerprint(anyString())).thenReturn(Optional.empty());
        when(referenceNumberGenerator.generate("CLM")).thenReturn("CLM-NEW");
        when(claimRepository.saveAndFlush(any(Claim.class))).thenAnswer(invocation -> {
            Claim saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        ClaimResponse response = claimService.submit(requestWithInvoice("INV-2"));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.claimNumber()).isEqualTo("CLM-NEW");
        assertThat(response.status()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.claimedAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void rejectsReviewOfAlreadyAdjudicatedClaim() {
        claim.setStatus(ClaimStatus.APPROVED);
        when(claimRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.review(10L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void approvesClaimUsingRulesEngineCalculation() {
        when(claimRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(claim));
        when(coverageRepository.findByPolicyIdAndCoverageType(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.of(coverage));
        when(exclusionRepository.findByPolicyIdAndTreatmentCategory(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.empty());
        when(rulesEngine.calculate(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(calculation());
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimResponse response = claimService.adjudicate(
                10L,
                new ClaimAdjudicationRequest(AdjudicationDecision.APPROVE, null, null, "Approved")
        );

        assertThat(response.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(response.approvedAmount()).isEqualByComparingTo("8100.00");
        assertThat(response.patientResponsibility()).isEqualByComparingTo("1900.00");
        assertThat(response.deductibleApplied()).isEqualByComparingTo("1000.00");
        assertThat(response.coPaymentAmount()).isEqualByComparingTo("900.00");
        assertThat(claim.getAdjudicatedAt()).isNotNull();
    }

    @Test
    void rejectsPartialApprovalAboveCalculatedMaximum() {
        when(claimRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(claim));
        when(coverageRepository.findByPolicyIdAndCoverageType(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.of(coverage));
        when(exclusionRepository.findByPolicyIdAndTreatmentCategory(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.empty());
        when(rulesEngine.calculate(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(calculation());

        assertThatThrownBy(() -> claimService.adjudicate(
                10L,
                new ClaimAdjudicationRequest(
                        AdjudicationDecision.PARTIAL_APPROVE,
                        new BigDecimal("9000.00"),
                        null,
                        null
                )
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("8100.00");
    }

    @Test
    void rejectsRejectionWithoutReason() {
        when(claimRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(claim));
        when(coverageRepository.findByPolicyIdAndCoverageType(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.of(coverage));
        when(exclusionRepository.findByPolicyIdAndTreatmentCategory(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.empty());
        when(rulesEngine.calculate(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(calculation());

        assertThatThrownBy(() -> claimService.adjudicate(
                10L,
                new ClaimAdjudicationRequest(AdjudicationDecision.REJECT, null, " ", null)
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rejection reason");
    }

    @Test
    void rejectsCancellationAfterAdjudication() {
        claim.setStatus(ClaimStatus.APPROVED);
        when(claimRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.cancel(10L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    private ClaimRequest requestWithInvoice(String invoiceNumber) {
        return new ClaimRequest(
                1L,
                2L,
                3L,
                null,
                "Dr. Smith",
                "J18.9",
                "PROC-1",
                TreatmentCategory.HOSPITALIZATION,
                today.minusDays(10),
                today.minusDays(9),
                today.minusDays(8),
                new BigDecimal("10000.00"),
                invoiceNumber,
                "Treatment description"
        );
    }

    private AdjudicationCalculation calculation() {
        return new AdjudicationCalculation(
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("900.00"),
                new BigDecimal("8100.00"),
                new BigDecimal("1900.00"),
                true,
                List.of()
        );
    }
}
