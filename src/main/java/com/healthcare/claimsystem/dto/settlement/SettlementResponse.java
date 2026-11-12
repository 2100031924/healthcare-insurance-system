package com.healthcare.claimsystem.dto.settlement;

import com.healthcare.claimsystem.entity.enums.PaymentMode;
import com.healthcare.claimsystem.entity.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SettlementResponse(
        Long id,
        String settlementNumber,
        Long claimId,
        String claimNumber,
        String patientCode,
        String patientFullName,
        String policyNumber,
        BigDecimal approvedAmount,
        BigDecimal paidAmount,
        BigDecimal patientResponsibility,
        PaymentMode paymentMode,
        String paymentReference,
        SettlementStatus status,
        String processedByUsername,
        Instant processedAt,
        String remarks,
        Instant createdAt
) {
}
