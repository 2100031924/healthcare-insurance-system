package com.healthcare.claimsystem.dto.preauthorization;

import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PreAuthorizationResponse(
        Long id,
        String referenceNumber,
        Long patientId,
        String patientCode,
        String patientFullName,
        Long policyId,
        String policyNumber,
        Long hospitalId,
        String hospitalCode,
        String hospitalName,
        String treatingDoctor,
        String diagnosisCode,
        String procedureCode,
        TreatmentCategory treatmentCategory,
        String treatmentDescription,
        BigDecimal estimatedCost,
        LocalDate requestedDate,
        PreAuthorizationStatus status,
        BigDecimal approvedAmount,
        LocalDate validUntil,
        String remarks,
        String requestedByUsername,
        String reviewedByUsername,
        Instant decisionDate,
        Instant createdAt,
        Instant updatedAt
) {
}
