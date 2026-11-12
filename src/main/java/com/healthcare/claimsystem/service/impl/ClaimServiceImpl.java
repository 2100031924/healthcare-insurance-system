package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.audit.AuditLogResponse;
import com.healthcare.claimsystem.dto.claim.AdjudicationPreviewResponse;
import com.healthcare.claimsystem.dto.claim.ClaimAdjudicationRequest;
import com.healthcare.claimsystem.dto.claim.ClaimRequest;
import com.healthcare.claimsystem.dto.claim.ClaimResponse;
import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PolicyExclusion;
import com.healthcare.claimsystem.entity.PreAuthorization;
import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.entity.enums.AdjudicationDecision;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.DuplicateResourceException;
import com.healthcare.claimsystem.exception.InvalidStatusTransitionException;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.ClaimMapper;
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
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.service.ClaimService;
import com.healthcare.claimsystem.specification.ClaimSpecifications;
import com.healthcare.claimsystem.util.ClaimFingerprint;
import com.healthcare.claimsystem.util.PageableUtils;
import com.healthcare.claimsystem.util.ReferenceNumberGenerator;
import com.healthcare.claimsystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimServiceImpl implements ClaimService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "claimNumber", "claimDate", "admissionDate", "claimedAmount", "approvedAmount", "status"
    );
    private static final Set<ClaimStatus> DECISION_ALLOWED_STATUSES = Set.of(
            ClaimStatus.SUBMITTED,
            ClaimStatus.UNDER_REVIEW
    );

    private final ClaimRepository claimRepository;
    private final PatientRepository patientRepository;
    private final InsurancePolicyRepository policyRepository;
    private final HospitalRepository hospitalRepository;
    private final PreAuthorizationRepository preAuthorizationRepository;
    private final PolicyCoverageRepository coverageRepository;
    private final PolicyExclusionRepository exclusionRepository;
    private final UserRepository userRepository;
    private final InsuranceRulesEngine rulesEngine;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final AuditService auditService;

    @Override
    @Transactional
    public ClaimResponse submit(ClaimRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));
        InsurancePolicy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", request.policyId()));
        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", "id", request.hospitalId()));

        validateParties(patient, policy, hospital);
        validateDates(request, policy);
        PreAuthorization preAuthorization = resolvePreAuthorization(request);

        String invoiceNumber = request.invoiceNumber().trim();
        if (claimRepository.existsByHospitalIdAndInvoiceNumber(hospital.getId(), invoiceNumber)) {
            throw new DuplicateResourceException("A claim with invoice number " + invoiceNumber + " already exists");
        }
        String fingerprint = ClaimFingerprint.generate(
                patient.getId(),
                policy.getId(),
                hospital.getId(),
                request.procedureCode(),
                request.admissionDate(),
                request.dischargeDate(),
                request.claimedAmount()
        );
        Optional<Claim> existing = claimRepository.findByClaimFingerprint(fingerprint);
        if (existing.isPresent()) {
            throw new DuplicateResourceException("Duplicate claim detected. Existing claim: " + existing.get().getClaimNumber());
        }

        Claim claim = new Claim();
        claim.setClaimNumber(referenceNumberGenerator.generate("CLM"));
        claim.setClaimFingerprint(fingerprint);
        claim.setPatient(patient);
        claim.setPolicy(policy);
        claim.setHospital(hospital);
        claim.setPreAuthorization(preAuthorization);
        claim.setTreatingDoctor(request.treatingDoctor().trim());
        claim.setDiagnosisCode(request.diagnosisCode().trim());
        claim.setProcedureCode(request.procedureCode().trim());
        claim.setTreatmentCategory(request.treatmentCategory());
        claim.setAdmissionDate(request.admissionDate());
        claim.setDischargeDate(request.dischargeDate());
        claim.setClaimDate(request.claimDate());
        claim.setClaimedAmount(request.claimedAmount());
        claim.setInvoiceNumber(invoiceNumber);
        claim.setDescription(request.description() == null ? null : request.description().trim());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setDeductibleApplied(BigDecimal.ZERO.setScale(2));
        claim.setCoPaymentAmount(BigDecimal.ZERO.setScale(2));
        claim.setNonCoveredAmount(BigDecimal.ZERO.setScale(2));
        claim.setApprovedAmount(BigDecimal.ZERO.setScale(2));
        claim.setPatientResponsibility(BigDecimal.ZERO.setScale(2));
        claim.setSubmittedBy(currentUser());

        Claim saved;
        try {
            saved = claimRepository.saveAndFlush(claim);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("A claim with the same invoice or service details already exists");
        }

        auditService.recordClaim(
                saved,
                AuditAction.CLAIM_SUBMITTED,
                "Claim " + saved.getClaimNumber() + " submitted for patient " + patient.getPatientCode()
                        + " with amount " + saved.getClaimedAmount()
        );
        return ClaimMapper.toResponse(saved);
    }

    @Override
    public ClaimResponse get(Long id) {
        return ClaimMapper.toResponse(findClaim(id));
    }

    @Override
    public Page<ClaimResponse> list(
            ClaimStatus status,
            Long patientId,
            Long hospitalId,
            String policyNumber,
            String claimNumber,
            TreatmentCategory treatmentCategory,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return claimRepository.findAll(
                ClaimSpecifications.withFilters(
                        status,
                        patientId,
                        hospitalId,
                        policyNumber,
                        claimNumber,
                        treatmentCategory,
                        fromDate,
                        toDate
                ),
                sanitized
        ).map(ClaimMapper::toResponse);
    }

    @Override
    @Transactional
    public ClaimResponse review(Long id) {
        Claim claim = findClaimForUpdate(id);
        if (claim.getStatus() != ClaimStatus.SUBMITTED) {
            throw new InvalidStatusTransitionException("Claim", claim.getStatus(), ClaimStatus.UNDER_REVIEW);
        }
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setReviewedBy(currentUser());
        claim.setReviewedAt(Instant.now());
        Claim saved = claimRepository.save(claim);
        auditService.recordClaim(
                saved,
                AuditAction.CLAIM_REVIEWED,
                "Claim " + saved.getClaimNumber() + " moved to review"
        );
        return ClaimMapper.toResponse(saved);
    }

    @Override
    public AdjudicationPreviewResponse previewAdjudication(Long id) {
        Claim claim = findClaim(id);
        if (!DECISION_ALLOWED_STATUSES.contains(claim.getStatus())) {
            throw new BusinessRuleException("Adjudication preview is only available before a decision is made");
        }
        AdjudicationCalculation calculation = calculate(claim);
        return new AdjudicationPreviewResponse(
                claim.getId(),
                claim.getClaimNumber(),
                calculation.claimedAmount(),
                calculation.eligibleAmount(),
                calculation.nonCoveredAmount(),
                calculation.deductibleApplied(),
                calculation.coPaymentAmount(),
                calculation.approvedAmount(),
                calculation.patientResponsibility(),
                calculation.covered(),
                calculation.remarks()
        );
    }

    @Override
    @Transactional
    public ClaimResponse adjudicate(Long id, ClaimAdjudicationRequest request) {
        Claim claim = findClaimForUpdate(id);
        if (!DECISION_ALLOWED_STATUSES.contains(claim.getStatus())) {
            throw new InvalidStatusTransitionException("Claim", claim.getStatus(), "ADJUDICATED");
        }
        AdjudicationCalculation calculation = calculate(claim);
        BigDecimal approvedAmount;
        ClaimStatus targetStatus;

        switch (request.decision()) {
            case APPROVE -> {
                if (!calculation.covered() || calculation.approvedAmount().signum() <= 0) {
                    throw new BusinessRuleException(
                            "Claim is not payable: " + String.join("; ", calculation.remarks())
                    );
                }
                approvedAmount = calculation.approvedAmount();
                targetStatus = ClaimStatus.APPROVED;
            }
            case PARTIAL_APPROVE -> {
                if (!calculation.covered()) {
                    throw new BusinessRuleException(
                            "Claim is not payable: " + String.join("; ", calculation.remarks())
                    );
                }
                if (request.approvedAmount() == null || request.approvedAmount().signum() <= 0) {
                    throw new BusinessRuleException("Approved amount is required for partial approval");
                }
                if (request.approvedAmount().compareTo(claim.getClaimedAmount()) >= 0) {
                    throw new BusinessRuleException("Partial approval amount must be less than the claimed amount");
                }
                if (request.approvedAmount().compareTo(calculation.approvedAmount()) > 0) {
                    throw new BusinessRuleException(
                            "Approved amount cannot exceed the maximum payable amount of "
                                    + calculation.approvedAmount().toPlainString()
                    );
                }
                approvedAmount = request.approvedAmount();
                targetStatus = ClaimStatus.PARTIALLY_APPROVED;
            }
            case REJECT -> {
                if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
                    throw new BusinessRuleException("A rejection reason is required");
                }
                approvedAmount = BigDecimal.ZERO.setScale(2);
                targetStatus = ClaimStatus.REJECTED;
            }
            default -> throw new BusinessRuleException("Unsupported adjudication decision");
        }

        Instant now = Instant.now();
        User officer = currentUser();
        if (claim.getReviewedBy() == null) {
            claim.setReviewedBy(officer);
            claim.setReviewedAt(now);
        }
        claim.setStatus(targetStatus);
        claim.setAdjudicatedBy(officer);
        claim.setAdjudicatedAt(now);
        claim.setAdjudicationNotes(request.notes() == null ? null : request.notes().trim());
        claim.setApprovedAmount(approvedAmount);
        claim.setPatientResponsibility(claim.getClaimedAmount().subtract(approvedAmount).max(BigDecimal.ZERO));
        if (targetStatus == ClaimStatus.REJECTED) {
            claim.setDeductibleApplied(BigDecimal.ZERO.setScale(2));
            claim.setCoPaymentAmount(BigDecimal.ZERO.setScale(2));
            claim.setNonCoveredAmount(claim.getClaimedAmount());
            claim.setRejectionReason(request.rejectionReason().trim());
        } else {
            claim.setDeductibleApplied(calculation.deductibleApplied());
            claim.setCoPaymentAmount(calculation.coPaymentAmount());
            claim.setNonCoveredAmount(calculation.nonCoveredAmount());
            claim.setRejectionReason(null);
        }

        Claim saved = claimRepository.save(claim);
        auditService.recordClaim(
                saved,
                AuditAction.CLAIM_ADJUDICATED,
                "Claim " + saved.getClaimNumber() + " adjudicated as " + saved.getStatus()
                        + " with approved amount " + saved.getApprovedAmount()
        );
        return ClaimMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ClaimResponse cancel(Long id) {
        Claim claim = findClaimForUpdate(id);
        if (!DECISION_ALLOWED_STATUSES.contains(claim.getStatus())) {
            throw new InvalidStatusTransitionException("Claim", claim.getStatus(), ClaimStatus.CANCELLED);
        }
        claim.setStatus(ClaimStatus.CANCELLED);
        Claim saved = claimRepository.save(claim);
        auditService.recordClaim(
                saved,
                AuditAction.CLAIM_CANCELLED,
                "Claim " + saved.getClaimNumber() + " cancelled"
        );
        return ClaimMapper.toResponse(saved);
    }

    @Override
    public Page<AuditLogResponse> getAudit(Long id, Pageable pageable) {
        findClaim(id);
        return auditService.getClaimAudit(id, pageable);
    }

    private void validateParties(Patient patient, InsurancePolicy policy, Hospital hospital) {
        if (!policy.getPatient().getId().equals(patient.getId())) {
            throw new BusinessRuleException("Policy does not belong to the specified patient");
        }
        if (!patient.isActive()) {
            throw new BusinessRuleException("Patient is inactive");
        }
        if (!hospital.isActive()) {
            throw new BusinessRuleException("Hospital is inactive");
        }
        if (policy.getStatus() == PolicyStatus.CANCELLED || policy.getStatus() == PolicyStatus.SUSPENDED) {
            throw new BusinessRuleException("Policy is " + policy.getStatus().name().toLowerCase());
        }
    }

    private void validateDates(ClaimRequest request, InsurancePolicy policy) {
        if (request.dischargeDate() != null && request.dischargeDate().isBefore(request.admissionDate())) {
            throw new BusinessRuleException("Discharge date must not be before the admission date");
        }
        if (request.claimDate().isBefore(request.admissionDate())) {
            throw new BusinessRuleException("Claim date must not be before the admission date");
        }
        if (request.dischargeDate() != null && request.claimDate().isBefore(request.dischargeDate())) {
            throw new BusinessRuleException("Claim date must not be before the discharge date");
        }
        LocalDate serviceEnd = request.dischargeDate() == null ? request.admissionDate() : request.dischargeDate();
        if (request.admissionDate().isBefore(policy.getStartDate()) || serviceEnd.isAfter(policy.getEndDate())) {
            throw new BusinessRuleException("Service dates are outside the policy period");
        }
    }

    private PreAuthorization resolvePreAuthorization(ClaimRequest request) {
        if (request.preAuthorizationId() == null) {
            return null;
        }
        PreAuthorization preAuthorization = preAuthorizationRepository.findById(request.preAuthorizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pre-authorization",
                        "id",
                        request.preAuthorizationId()
                ));
        if (!preAuthorization.getPatient().getId().equals(request.patientId())) {
            throw new BusinessRuleException("Pre-authorization does not belong to the specified patient");
        }
        if (!preAuthorization.getPolicy().getId().equals(request.policyId())) {
            throw new BusinessRuleException("Pre-authorization does not belong to the specified policy");
        }
        if (!preAuthorization.getHospital().getId().equals(request.hospitalId())) {
            throw new BusinessRuleException("Pre-authorization does not belong to the specified hospital");
        }
        if (preAuthorization.getTreatmentCategory() != request.treatmentCategory()) {
            throw new BusinessRuleException("Pre-authorization treatment category does not match the claim");
        }
        if (preAuthorization.getStatus() != PreAuthorizationStatus.APPROVED
                && preAuthorization.getStatus() != PreAuthorizationStatus.PARTIALLY_APPROVED) {
            throw new BusinessRuleException("Pre-authorization is not approved");
        }
        if (preAuthorization.getValidUntil() == null
                || preAuthorization.getValidUntil().isBefore(request.admissionDate())) {
            throw new BusinessRuleException("Pre-authorization is not valid for the admission date");
        }
        if (preAuthorization.getApprovedAmount() == null || preAuthorization.getApprovedAmount().signum() <= 0) {
            throw new BusinessRuleException("Pre-authorization has no approved amount");
        }
        return preAuthorization;
    }

    private AdjudicationCalculation calculate(Claim claim) {
        InsurancePolicy policy = claim.getPolicy();
        PolicyCoverage coverage = coverageRepository
                .findByPolicyIdAndCoverageType(policy.getId(), claim.getTreatmentCategory())
                .orElse(null);
        Optional<PolicyExclusion> exclusion = exclusionRepository
                .findByPolicyIdAndTreatmentCategory(policy.getId(), claim.getTreatmentCategory());
        BigDecimal preAuthorizationLimit = null;
        PreAuthorization preAuthorization = claim.getPreAuthorization();
        if (preAuthorization != null) {
            boolean valid = (preAuthorization.getStatus() == PreAuthorizationStatus.APPROVED
                    || preAuthorization.getStatus() == PreAuthorizationStatus.PARTIALLY_APPROVED)
                    && preAuthorization.getValidUntil() != null
                    && !preAuthorization.getValidUntil().isBefore(claim.getAdmissionDate())
                    && preAuthorization.getApprovedAmount() != null
                    && preAuthorization.getApprovedAmount().signum() > 0;
            preAuthorizationLimit = valid ? preAuthorization.getApprovedAmount() : BigDecimal.ZERO.setScale(2);
        }
        return rulesEngine.calculate(
                claim.getClaimedAmount(),
                claim.getAdmissionDate(),
                policy,
                coverage,
                exclusion.isPresent(),
                exclusion.map(PolicyExclusion::getReason).orElse(null),
                preAuthorizationLimit
        );
    }

    private Claim findClaim(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", id));
    }

    private Claim findClaimForUpdate(Long id) {
        return claimRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", id));
    }

    private User currentUser() {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }
}
