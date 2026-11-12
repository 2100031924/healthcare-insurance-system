package com.healthcare.claimsystem.dto.policy;

import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PolicyStatusRequest(
        @NotNull(message = "Policy status is required")
        PolicyStatus status,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
