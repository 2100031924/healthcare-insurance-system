package com.healthcare.claimsystem.controller;

import com.healthcare.claimsystem.dto.common.PageResponse;
import com.healthcare.claimsystem.dto.settlement.SettlementRequest;
import com.healthcare.claimsystem.dto.settlement.SettlementResponse;
import com.healthcare.claimsystem.entity.enums.SettlementStatus;
import com.healthcare.claimsystem.service.SettlementService;
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
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@Tag(name = "Claim Settlement", description = "Claim payment calculation and settlement processing")
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @Operation(summary = "Process a claim settlement")
    public ResponseEntity<SettlementResponse> settle(@Valid @RequestBody SettlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(settlementService.settle(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    @Operation(summary = "Get a settlement by identifier")
    public ResponseEntity<SettlementResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(settlementService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    @Operation(summary = "List settlements with pagination, sorting and filters")
    public ResponseEntity<PageResponse<SettlementResponse>> list(
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) Long claimId,
            @RequestParam(required = false) String claimNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(settlementService.list(
                status,
                claimId,
                claimNumber,
                fromDate,
                toDate,
                pageable
        )));
    }
}
