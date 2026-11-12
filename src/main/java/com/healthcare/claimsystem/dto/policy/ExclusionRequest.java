package com.healthcare.claimsystem.dto.policy;

import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExclusionRequest(
        @NotNull(message = "Treatment category is required")
        TreatmentCategory treatmentCategory,

        @NotBlank(message = "Exclusion reason is required")
        @Size(max = 500, message = "Exclusion reason must not exceed 500 characters")
        String reason
) {
}
