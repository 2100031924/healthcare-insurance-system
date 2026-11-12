package com.healthcare.claimsystem.dto.policy;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyRenewRequest(
        @NotNull(message = "New end date is required")
        @Future(message = "New end date must be in the future")
        LocalDate newEndDate,

        @PositiveOrZero(message = "Additional sum insured must not be negative")
        @Digits(integer = 12, fraction = 2, message = "Additional sum insured is invalid")
        BigDecimal additionalSumInsured,

        @PositiveOrZero(message = "Premium amount must not be negative")
        @Digits(integer = 12, fraction = 2, message = "Premium amount is invalid")
        BigDecimal premiumAmount
) {
}
