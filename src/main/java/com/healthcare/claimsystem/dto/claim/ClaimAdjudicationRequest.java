package com.healthcare.claimsystem.dto.claim;

import com.healthcare.claimsystem.entity.enums.AdjudicationDecision;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ClaimAdjudicationRequest(
        @NotNull(message = "Decision is required")
        AdjudicationDecision decision,

        @PositiveOrZero(message = "Approved amount must not be negative")
        @Digits(integer = 12, fraction = 2, message = "Approved amount is invalid")
        BigDecimal approvedAmount,

        @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
        String rejectionReason,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {
}
