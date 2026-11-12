package com.healthcare.claimsystem.dto.claim;

import java.math.BigDecimal;
import java.util.List;

public record AdjudicationPreviewResponse(
        Long claimId,
        String claimNumber,
        BigDecimal claimedAmount,
        BigDecimal eligibleAmount,
        BigDecimal nonCoveredAmount,
        BigDecimal deductibleApplied,
        BigDecimal coPaymentAmount,
        BigDecimal approvedAmount,
        BigDecimal patientResponsibility,
        boolean covered,
        List<String> remarks
) {
}
