package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.settlement.SettlementRequest;
import com.healthcare.claimsystem.dto.settlement.SettlementResponse;
import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.ClaimSettlement;
import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.Gender;
import com.healthcare.claimsystem.entity.enums.PaymentMode;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.SettlementStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.InvalidStatusTransitionException;
import com.healthcare.claimsystem.repository.ClaimRepository;
import com.healthcare.claimsystem.repository.ClaimSettlementRepository;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.UserRepository;
import com.healthcare.claimsystem.service.impl.SettlementServiceImpl;
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
class SettlementServiceImplTest {

    @Mock
    private ClaimSettlementRepository settlementRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private InsurancePolicyRepository policyRepository;
    @Mock
    private PolicyCoverageRepository coverageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReferenceNumberGenerator referenceNumberGenerator;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    private Claim claim;
    private InsurancePolicy policy;
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

        policy = new InsurancePolicy();
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

        claim = new Claim();
        claim.setId(5L);
        claim.setClaimNumber("CLM-1");
        claim.setClaimFingerprint("fingerprint");
        claim.setPatient(patient);
        claim.setPolicy(policy);
        claim.setHospital(hospital);
        claim.setTreatmentCategory(TreatmentCategory.HOSPITALIZATION);
        claim.setClaimedAmount(new BigDecimal("10000.00"));
        claim.setApprovedAmount(new BigDecimal("8100.00"));
        claim.setPatientResponsibility(new BigDecimal("1900.00"));
        claim.setStatus(ClaimStatus.APPROVED);
    }

    @Test
    void settlesApprovedClaimAndUpdatesUtilization() {
        when(claimRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(claim));
        when(settlementRepository.existsByClaimId(5L)).thenReturn(false);
        when(settlementRepository.existsByPaymentReference("PAY-1")).thenReturn(false);
        when(policyRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(policy));
        when(coverageRepository.findByPolicyIdAndCoverageTypeForUpdate(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.of(coverage));
        when(referenceNumberGenerator.generate("STL")).thenReturn("STL-1");
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyRepository.save(any(InsurancePolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coverageRepository.save(any(PolicyCoverage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementRepository.save(any(ClaimSettlement.class))).thenAnswer(invocation -> {
            ClaimSettlement settlement = invocation.getArgument(0);
            settlement.setId(20L);
            return settlement;
        });

        SettlementResponse response = settlementService.settle(new SettlementRequest(
                5L,
                PaymentMode.BANK_TRANSFER,
                "PAY-1",
                "Settled"
        ));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo(SettlementStatus.PROCESSED);
        assertThat(response.paidAmount()).isEqualByComparingTo("8100.00");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SETTLED);
        assertThat(policy.getUtilizedAmount()).isEqualByComparingTo("8100.00");
        assertThat(coverage.getUtilizedAmount()).isEqualByComparingTo("8100.00");
    }

    @Test
    void rejectsAlreadySettledClaim() {
        claim.setStatus(ClaimStatus.SETTLED);
        when(claimRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> settlementService.settle(new SettlementRequest(
                5L,
                PaymentMode.BANK_TRANSFER,
                "PAY-2",
                null
        ))).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been settled");
    }

    @Test
    void rejectsSettlementOfUnapprovedClaim() {
        claim.setStatus(ClaimStatus.SUBMITTED);
        when(claimRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> settlementService.settle(new SettlementRequest(
                5L,
                PaymentMode.UPI,
                "PAY-3",
                null
        ))).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void rejectsSettlementExceedingRemainingCoverage() {
        policy.setUtilizedAmount(new BigDecimal("95000.00"));
        when(claimRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(claim));
        when(settlementRepository.existsByClaimId(5L)).thenReturn(false);
        when(settlementRepository.existsByPaymentReference("PAY-4")).thenReturn(false);
        when(policyRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(policy));
        when(coverageRepository.findByPolicyIdAndCoverageTypeForUpdate(2L, TreatmentCategory.HOSPITALIZATION))
                .thenReturn(Optional.of(coverage));

        assertThatThrownBy(() -> settlementService.settle(new SettlementRequest(
                5L,
                PaymentMode.CHEQUE,
                "PAY-4",
                null
        ))).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient policy coverage");
    }
}
