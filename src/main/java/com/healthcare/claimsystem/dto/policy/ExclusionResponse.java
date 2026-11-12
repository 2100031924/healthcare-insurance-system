package com.healthcare.claimsystem.dto.policy;

import com.healthcare.claimsystem.entity.enums.TreatmentCategory;

public record ExclusionResponse(
        Long id,
        TreatmentCategory treatmentCategory,
        String reason
) {
}
