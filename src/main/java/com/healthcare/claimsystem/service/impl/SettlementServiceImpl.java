package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.settlement.SettlementRequest;
import com.healthcare.claimsystem.dto.settlement.SettlementResponse;
import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.ClaimSettlement;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.SettlementStatus;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.DuplicateResourceException;
import com.healthcare.claimsystem.exception.InvalidStatusTransitionException;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.SettlementMapper;
import com.healthcare.claimsystem.repository.ClaimRepository;
import com.healthcare.claimsystem.repository.ClaimSettlementRepository;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.UserRepository;
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.service.SettlementService;
import com.healthcare.claimsystem.specification.SettlementSpecifications;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "settlementNumber", "paidAmount", "processedAt", "status"
    );

    private final ClaimSettlementRepository settlementRepository;
    private final ClaimRepository claimRepository;
    private final InsurancePolicyRepository policyRepository;
    private final PolicyCoverageRepository coverageRepository;
    private final UserRepository userRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final AuditService auditService;

    @Override
    @Transactional
    public SettlementResponse settle(SettlementRequest request) {
        Claim claim = claimRepository.findByIdForUpdate(request.claimId())
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", request.claimId()));
        if (claim.getStatus() == ClaimStatus.SETTLED || settlementRepository.existsByClaimId(claim.getId())) {
            throw new BusinessRuleException("Claim has already been settled");
        }
        if (claim.getStatus() != ClaimStatus.APPROVED && claim.getStatus() != ClaimStatus.PARTIALLY_APPROVED) {
            throw new InvalidStatusTransitionException("Claim", claim.getStatus(), ClaimStatus.SETTLED);
        }
        BigDecimal approvedAmount = claim.getApprovedAmount();
        if (approvedAmount == null || approvedAmount.signum() <= 0) {
            throw new BusinessRuleException("Claim has no approved amount to settle");
        }
        String paymentReference = request.paymentReference().trim();
        if (settlementRepository.existsByPaymentReference(paymentReference)) {
            throw new DuplicateResourceException("Payment reference already exists: " + paymentReference);
        }

        InsurancePolicy policy = policyRepository.findByIdForUpdate(claim.getPolicy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", claim.getPolicy().getId()));
        PolicyCoverage coverage = coverageRepository
                .findByPolicyIdAndCoverageTypeForUpdate(policy.getId(), claim.getTreatmentCategory())
                .orElseThrow(() -> new BusinessRuleException(
                        "No coverage exists for " + claim.getTreatmentCategory()
                ));
        if (!coverage.isActive()) {
            throw new BusinessRuleException("Coverage is inactive");
        }
        BigDecimal policyRemaining = policy.getSumInsured()
                .subtract(policy.getUtilizedAmount())
                .max(BigDecimal.ZERO);
        BigDecimal categoryRemaining = coverage.getCoverageLimit()
                .subtract(coverage.getUtilizedAmount())
                .max(BigDecimal.ZERO);
        if (approvedAmount.compareTo(policyRemaining) > 0) {
            throw new BusinessRuleException("Insufficient policy coverage remaining for settlement");
        }
        if (approvedAmount.compareTo(categoryRemaining) > 0) {
            throw new BusinessRuleException("Insufficient category coverage remaining for settlement");
        }

        policy.setUtilizedAmount(policy.getUtilizedAmount().add(approvedAmount));
        coverage.setUtilizedAmount(coverage.getUtilizedAmount().add(approvedAmount));
        policyRepository.save(policy);
        coverageRepository.save(coverage);

        claim.setStatus(ClaimStatus.SETTLED);
        claimRepository.save(claim);

        ClaimSettlement settlement = new ClaimSettlement();
        settlement.setSettlementNumber(referenceNumberGenerator.generate("STL"));
        settlement.setClaim(claim);
        settlement.setApprovedAmount(approvedAmount);
        settlement.setPaidAmount(approvedAmount);
        settlement.setPatientResponsibility(claim.getPatientResponsibility());
        settlement.setPaymentMode(request.paymentMode());
        settlement.setPaymentReference(paymentReference);
        settlement.setStatus(SettlementStatus.PROCESSED);
        settlement.setProcessedBy(currentUser());
        settlement.setProcessedAt(Instant.now());
        settlement.setRemarks(request.remarks() == null ? null : request.remarks().trim());
        ClaimSettlement saved = settlementRepository.save(settlement);

        auditService.recordClaim(
                claim,
                AuditAction.CLAIM_SETTLED,
                "Claim " + claim.getClaimNumber() + " settled with payment " + saved.getPaidAmount()
                        + " using reference " + saved.getPaymentReference()
        );
        return SettlementMapper.toResponse(saved);
    }

    @Override
    public SettlementResponse get(Long id) {
        ClaimSettlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement", "id", id));
        return SettlementMapper.toResponse(settlement);
    }

    @Override
    public Page<SettlementResponse> list(
            SettlementStatus status,
            Long claimId,
            String claimNumber,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return settlementRepository.findAll(
                SettlementSpecifications.withFilters(status, claimId, claimNumber, fromDate, toDate),
                sanitized
        ).map(SettlementMapper::toResponse);
    }

    private User currentUser() {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }
}
