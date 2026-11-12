package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationDecisionRequest;
import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationRequest;
import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationResponse;
import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PolicyExclusion;
import com.healthcare.claimsystem.entity.PreAuthorization;
import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationDecision;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.DuplicateResourceException;
import com.healthcare.claimsystem.exception.InvalidStatusTransitionException;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.PreAuthorizationMapper;
import com.healthcare.claimsystem.repository.HospitalRepository;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.repository.PreAuthorizationRepository;
import com.healthcare.claimsystem.repository.UserRepository;
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.service.PreAuthorizationService;
import com.healthcare.claimsystem.specification.PreAuthorizationSpecifications;
import com.healthcare.claimsystem.util.PageableUtils;
import com.healthcare.claimsystem.util.ReferenceNumberGenerator;
import com.healthcare.claimsystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreAuthorizationServiceImpl implements PreAuthorizationService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "referenceNumber", "requestedDate", "estimatedCost", "approvedAmount", "status"
    );
    private static final List<PreAuthorizationStatus> ACTIVE_STATUSES = List.of(
            PreAuthorizationStatus.PENDING,
            PreAuthorizationStatus.UNDER_REVIEW
    );

    private final PreAuthorizationRepository preAuthorizationRepository;
    private final PatientRepository patientRepository;
    private final InsurancePolicyRepository policyRepository;
    private final HospitalRepository hospitalRepository;
    private final PolicyCoverageRepository coverageRepository;
    private final PolicyExclusionRepository exclusionRepository;
    private final UserRepository userRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final AuditService auditService;

    @Override
    @Transactional
    public PreAuthorizationResponse request(PreAuthorizationRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));
        InsurancePolicy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", request.policyId()));
        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", "id", request.hospitalId()));

        if (!policy.getPatient().getId().equals(patient.getId())) {
            throw new BusinessRuleException("Policy does not belong to the specified patient");
        }
        if (!patient.isActive()) {
            throw new BusinessRuleException("Patient is inactive");
        }
        if (!hospital.isActive()) {
            throw new BusinessRuleException("Hospital is inactive");
        }
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BusinessRuleException("Policy is not active");
        }
        if (request.requestedDate().isBefore(policy.getStartDate()) || request.requestedDate().isAfter(policy.getEndDate())) {
            throw new BusinessRuleException("Requested date is outside the policy period");
        }
        LocalDate waitingPeriodEnd = policy.getStartDate().plusDays(policy.getWaitingPeriodDays());
        if (request.requestedDate().isBefore(waitingPeriodEnd)) {
            throw new BusinessRuleException("Policy waiting period ends on " + waitingPeriodEnd);
        }
        PolicyCoverage coverage = coverageRepository
                .findByPolicyIdAndCoverageType(policy.getId(), request.treatmentCategory())
                .orElseThrow(() -> new BusinessRuleException("No coverage exists for " + request.treatmentCategory()));
        if (!coverage.isActive()) {
            throw new BusinessRuleException("Coverage for " + request.treatmentCategory() + " is inactive");
        }
        Optional<PolicyExclusion> exclusion = exclusionRepository
                .findByPolicyIdAndTreatmentCategory(policy.getId(), request.treatmentCategory());
        if (exclusion.isPresent()) {
            throw new BusinessRuleException("Treatment category is excluded: " + exclusion.get().getReason());
        }
        if (preAuthorizationRepository.existsByPatientIdAndProcedureCodeAndStatusIn(
                patient.getId(),
                request.procedureCode().trim(),
                ACTIVE_STATUSES
        )) {
            throw new DuplicateResourceException("An active pre-authorization request already exists for this procedure");
        }

        PreAuthorization preAuthorization = new PreAuthorization();
        preAuthorization.setReferenceNumber(referenceNumberGenerator.generate("PA"));
        preAuthorization.setPatient(patient);
        preAuthorization.setPolicy(policy);
        preAuthorization.setHospital(hospital);
        preAuthorization.setTreatingDoctor(request.treatingDoctor().trim());
        preAuthorization.setDiagnosisCode(request.diagnosisCode().trim());
        preAuthorization.setProcedureCode(request.procedureCode().trim());
        preAuthorization.setTreatmentCategory(request.treatmentCategory());
        preAuthorization.setTreatmentDescription(request.treatmentDescription().trim());
        preAuthorization.setEstimatedCost(request.estimatedCost());
        preAuthorization.setRequestedDate(request.requestedDate());
        preAuthorization.setStatus(PreAuthorizationStatus.PENDING);
        preAuthorization.setRequestedBy(currentUser());
        PreAuthorization saved = preAuthorizationRepository.save(preAuthorization);
        auditService.recordEntity(
                AuditEntityType.PRE_AUTHORIZATION,
                saved.getId(),
                AuditAction.PRE_AUTHORIZATION_REQUESTED,
                "Pre-authorization " + saved.getReferenceNumber() + " requested for patient " + patient.getPatientCode()
                        + " with estimated cost " + saved.getEstimatedCost()
        );
        return PreAuthorizationMapper.toResponse(saved);
    }

    @Override
    public PreAuthorizationResponse get(Long id) {
        return PreAuthorizationMapper.toResponse(findPreAuthorization(id));
    }

    @Override
    public Page<PreAuthorizationResponse> list(
            PreAuthorizationStatus status,
            Long patientId,
            Long hospitalId,
            String policyNumber,
            String referenceNumber,
            TreatmentCategory treatmentCategory,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return preAuthorizationRepository.findAll(
                PreAuthorizationSpecifications.withFilters(
                        status,
                        patientId,
                        hospitalId,
                        policyNumber,
                        referenceNumber,
                        treatmentCategory,
                        fromDate,
                        toDate
                ),
                sanitized
        ).map(PreAuthorizationMapper::toResponse);
    }

    @Override
    @Transactional
    public PreAuthorizationResponse review(Long id) {
        PreAuthorization preAuthorization = preAuthorizationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-authorization", "id", id));
        if (preAuthorization.getStatus() != PreAuthorizationStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    "Pre-authorization",
                    preAuthorization.getStatus(),
                    PreAuthorizationStatus.UNDER_REVIEW
            );
        }
        preAuthorization.setStatus(PreAuthorizationStatus.UNDER_REVIEW);
        preAuthorization.setReviewedBy(currentUser());
        PreAuthorization saved = preAuthorizationRepository.save(preAuthorization);
        auditService.recordEntity(
                AuditEntityType.PRE_AUTHORIZATION,
                saved.getId(),
                AuditAction.PRE_AUTHORIZATION_REVIEWED,
                "Pre-authorization " + saved.getReferenceNumber() + " moved to review"
        );
        return PreAuthorizationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PreAuthorizationResponse decide(Long id, PreAuthorizationDecisionRequest request) {
        PreAuthorization preAuthorization = preAuthorizationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-authorization", "id", id));
        if (preAuthorization.getStatus() != PreAuthorizationStatus.PENDING
                && preAuthorization.getStatus() != PreAuthorizationStatus.UNDER_REVIEW) {
            throw new InvalidStatusTransitionException(
                    "Pre-authorization",
                    preAuthorization.getStatus(),
                    "DECIDED"
            );
        }

        InsurancePolicy policy = preAuthorization.getPolicy();
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BusinessRuleException("Policy is not active");
        }
        PolicyCoverage coverage = coverageRepository
                .findByPolicyIdAndCoverageType(policy.getId(), preAuthorization.getTreatmentCategory())
                .orElseThrow(() -> new BusinessRuleException(
                        "No coverage exists for " + preAuthorization.getTreatmentCategory()
                ));
        if (!coverage.isActive()) {
            throw new BusinessRuleException("Coverage is inactive");
        }
        Optional<PolicyExclusion> exclusion = exclusionRepository
                .findByPolicyIdAndTreatmentCategory(policy.getId(), preAuthorization.getTreatmentCategory());
        if (exclusion.isPresent()) {
            throw new BusinessRuleException("Treatment category is excluded: " + exclusion.get().getReason());
        }

        BigDecimal policyRemaining = policy.getSumInsured().subtract(policy.getUtilizedAmount()).max(BigDecimal.ZERO);
        BigDecimal categoryRemaining = coverage.getCoverageLimit()
                .subtract(coverage.getUtilizedAmount())
                .max(BigDecimal.ZERO);
        BigDecimal estimatedCost = preAuthorization.getEstimatedCost();
        BigDecimal approvedAmount;
        LocalDate validUntil;
        PreAuthorizationStatus status;

        switch (request.decision()) {
            case REJECT -> {
                if (request.remarks() == null || request.remarks().isBlank()) {
                    throw new BusinessRuleException("A reason is required to reject a pre-authorization");
                }
                approvedAmount = BigDecimal.ZERO.setScale(2);
                validUntil = null;
                status = PreAuthorizationStatus.REJECTED;
            }
            case APPROVE -> {
                approvedAmount = request.approvedAmount() == null ? estimatedCost : request.approvedAmount();
                if (approvedAmount.signum() <= 0) {
                    throw new BusinessRuleException("Approved amount must be greater than zero");
                }
                if (approvedAmount.compareTo(estimatedCost) > 0) {
                    throw new BusinessRuleException("Approved amount cannot exceed the estimated cost");
                }
                validateRemaining(approvedAmount, policyRemaining, categoryRemaining);
                validUntil = resolveValidUntil(request.validUntil());
                status = PreAuthorizationStatus.APPROVED;
            }
            case PARTIAL_APPROVE -> {
                if (request.approvedAmount() == null || request.approvedAmount().signum() <= 0) {
                    throw new BusinessRuleException("Approved amount is required for partial approval");
                }
                if (request.approvedAmount().compareTo(estimatedCost) >= 0) {
                    throw new BusinessRuleException("Partial approval amount must be less than the estimated cost");
                }
                validateRemaining(request.approvedAmount(), policyRemaining, categoryRemaining);
                approvedAmount = request.approvedAmount();
                validUntil = resolveValidUntil(request.validUntil());
                status = PreAuthorizationStatus.PARTIALLY_APPROVED;
            }
            default -> throw new BusinessRuleException("Unsupported pre-authorization decision");
        }

        preAuthorization.setStatus(status);
        preAuthorization.setApprovedAmount(approvedAmount);
        preAuthorization.setValidUntil(validUntil);
        preAuthorization.setRemarks(request.remarks() == null ? null : request.remarks().trim());
        if (preAuthorization.getReviewedBy() == null) {
            preAuthorization.setReviewedBy(currentUser());
        }
        preAuthorization.setDecisionDate(Instant.now());
        PreAuthorization saved = preAuthorizationRepository.save(preAuthorization);
        auditService.recordEntity(
                AuditEntityType.PRE_AUTHORIZATION,
                saved.getId(),
                AuditAction.PRE_AUTHORIZATION_DECIDED,
                "Pre-authorization " + saved.getReferenceNumber() + " decided as " + saved.getStatus()
                        + " with approved amount " + saved.getApprovedAmount()
        );
        return PreAuthorizationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PreAuthorizationResponse cancel(Long id) {
        PreAuthorization preAuthorization = preAuthorizationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-authorization", "id", id));
        if (preAuthorization.getStatus() != PreAuthorizationStatus.PENDING
                && preAuthorization.getStatus() != PreAuthorizationStatus.UNDER_REVIEW) {
            throw new InvalidStatusTransitionException(
                    "Pre-authorization",
                    preAuthorization.getStatus(),
                    PreAuthorizationStatus.CANCELLED
            );
        }
        preAuthorization.setStatus(PreAuthorizationStatus.CANCELLED);
        PreAuthorization saved = preAuthorizationRepository.save(preAuthorization);
        auditService.recordEntity(
                AuditEntityType.PRE_AUTHORIZATION,
                saved.getId(),
                AuditAction.PRE_AUTHORIZATION_CANCELLED,
                "Pre-authorization " + saved.getReferenceNumber() + " cancelled"
        );
        return PreAuthorizationMapper.toResponse(saved);
    }

    private void validateRemaining(BigDecimal approvedAmount, BigDecimal policyRemaining, BigDecimal categoryRemaining) {
        if (approvedAmount.compareTo(policyRemaining) > 0) {
            throw new BusinessRuleException("Approved amount exceeds the remaining policy coverage");
        }
        if (approvedAmount.compareTo(categoryRemaining) > 0) {
            throw new BusinessRuleException("Approved amount exceeds the remaining category coverage");
        }
    }

    private LocalDate resolveValidUntil(LocalDate requestedValidUntil) {
        LocalDate validUntil = requestedValidUntil == null ? LocalDate.now().plusDays(30) : requestedValidUntil;
        if (validUntil.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Valid until date must not be in the past");
        }
        return validUntil;
    }

    private PreAuthorization findPreAuthorization(Long id) {
        return preAuthorizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-authorization", "id", id));
    }

    private User currentUser() {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }
}
