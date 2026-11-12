package com.healthcare.claimsystem.mapper;

import com.healthcare.claimsystem.dto.claim.ClaimResponse;
import com.healthcare.claimsystem.entity.Claim;

public final class ClaimMapper {

    private ClaimMapper() {
    }

    public static ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getPatient().getId(),
                claim.getPatient().getPatientCode(),
                claim.getPatient().getFirstName() + " " + claim.getPatient().getLastName(),
                claim.getPolicy().getId(),
                claim.getPolicy().getPolicyNumber(),
                claim.getHospital().getId(),
                claim.getHospital().getHospitalCode(),
                claim.getHospital().getName(),
                claim.getPreAuthorization() == null ? null : claim.getPreAuthorization().getId(),
                claim.getPreAuthorization() == null ? null : claim.getPreAuthorization().getReferenceNumber(),
                claim.getTreatingDoctor(),
                claim.getDiagnosisCode(),
                claim.getProcedureCode(),
                claim.getTreatmentCategory(),
                claim.getAdmissionDate(),
                claim.getDischargeDate(),
                claim.getClaimDate(),
                claim.getClaimedAmount(),
                claim.getInvoiceNumber(),
                claim.getDescription(),
                claim.getStatus(),
                claim.getDeductibleApplied(),
                claim.getCoPaymentAmount(),
                claim.getNonCoveredAmount(),
                claim.getApprovedAmount(),
                claim.getPatientResponsibility(),
                claim.getRejectionReason(),
                claim.getAdjudicationNotes(),
                claim.getSubmittedBy() == null ? null : claim.getSubmittedBy().getUsername(),
                claim.getReviewedBy() == null ? null : claim.getReviewedBy().getUsername(),
                claim.getAdjudicatedBy() == null ? null : claim.getAdjudicatedBy().getUsername(),
                claim.getReviewedAt(),
                claim.getAdjudicatedAt(),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }
}
