package com.healthcare.claimsystem.dto.preauthorization;

import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PreAuthorizationRequest(
        @NotNull(message = "Patient id is required")
        Long patientId,

        @NotNull(message = "Policy id is required")
        Long policyId,

        @NotNull(message = "Hospital id is required")
        Long hospitalId,

        @NotBlank(message = "Treating doctor is required")
        @Size(max = 120, message = "Treating doctor must not exceed 120 characters")
        String treatingDoctor,

        @NotBlank(message = "Diagnosis code is required")
        @Size(max = 20, message = "Diagnosis code must not exceed 20 characters")
        String diagnosisCode,

        @NotBlank(message = "Procedure code is required")
        @Size(max = 20, message = "Procedure code must not exceed 20 characters")
        String procedureCode,

        @NotNull(message = "Treatment category is required")
        TreatmentCategory treatmentCategory,

        @NotBlank(message = "Treatment description is required")
        @Size(max = 500, message = "Treatment description must not exceed 500 characters")
        String treatmentDescription,

        @NotNull(message = "Estimated cost is required")
        @DecimalMin(value = "0.01", message = "Estimated cost must be greater than zero")
        @Digits(integer = 12, fraction = 2, message = "Estimated cost is invalid")
        BigDecimal estimatedCost,

        @NotNull(message = "Requested date is required")
        @PastOrPresent(message = "Requested date must not be in the future")
        LocalDate requestedDate
) {
}
