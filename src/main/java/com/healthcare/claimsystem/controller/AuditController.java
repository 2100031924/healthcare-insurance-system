package com.healthcare.claimsystem.controller;

import com.healthcare.claimsystem.dto.audit.AuditLogResponse;
import com.healthcare.claimsystem.dto.common.PageResponse;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import com.healthcare.claimsystem.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Management", description = "System activity and claim audit history")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List audit records with pagination, sorting and filters")
    public ResponseEntity<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) Long claimId,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(auditService.list(
                claimId,
                entityType,
                action,
                fromDate,
                toDate,
                pageable
        )));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an audit record by identifier")
    public ResponseEntity<AuditLogResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.get(id));
    }
}
