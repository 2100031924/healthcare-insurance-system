package com.healthcare.claimsystem.dto.eligibility;

import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EligibilityResponse(
        boolean eligible,
        Long patientId,
        String patientCode,
        String patientName,
        Long policyId,
        String policyNumber,
        PolicyStatus policyStatus,
        LocalDate policyStartDate,
        LocalDate policyEndDate,
        LocalDate serviceDate,
        LocalDate waitingPeriodEndDate,
        BigDecimal sumInsured,
        BigDecimal utilizedAmount,
        BigDecimal remainingCoverage,
        TreatmentCategory treatmentCategory,
        BigDecimal categoryLimit,
        BigDecimal categoryUtilized,
        BigDecimal categoryRemaining,
        BigDecimal deductibleAmount,
        BigDecimal coPaymentPercentage,
        BigDecimal estimatedCost,
        BigDecimal estimatedInsurerPayable,
        BigDecimal estimatedPatientResponsibility,
        List<String> reasons,
        Instant verifiedAt
) {
}
