package com.healthcare.claimsystem.mapper;

import com.healthcare.claimsystem.dto.audit.AuditLogResponse;
import com.healthcare.claimsystem.entity.AuditLog;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getClaim() == null ? null : auditLog.getClaim().getId(),
                auditLog.getClaim() == null ? null : auditLog.getClaim().getClaimNumber(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getPerformedBy() == null ? null : auditLog.getPerformedBy().getUsername(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
