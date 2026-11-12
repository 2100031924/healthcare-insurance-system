package com.healthcare.claimsystem.dto.policy;

import com.healthcare.claimsystem.entity.enums.TreatmentCategory;

import java.math.BigDecimal;

public record CoverageResponse(
        Long id,
        TreatmentCategory coverageType,
        BigDecimal coverageLimit,
        BigDecimal utilizedAmount,
        BigDecimal remainingAmount,
        boolean active
) {
}
