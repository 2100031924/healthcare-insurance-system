package com.healthcare.claimsystem.dto.claim;

import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ClaimResponse(
        Long id,
        String claimNumber,
        Long patientId,
        String patientCode,
        String patientFullName,
        Long policyId,
        String policyNumber,
        Long hospitalId,
        String hospitalCode,
        String hospitalName,
        Long preAuthorizationId,
        String preAuthorizationNumber,
        String treatingDoctor,
        String diagnosisCode,
        String procedureCode,
        TreatmentCategory treatmentCategory,
        LocalDate admissionDate,
        LocalDate dischargeDate,
        LocalDate claimDate,
        BigDecimal claimedAmount,
        String invoiceNumber,
        String description,
        ClaimStatus status,
        BigDecimal deductibleApplied,
        BigDecimal coPaymentAmount,
        BigDecimal nonCoveredAmount,
        BigDecimal approvedAmount,
        BigDecimal patientResponsibility,
        String rejectionReason,
        String adjudicationNotes,
        String submittedByUsername,
        String reviewedByUsername,
        String adjudicatedByUsername,
        Instant reviewedAt,
        Instant adjudicatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
