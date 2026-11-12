package com.healthcare.claimsystem.mapper;

import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationResponse;
import com.healthcare.claimsystem.entity.PreAuthorization;

public final class PreAuthorizationMapper {

    private PreAuthorizationMapper() {
    }

    public static PreAuthorizationResponse toResponse(PreAuthorization preAuthorization) {
        return new PreAuthorizationResponse(
                preAuthorization.getId(),
                preAuthorization.getReferenceNumber(),
                preAuthorization.getPatient().getId(),
                preAuthorization.getPatient().getPatientCode(),
                preAuthorization.getPatient().getFirstName() + " " + preAuthorization.getPatient().getLastName(),
                preAuthorization.getPolicy().getId(),
                preAuthorization.getPolicy().getPolicyNumber(),
                preAuthorization.getHospital().getId(),
                preAuthorization.getHospital().getHospitalCode(),
                preAuthorization.getHospital().getName(),
                preAuthorization.getTreatingDoctor(),
                preAuthorization.getDiagnosisCode(),
                preAuthorization.getProcedureCode(),
                preAuthorization.getTreatmentCategory(),
                preAuthorization.getTreatmentDescription(),
                preAuthorization.getEstimatedCost(),
                preAuthorization.getRequestedDate(),
                preAuthorization.getStatus(),
                preAuthorization.getApprovedAmount(),
                preAuthorization.getValidUntil(),
                preAuthorization.getRemarks(),
                preAuthorization.getRequestedBy() == null ? null : preAuthorization.getRequestedBy().getUsername(),
                preAuthorization.getReviewedBy() == null ? null : preAuthorization.getReviewedBy().getUsername(),
                preAuthorization.getDecisionDate(),
                preAuthorization.getCreatedAt(),
                preAuthorization.getUpdatedAt()
        );
    }
}
