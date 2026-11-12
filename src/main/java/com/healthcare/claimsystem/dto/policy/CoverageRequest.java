package com.healthcare.claimsystem.dto.policy;

import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CoverageRequest(
        @NotNull(message = "Coverage type is required")
        TreatmentCategory coverageType,

        @NotNull(message = "Coverage limit is required")
        @DecimalMin(value = "0.01", message = "Coverage limit must be greater than zero")
        @Digits(integer = 12, fraction = 2, message = "Coverage limit is invalid")
        BigDecimal coverageLimit,

        Boolean active
) {
}
