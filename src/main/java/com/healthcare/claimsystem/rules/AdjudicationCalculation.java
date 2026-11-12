package com.healthcare.claimsystem.rules;

import java.math.BigDecimal;
import java.util.List;

public record AdjudicationCalculation(
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
