package com.healthcare.claimsystem.entity;

import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "insurance_policies",
        indexes = {
                @Index(name = "idx_policies_patient", columnList = "patient_id"),
                @Index(name = "idx_policies_status", columnList = "status"),
                @Index(name = "idx_policies_end_date", columnList = "end_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InsurancePolicy extends BaseEntity {

    @Column(name = "policy_number", nullable = false, unique = true, length = 50)
    private String policyNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "provider_name", nullable = false, length = 150)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 30)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PolicyStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "sum_insured", nullable = false, precision = 14, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "utilized_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    @Column(name = "deductible_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal deductibleAmount = BigDecimal.ZERO;

    @Column(name = "co_payment_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal coPaymentPercentage = BigDecimal.ZERO;

    @Column(name = "waiting_period_days", nullable = false)
    private int waitingPeriodDays;

    @Column(name = "premium_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal premiumAmount = BigDecimal.ZERO;

    @Column(name = "renewal_count", nullable = false)
    private int renewalCount;

    @Column(name = "last_renewed_at")
    private Instant lastRenewedAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
