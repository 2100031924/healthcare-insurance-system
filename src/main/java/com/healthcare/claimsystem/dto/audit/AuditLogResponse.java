package com.healthcare.claimsystem.dto.audit;

import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long claimId,
        String claimNumber,
        AuditEntityType entityType,
        Long entityId,
        AuditAction action,
        String performedByUsername,
        String details,
        Instant createdAt
) {
}
