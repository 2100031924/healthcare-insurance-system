package com.healthcare.claimsystem.controller;

import com.healthcare.claimsystem.dto.eligibility.EligibilityRequest;
import com.healthcare.claimsystem.dto.eligibility.EligibilityResponse;
import com.healthcare.claimsystem.service.EligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/eligibility")
@RequiredArgsConstructor
@Tag(name = "Patient Eligibility", description = "Policy eligibility and coverage verification")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMIN','HOSPITAL','INSURANCE_OFFICER','FINANCE_OFFICER')")
    @Operation(summary = "Verify patient eligibility for a policy and treatment category")
    public ResponseEntity<EligibilityResponse> verify(@Valid @RequestBody EligibilityRequest request) {
        return ResponseEntity.ok(eligibilityService.verify(request));
    }
}
