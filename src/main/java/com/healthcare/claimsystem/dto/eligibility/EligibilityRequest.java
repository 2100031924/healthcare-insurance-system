package com.healthcare.claimsystem.dto.eligibility;

import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EligibilityRequest(
        @NotNull(message = "Patient id is required")
        Long patientId,

        @NotBlank(message = "Policy number is required")
        String policyNumber,

        @NotNull(message = "Treatment category is required")
        TreatmentCategory treatmentCategory,

        @DecimalMin(value = "0.00", message = "Estimated cost must not be negative")
        @Digits(integer = 12, fraction = 2, message = "Estimated cost is invalid")
        BigDecimal estimatedCost,

        LocalDate serviceDate
) {
}
