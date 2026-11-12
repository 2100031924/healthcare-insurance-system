package com.healthcare.claimsystem.controller;

import com.healthcare.claimsystem.dto.common.PageResponse;
import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationDecisionRequest;
import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationRequest;
import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationResponse;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.service.PreAuthorizationService;
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
@RequestMapping("/api/v1/pre-authorizations")
@RequiredArgsConstructor
@Tag(name = "Pre-Authorization Management", description = "Treatment approval request and decision workflow")
public class PreAuthorizationController {

    private final PreAuthorizationService preAuthorizationService;

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL')")
    @Operation(summary = "Submit a pre-authorization request")
    public ResponseEntity<PreAuthorizationResponse> request(@Valid @RequestBody PreAuthorizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preAuthorizationService.request(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "Get a pre-authorization by identifier")
    public ResponseEntity<PreAuthorizationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(preAuthorizationService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "List pre-authorizations with pagination, sorting and filters")
    public ResponseEntity<PageResponse<PreAuthorizationResponse>> list(
            @RequestParam(required = false) PreAuthorizationStatus status,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) TreatmentCategory treatmentCategory,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(preAuthorizationService.list(
                status,
                patientId,
                hospitalId,
                policyNumber,
                referenceNumber,
                treatmentCategory,
                fromDate,
                toDate,
                pageable
        )));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('INSURANCE_OFFICER')")
    @Operation(summary = "Move a pre-authorization into review")
    public ResponseEntity<PreAuthorizationResponse> review(@PathVariable Long id) {
        return ResponseEntity.ok(preAuthorizationService.review(id));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('INSURANCE_OFFICER')")
    @Operation(summary = "Approve, partially approve or reject a pre-authorization")
    public ResponseEntity<PreAuthorizationResponse> decide(
            @PathVariable Long id,
            @Valid @RequestBody PreAuthorizationDecisionRequest request
    ) {
        return ResponseEntity.ok(preAuthorizationService.decide(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('HOSPITAL')")
    @Operation(summary = "Cancel a pending pre-authorization request")
    public ResponseEntity<PreAuthorizationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(preAuthorizationService.cancel(id));
    }
}
