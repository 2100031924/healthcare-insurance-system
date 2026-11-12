package com.healthcare.claimsystem.entity;

import com.healthcare.claimsystem.entity.enums.PaymentMode;
import com.healthcare.claimsystem.entity.enums.SettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "claim_settlements",
        indexes = {
                @Index(name = "idx_settlements_claim", columnList = "claim_id"),
                @Index(name = "idx_settlements_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ClaimSettlement extends BaseEntity {

    @Column(name = "settlement_number", nullable = false, unique = true, length = 40)
    private String settlementNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private Claim claim;

    @Column(name = "approved_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "patient_responsibility", nullable = false, precision = 14, scale = 2)
    private BigDecimal patientResponsibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 30)
    private PaymentMode paymentMode;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SettlementStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
