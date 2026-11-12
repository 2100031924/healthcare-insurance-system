package com.healthcare.claimsystem.dto.preauthorization;

import com.healthcare.claimsystem.entity.enums.PreAuthorizationDecision;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PreAuthorizationDecisionRequest(
        @NotNull(message = "Decision is required")
        PreAuthorizationDecision decision,

        @PositiveOrZero(message = "Approved amount must not be negative")
        @Digits(integer = 12, fraction = 2, message = "Approved amount is invalid")
        BigDecimal approvedAmount,

        @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
        String remarks,

        LocalDate validUntil
) {
}
