package com.healthcare.claimsystem.dto.policy;

import com.healthcare.claimsystem.entity.enums.PolicyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyRequest(
        @NotNull(message = "Patient id is required")
        Long patientId,

        @Size(max = 50, message = "Policy number must not exceed 50 characters")
        String policyNumber,

        @NotBlank(message = "Provider name is required")
        @Size(max = 150, message = "Provider name must not exceed 150 characters")
        String providerName,

        @NotNull(message = "Policy type is required")
        PolicyType policyType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Sum insured is required")
        @DecimalMin(value = "0.01", message = "Sum insured must be greater than zero")
        @Digits(integer = 12, fraction = 2, message = "Sum insured is invalid")
        BigDecimal sumInsured,

        @NotNull(message = "Deductible amount is required")
        @PositiveOrZero(message = "Deductible amount must not be negative")
        @Digits(integer = 12, fraction = 2, message = "Deductible amount is invalid")
        BigDecimal deductibleAmount,

        @NotNull(message = "Co-payment percentage is required")
        @DecimalMin(value = "0.00", message = "Co-payment percentage must not be negative")
        @DecimalMax(value = "100.00", message = "Co-payment percentage must not exceed 100")
        BigDecimal coPaymentPercentage,

        @Min(value = 0, message = "Waiting period must not be negative")
        int waitingPeriodDays,

        @NotNull(message = "Premium amount is required")
        @PositiveOrZero(message = "Premium amount must not be negative")
        @Digits(integer = 12, fraction = 2, message = "Premium amount is invalid")
        BigDecimal premiumAmount
) {
}
