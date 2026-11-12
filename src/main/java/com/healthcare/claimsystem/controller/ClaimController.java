package com.healthcare.claimsystem.controller;

import com.healthcare.claimsystem.dto.audit.AuditLogResponse;
import com.healthcare.claimsystem.dto.claim.AdjudicationPreviewResponse;
import com.healthcare.claimsystem.dto.claim.ClaimAdjudicationRequest;
import com.healthcare.claimsystem.dto.claim.ClaimRequest;
import com.healthcare.claimsystem.dto.claim.ClaimResponse;
import com.healthcare.claimsystem.dto.common.PageResponse;
import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Tag(name = "Claim Management", description = "Claim submission, adjudication, workflow and audit history")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL')")
    @Operation(summary = "Submit an insurance claim")
    public ResponseEntity<ClaimResponse> submit(@Valid @RequestBody ClaimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.submit(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "Get a claim by identifier")
    public ResponseEntity<ClaimResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "List claims with pagination, sorting and filters")
    public ResponseEntity<PageResponse<ClaimResponse>> list(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(required = false) String claimNumber,
            @RequestParam(required = false) TreatmentCategory treatmentCategory,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(claimService.list(
                status,
                patientId,
                hospitalId,
                policyNumber,
                claimNumber,
                treatmentCategory,
                fromDate,
                toDate,
                pageable
        )));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('INSURANCE_OFFICER')")
    @Operation(summary = "Move a claim into review")
    public ResponseEntity<ClaimResponse> review(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.review(id));
    }

    @GetMapping("/{id}/adjudication-preview")
    @PreAuthorize("hasRole('INSURANCE_OFFICER')")
    @Operation(summary = "Preview the rules engine calculation for a claim")
    public ResponseEntity<AdjudicationPreviewResponse> previewAdjudication(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.previewAdjudication(id));
    }

    @PostMapping("/{id}/adjudicate")
    @PreAuthorize("hasRole('INSURANCE_OFFICER')")
    @Operation(summary = "Approve, partially approve or reject a claim")
    public ResponseEntity<ClaimResponse> adjudicate(
            @PathVariable Long id,
            @Valid @RequestBody ClaimAdjudicationRequest request
    ) {
        return ResponseEntity.ok(claimService.adjudicate(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('HOSPITAL')")
    @Operation(summary = "Cancel a claim before adjudication")
    public ResponseEntity<ClaimResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.cancel(id));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "List the audit history of a claim")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAudit(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(claimService.getAudit(id, pageable)));
    }
}
