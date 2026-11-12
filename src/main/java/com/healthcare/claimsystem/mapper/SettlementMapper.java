package com.healthcare.claimsystem.mapper;

import com.healthcare.claimsystem.dto.settlement.SettlementResponse;
import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.ClaimSettlement;

public final class SettlementMapper {

    private SettlementMapper() {
    }

    public static SettlementResponse toResponse(ClaimSettlement settlement) {
        Claim claim = settlement.getClaim();
        return new SettlementResponse(
                settlement.getId(),
                settlement.getSettlementNumber(),
                claim.getId(),
                claim.getClaimNumber(),
                claim.getPatient().getPatientCode(),
                claim.getPatient().getFirstName() + " " + claim.getPatient().getLastName(),
                claim.getPolicy().getPolicyNumber(),
                settlement.getApprovedAmount(),
                settlement.getPaidAmount(),
                settlement.getPatientResponsibility(),
                settlement.getPaymentMode(),
                settlement.getPaymentReference(),
                settlement.getStatus(),
                settlement.getProcessedBy() == null ? null : settlement.getProcessedBy().getUsername(),
                settlement.getProcessedAt(),
                settlement.getRemarks(),
                settlement.getCreatedAt()
        );
    }
}
