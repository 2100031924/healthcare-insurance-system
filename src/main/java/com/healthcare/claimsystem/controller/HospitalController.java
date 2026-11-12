package com.healthcare.claimsystem.controller;

import com.healthcare.claimsystem.dto.common.PageResponse;
import com.healthcare.claimsystem.dto.hospital.HospitalRequest;
import com.healthcare.claimsystem.dto.hospital.HospitalResponse;
import com.healthcare.claimsystem.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
@Tag(name = "Hospital Management", description = "Hospital registration and administration")
public class HospitalController {

    private final HospitalService hospitalService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a hospital")
    public ResponseEntity<HospitalResponse> create(@Valid @RequestBody HospitalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a hospital")
    public ResponseEntity<HospitalResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody HospitalRequest request
    ) {
        return ResponseEntity.ok(hospitalService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "Get a hospital by identifier")
    public ResponseEntity<HospitalResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(hospitalService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "List hospitals with pagination, sorting and filters")
    public ResponseEntity<PageResponse<HospitalResponse>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(hospitalService.list(active, search, pageable)));
    }
}
