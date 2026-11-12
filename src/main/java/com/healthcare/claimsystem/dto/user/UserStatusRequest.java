package com.healthcare.claimsystem.dto.user;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull(message = "Enabled flag is required")
        Boolean enabled
) {
}
