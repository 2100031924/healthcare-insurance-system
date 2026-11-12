package com.healthcare.claimsystem.dto.settlement;

import com.healthcare.claimsystem.entity.enums.PaymentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SettlementRequest(
        @NotNull(message = "Claim id is required")
        Long claimId,

        @NotNull(message = "Payment mode is required")
        PaymentMode paymentMode,

        @NotBlank(message = "Payment reference is required")
        @Size(max = 100, message = "Payment reference must not exceed 100 characters")
        String paymentReference,

        @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
        String remarks
) {
}
