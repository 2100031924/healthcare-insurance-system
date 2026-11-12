package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.audit.AuditLogResponse;
import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AuditService {

    void recordClaim(Claim claim, AuditAction action, String details);

    void recordEntity(AuditEntityType entityType, Long entityId, AuditAction action, String details);

    Page<AuditLogResponse> list(
            Long claimId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    Page<AuditLogResponse> getClaimAudit(Long claimId, Pageable pageable);

    AuditLogResponse get(Long id);
}
