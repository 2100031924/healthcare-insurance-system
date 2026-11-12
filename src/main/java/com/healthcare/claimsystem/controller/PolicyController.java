package com.healthcare.claimsystem.controller;

import com.healthcare.claimsystem.dto.common.PageResponse;
import com.healthcare.claimsystem.dto.policy.CoverageRequest;
import com.healthcare.claimsystem.dto.policy.CoverageResponse;
import com.healthcare.claimsystem.dto.policy.ExclusionRequest;
import com.healthcare.claimsystem.dto.policy.ExclusionResponse;
import com.healthcare.claimsystem.dto.policy.PolicyRenewRequest;
import com.healthcare.claimsystem.dto.policy.PolicyRequest;
import com.healthcare.claimsystem.dto.policy.PolicyResponse;
import com.healthcare.claimsystem.dto.policy.PolicyStatusRequest;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import com.healthcare.claimsystem.service.PolicyService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Insurance Policy Management", description = "Policy lifecycle, coverage and exclusion management")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSURANCE_OFFICER')")
    @Operation(summary = "Register an insurance policy")
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSURANCE_OFFICER')")
    @Operation(summary = "Update an insurance policy")
    public ResponseEntity<PolicyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRequest request
    ) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "Get an insurance policy by identifier")
    public ResponseEntity<PolicyResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "List insurance policies with pagination, sorting and filters")
    public ResponseEntity<PageResponse<PolicyResponse>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) PolicyStatus status,
            @RequestParam(required = false) PolicyType policyType,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(required = false) String providerName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(policyService.list(
                patientId,
                status,
                policyType,
                policyNumber,
                providerName,
                expiringBefore,
                pageable
        )));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','INSURANCE_OFFICER')")
    @Operation(summary = "Renew an insurance policy")
    public ResponseEntity<PolicyResponse> renew(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRenewRequest request
    ) {
        return ResponseEntity.ok(policyService.renew(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','INSURANCE_OFFICER')")
    @Operation(summary = "Change an insurance policy status")
    public ResponseEntity<PolicyResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody PolicyStatusRequest request
    ) {
        return ResponseEntity.ok(policyService.changeStatus(id, request));
    }

    @PostMapping("/{id}/coverages")
    @PreAuthorize("hasAnyRole('ADMIN','INSURANCE_OFFICER')")
    @Operation(summary = "Add a coverage to an insurance policy")
    public ResponseEntity<CoverageResponse> addCoverage(
            @PathVariable Long id,
            @Valid @RequestBody CoverageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.addCoverage(id, request));
    }

    @PutMapping("/{id}/coverages/{coverageId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSURANCE_OFFICER')")
    @Operation(summary = "Update a policy coverage")
    public ResponseEntity<CoverageResponse> updateCoverage(
            @PathVariable Long id,
            @PathVariable Long coverageId,
            @Valid @RequestBody CoverageRequest request
    ) {
        return ResponseEntity.ok(policyService.updateCoverage(id, coverageId, request));
    }

    @GetMapping("/{id}/coverages")
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "List policy coverages")
    public ResponseEntity<List<CoverageResponse>> getCoverages(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getCoverages(id));
    }

    @PostMapping("/{id}/exclusions")
    @PreAuthorize("hasAnyRole('ADMIN','INSURANCE_OFFICER')")
    @Operation(summary = "Add a policy exclusion")
    public ResponseEntity<ExclusionResponse> addExclusion(
            @PathVariable Long id,
            @Valid @RequestBody ExclusionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.addExclusion(id, request));
    }

    @GetMapping("/{id}/exclusions")
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "List policy exclusions")
    public ResponseEntity<List<ExclusionResponse>> getExclusions(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getExclusions(id));
    }

    @DeleteMapping("/{id}/exclusions/{exclusionId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSURANCE_OFFICER')")
    @Operation(summary = "Remove a policy exclusion")
    public ResponseEntity<Void> removeExclusion(
            @PathVariable Long id,
            @PathVariable Long exclusionId
    ) {
        policyService.removeExclusion(id, exclusionId);
        return ResponseEntity.noContent().build();
    }
}
