package com.healthcare.claimsystem.dto.claim;

import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(
        @NotNull(message = "Patient id is required")
        Long patientId,

        @NotNull(message = "Policy id is required")
        Long policyId,

        @NotNull(message = "Hospital id is required")
        Long hospitalId,

        Long preAuthorizationId,

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

        @NotNull(message = "Admission date is required")
        @PastOrPresent(message = "Admission date must not be in the future")
        LocalDate admissionDate,

        @PastOrPresent(message = "Discharge date must not be in the future")
        LocalDate dischargeDate,

        @NotNull(message = "Claim date is required")
        @PastOrPresent(message = "Claim date must not be in the future")
        LocalDate claimDate,

        @NotNull(message = "Claimed amount is required")
        @DecimalMin(value = "0.01", message = "Claimed amount must be greater than zero")
        @Digits(integer = 12, fraction = 2, message = "Claimed amount is invalid")
        BigDecimal claimedAmount,

        @NotBlank(message = "Invoice number is required")
        @Size(max = 80, message = "Invoice number must not exceed 80 characters")
        String invoiceNumber,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {
}
