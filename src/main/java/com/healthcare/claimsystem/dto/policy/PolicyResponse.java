package com.healthcare.claimsystem.dto.policy;

import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PolicyResponse(
        Long id,
        String policyNumber,
        Long patientId,
        String patientCode,
        String patientFullName,
        String providerName,
        PolicyType policyType,
        PolicyStatus status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal sumInsured,
        BigDecimal utilizedAmount,
        BigDecimal remainingAmount,
        BigDecimal deductibleAmount,
        BigDecimal coPaymentPercentage,
        int waitingPeriodDays,
        BigDecimal premiumAmount,
        int renewalCount,
        Instant lastRenewedAt,
        String cancellationReason,
        List<CoverageResponse> coverages,
        List<ExclusionResponse> exclusions,
        Instant createdAt
) {
}
