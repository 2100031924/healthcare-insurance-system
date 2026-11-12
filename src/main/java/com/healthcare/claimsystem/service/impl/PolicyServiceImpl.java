package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.policy.CoverageRequest;
import com.healthcare.claimsystem.dto.policy.CoverageResponse;
import com.healthcare.claimsystem.dto.policy.ExclusionRequest;
import com.healthcare.claimsystem.dto.policy.ExclusionResponse;
import com.healthcare.claimsystem.dto.policy.PolicyRenewRequest;
import com.healthcare.claimsystem.dto.policy.PolicyRequest;
import com.healthcare.claimsystem.dto.policy.PolicyResponse;
import com.healthcare.claimsystem.dto.policy.PolicyStatusRequest;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PolicyExclusion;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.DuplicateResourceException;
import com.healthcare.claimsystem.exception.InvalidStatusTransitionException;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.PolicyMapper;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.service.PolicyService;
import com.healthcare.claimsystem.specification.InsurancePolicySpecifications;
import com.healthcare.claimsystem.util.PageableUtils;
import com.healthcare.claimsystem.util.ReferenceNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "policyNumber", "startDate", "endDate", "sumInsured", "utilizedAmount", "status", "policyType"
    );

    private final InsurancePolicyRepository policyRepository;
    private final PatientRepository patientRepository;
    private final PolicyCoverageRepository coverageRepository;
    private final PolicyExclusionRepository exclusionRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final AuditService auditService;

    @Override
    @Transactional
    public PolicyResponse create(PolicyRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));
        validateDates(request.startDate(), request.endDate());
        String policyNumber = request.policyNumber() == null || request.policyNumber().isBlank()
                ? referenceNumberGenerator.generate("POL")
                : request.policyNumber().trim().toUpperCase();
        if (policyRepository.existsByPolicyNumber(policyNumber)) {
            throw new DuplicateResourceException("Policy number already exists: " + policyNumber);
        }

        InsurancePolicy policy = new InsurancePolicy();
        policy.setPolicyNumber(policyNumber);
        policy.setPatient(patient);
        policy.setProviderName(request.providerName().trim());
        policy.setPolicyType(request.policyType());
        policy.setStartDate(request.startDate());
        policy.setEndDate(request.endDate());
        policy.setSumInsured(request.sumInsured());
        policy.setUtilizedAmount(BigDecimal.ZERO.setScale(2));
        policy.setDeductibleAmount(request.deductibleAmount());
        policy.setCoPaymentPercentage(request.coPaymentPercentage());
        policy.setWaitingPeriodDays(request.waitingPeriodDays());
        policy.setPremiumAmount(request.premiumAmount());
        policy.setStatus(request.endDate().isBefore(LocalDate.now()) ? PolicyStatus.EXPIRED : PolicyStatus.ACTIVE);
        InsurancePolicy saved = policyRepository.save(policy);
        auditService.recordEntity(
                AuditEntityType.POLICY,
                saved.getId(),
                AuditAction.POLICY_CREATED,
                "Policy " + saved.getPolicyNumber() + " created for patient " + patient.getPatientCode()
        );
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PolicyResponse update(Long id, PolicyRequest request) {
        InsurancePolicy policy = findPolicy(id);
        if (!policy.getPatient().getId().equals(request.patientId())) {
            throw new BusinessRuleException("Policy holder cannot be changed");
        }
        validateDates(request.startDate(), request.endDate());
        if (request.sumInsured().compareTo(policy.getUtilizedAmount()) < 0) {
            throw new BusinessRuleException("Sum insured cannot be lower than the utilized amount");
        }

        policy.setProviderName(request.providerName().trim());
        policy.setPolicyType(request.policyType());
        policy.setStartDate(request.startDate());
        policy.setEndDate(request.endDate());
        policy.setSumInsured(request.sumInsured());
        policy.setDeductibleAmount(request.deductibleAmount());
        policy.setCoPaymentPercentage(request.coPaymentPercentage());
        policy.setWaitingPeriodDays(request.waitingPeriodDays());
        policy.setPremiumAmount(request.premiumAmount());
        if (policy.getStatus() == PolicyStatus.ACTIVE && policy.getEndDate().isBefore(LocalDate.now())) {
            policy.setStatus(PolicyStatus.EXPIRED);
        } else if (policy.getStatus() == PolicyStatus.EXPIRED && !policy.getEndDate().isBefore(LocalDate.now())) {
            policy.setStatus(PolicyStatus.ACTIVE);
        }
        InsurancePolicy saved = policyRepository.save(policy);
        auditService.recordEntity(
                AuditEntityType.POLICY,
                saved.getId(),
                AuditAction.POLICY_UPDATED,
                "Policy " + saved.getPolicyNumber() + " updated"
        );
        return toResponse(saved);
    }

    @Override
    public PolicyResponse get(Long id) {
        return toResponse(findPolicy(id));
    }

    @Override
    public Page<PolicyResponse> list(
            Long patientId,
            PolicyStatus status,
            PolicyType policyType,
            String policyNumber,
            String providerName,
            LocalDate expiringBefore,
            Pageable pageable
    ) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return policyRepository.findAll(
                InsurancePolicySpecifications.withFilters(
                        patientId,
                        status,
                        policyType,
                        policyNumber,
                        providerName,
                        expiringBefore
                ),
                sanitized
        ).map(this::toResponse);
    }

    @Override
    @Transactional
    public PolicyResponse renew(Long id, PolicyRenewRequest request) {
        InsurancePolicy policy = policyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", id));
        if (policy.getStatus() == PolicyStatus.CANCELLED || policy.getStatus() == PolicyStatus.SUSPENDED) {
            throw new BusinessRuleException("A " + policy.getStatus().name().toLowerCase() + " policy cannot be renewed");
        }
        if (!request.newEndDate().isAfter(policy.getEndDate())) {
            throw new BusinessRuleException("New end date must be after the current end date");
        }
        if (request.newEndDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("New end date must not be in the past");
        }
        if (request.additionalSumInsured() != null && request.additionalSumInsured().signum() > 0) {
            policy.setSumInsured(policy.getSumInsured().add(request.additionalSumInsured()));
        }
        if (request.premiumAmount() != null) {
            policy.setPremiumAmount(request.premiumAmount());
        }
        policy.setEndDate(request.newEndDate());
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setRenewalCount(policy.getRenewalCount() + 1);
        policy.setLastRenewedAt(Instant.now());
        InsurancePolicy saved = policyRepository.save(policy);
        auditService.recordEntity(
                AuditEntityType.POLICY,
                saved.getId(),
                AuditAction.POLICY_RENEWED,
                "Policy " + saved.getPolicyNumber() + " renewed until " + saved.getEndDate()
        );
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PolicyResponse changeStatus(Long id, PolicyStatusRequest request) {
        InsurancePolicy policy = policyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", id));
        PolicyStatus target = request.status();
        if (target == PolicyStatus.EXPIRED) {
            throw new InvalidStatusTransitionException("Policy expiration is managed by the system");
        }
        switch (target) {
            case ACTIVE -> {
                if (policy.getStatus() == PolicyStatus.CANCELLED) {
                    throw new InvalidStatusTransitionException("Cancelled policies cannot be reactivated");
                }
                if (policy.getEndDate().isBefore(LocalDate.now())) {
                    throw new BusinessRuleException("An expired policy must be renewed before activation");
                }
                policy.setStatus(PolicyStatus.ACTIVE);
                policy.setCancellationReason(null);
            }
            case SUSPENDED -> {
                if (policy.getStatus() == PolicyStatus.CANCELLED) {
                    throw new InvalidStatusTransitionException("Cancelled policies cannot be suspended");
                }
                if (request.reason() == null || request.reason().isBlank()) {
                    throw new BusinessRuleException("A reason is required to suspend a policy");
                }
                policy.setStatus(PolicyStatus.SUSPENDED);
                policy.setCancellationReason(request.reason().trim());
            }
            case CANCELLED -> {
                if (policy.getStatus() == PolicyStatus.CANCELLED) {
                    throw new InvalidStatusTransitionException("Policy is already cancelled");
                }
                if (request.reason() == null || request.reason().isBlank()) {
                    throw new BusinessRuleException("A reason is required to cancel a policy");
                }
                policy.setStatus(PolicyStatus.CANCELLED);
                policy.setCancellationReason(request.reason().trim());
            }
            default -> throw new InvalidStatusTransitionException("Unsupported policy status: " + target);
        }
        InsurancePolicy saved = policyRepository.save(policy);
        auditService.recordEntity(
                AuditEntityType.POLICY,
                saved.getId(),
                AuditAction.POLICY_STATUS_CHANGED,
                "Policy " + saved.getPolicyNumber() + " status changed to " + saved.getStatus()
        );
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CoverageResponse addCoverage(Long policyId, CoverageRequest request) {
        InsurancePolicy policy = findPolicy(policyId);
        if (coverageRepository.existsByPolicyIdAndCoverageType(policyId, request.coverageType())) {
            throw new DuplicateResourceException("Coverage already exists for " + request.coverageType());
        }
        PolicyCoverage coverage = new PolicyCoverage();
        coverage.setPolicy(policy);
        coverage.setCoverageType(request.coverageType());
        coverage.setCoverageLimit(request.coverageLimit());
        coverage.setUtilizedAmount(BigDecimal.ZERO.setScale(2));
        coverage.setActive(request.active() == null || request.active());
        PolicyCoverage saved = coverageRepository.save(coverage);
        auditService.recordEntity(
                AuditEntityType.POLICY,
                policyId,
                AuditAction.COVERAGE_ADDED,
                "Coverage " + saved.getCoverageType() + " added with limit " + saved.getCoverageLimit()
        );
        return PolicyMapper.toCoverageResponse(saved);
    }

    @Override
    @Transactional
    public CoverageResponse updateCoverage(Long policyId, Long coverageId, CoverageRequest request) {
        InsurancePolicy policy = findPolicy(policyId);
        PolicyCoverage coverage = coverageRepository.findByIdAndPolicyId(coverageId, policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy coverage", "id", coverageId));
        if (coverage.getCoverageType() != request.coverageType()) {
            throw new BusinessRuleException("Coverage type cannot be changed");
        }
        if (request.coverageLimit().compareTo(coverage.getUtilizedAmount()) < 0) {
            throw new BusinessRuleException("Coverage limit cannot be lower than the utilized amount");
        }
        coverage.setCoverageLimit(request.coverageLimit());
        if (request.active() != null) {
            coverage.setActive(request.active());
        }
        PolicyCoverage saved = coverageRepository.save(coverage);
        auditService.recordEntity(
                AuditEntityType.POLICY,
                policy.getId(),
                AuditAction.COVERAGE_UPDATED,
                "Coverage " + saved.getCoverageType() + " updated with limit " + saved.getCoverageLimit()
        );
        return PolicyMapper.toCoverageResponse(saved);
    }

    @Override
    public List<CoverageResponse> getCoverages(Long policyId) {
        findPolicy(policyId);
        return coverageRepository.findByPolicyIdOrderByCoverageTypeAsc(policyId).stream()
                .map(PolicyMapper::toCoverageResponse)
                .toList();
    }

    @Override
    @Transactional
    public ExclusionResponse addExclusion(Long policyId, ExclusionRequest request) {
        InsurancePolicy policy = findPolicy(policyId);
        if (exclusionRepository.existsByPolicyIdAndTreatmentCategory(policyId, request.treatmentCategory())) {
            throw new DuplicateResourceException("Exclusion already exists for " + request.treatmentCategory());
        }
        PolicyExclusion exclusion = new PolicyExclusion();
        exclusion.setPolicy(policy);
        exclusion.setTreatmentCategory(request.treatmentCategory());
        exclusion.setReason(request.reason().trim());
        PolicyExclusion saved = exclusionRepository.save(exclusion);
        auditService.recordEntity(
                AuditEntityType.POLICY,
                policyId,
                AuditAction.EXCLUSION_ADDED,
                "Exclusion " + saved.getTreatmentCategory() + " added"
        );
        return PolicyMapper.toExclusionResponse(saved);
    }

    @Override
    public List<ExclusionResponse> getExclusions(Long policyId) {
        findPolicy(policyId);
        return exclusionRepository.findByPolicyIdOrderByTreatmentCategoryAsc(policyId).stream()
                .map(PolicyMapper::toExclusionResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeExclusion(Long policyId, Long exclusionId) {
        InsurancePolicy policy = findPolicy(policyId);
        PolicyExclusion exclusion = exclusionRepository.findByIdAndPolicyId(exclusionId, policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy exclusion", "id", exclusionId));
        exclusionRepository.delete(exclusion);
        auditService.recordEntity(
                AuditEntityType.POLICY,
                policy.getId(),
                AuditAction.EXCLUSION_REMOVED,
                "Exclusion " + exclusion.getTreatmentCategory() + " removed"
        );
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("Policy end date must not be before the start date");
        }
    }

    private InsurancePolicy findPolicy(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", id));
    }

    private PolicyResponse toResponse(InsurancePolicy policy) {
        return PolicyMapper.toResponse(
                policy,
                coverageRepository.findByPolicyIdOrderByCoverageTypeAsc(policy.getId()),
                exclusionRepository.findByPolicyIdOrderByTreatmentCategoryAsc(policy.getId())
        );
    }
}
