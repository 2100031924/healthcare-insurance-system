package com.healthcare.claimsystem.mapper;

import com.healthcare.claimsystem.dto.policy.CoverageResponse;
import com.healthcare.claimsystem.dto.policy.ExclusionResponse;
import com.healthcare.claimsystem.dto.policy.PolicyResponse;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PolicyExclusion;

import java.math.BigDecimal;
import java.util.List;

public final class PolicyMapper {

    private PolicyMapper() {
    }

    public static PolicyResponse toResponse(
            InsurancePolicy policy,
            List<PolicyCoverage> coverages,
            List<PolicyExclusion> exclusions
    ) {
        BigDecimal remaining = policy.getSumInsured().subtract(policy.getUtilizedAmount()).max(BigDecimal.ZERO);
        return new PolicyResponse(
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getPatient().getId(),
                policy.getPatient().getPatientCode(),
                policy.getPatient().getFirstName() + " " + policy.getPatient().getLastName(),
                policy.getProviderName(),
                policy.getPolicyType(),
                policy.getStatus(),
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getSumInsured(),
                policy.getUtilizedAmount(),
                remaining,
                policy.getDeductibleAmount(),
                policy.getCoPaymentPercentage(),
                policy.getWaitingPeriodDays(),
                policy.getPremiumAmount(),
                policy.getRenewalCount(),
                policy.getLastRenewedAt(),
                policy.getCancellationReason(),
                coverages.stream().map(PolicyMapper::toCoverageResponse).toList(),
                exclusions.stream().map(PolicyMapper::toExclusionResponse).toList(),
                policy.getCreatedAt()
        );
    }

    public static CoverageResponse toCoverageResponse(PolicyCoverage coverage) {
        BigDecimal remaining = coverage.getCoverageLimit().subtract(coverage.getUtilizedAmount()).max(BigDecimal.ZERO);
        return new CoverageResponse(
                coverage.getId(),
                coverage.getCoverageType(),
                coverage.getCoverageLimit(),
                coverage.getUtilizedAmount(),
                remaining,
                coverage.isActive()
        );
    }

    public static ExclusionResponse toExclusionResponse(PolicyExclusion exclusion) {
        return new ExclusionResponse(
                exclusion.getId(),
                exclusion.getTreatmentCategory(),
                exclusion.getReason()
        );
    }
}
