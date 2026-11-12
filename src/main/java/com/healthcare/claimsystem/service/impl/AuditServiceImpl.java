package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.audit.AuditLogResponse;
import com.healthcare.claimsystem.entity.AuditLog;
import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.AuditLogMapper;
import com.healthcare.claimsystem.repository.AuditLogRepository;
import com.healthcare.claimsystem.repository.UserRepository;
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.specification.AuditLogSpecifications;
import com.healthcare.claimsystem.util.PageableUtils;
import com.healthcare.claimsystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "action", "entityType");

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void recordClaim(Claim claim, AuditAction action, String details) {
        save(claim, AuditEntityType.CLAIM, claim.getId(), action, details);
    }

    @Override
    @Transactional
    public void recordEntity(AuditEntityType entityType, Long entityId, AuditAction action, String details) {
        save(null, entityType, entityId, action, details);
    }

    @Override
    public Page<AuditLogResponse> list(
            Long claimId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return auditLogRepository.findAll(
                AuditLogSpecifications.withFilters(claimId, entityType, action, fromDate, toDate),
                sanitized
        ).map(AuditLogMapper::toResponse);
    }

    @Override
    public Page<AuditLogResponse> getClaimAudit(Long claimId, Pageable pageable) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return auditLogRepository.findByClaimId(claimId, sanitized).map(AuditLogMapper::toResponse);
    }

    @Override
    public AuditLogResponse get(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log", "id", id));
        return AuditLogMapper.toResponse(auditLog);
    }

    private void save(Claim claim, AuditEntityType entityType, Long entityId, AuditAction action, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setClaim(claim);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setPerformedBy(currentUser());
        auditLogRepository.save(auditLog);
    }

    private User currentUser() {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }
}
