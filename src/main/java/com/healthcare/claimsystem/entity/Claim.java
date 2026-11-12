package com.healthcare.claimsystem.entity;

import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "claims",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_claims_number", columnNames = "claim_number"),
                @UniqueConstraint(name = "uk_claims_fingerprint", columnNames = "claim_fingerprint"),
                @UniqueConstraint(name = "uk_claims_hospital_invoice", columnNames = {"hospital_id", "invoice_number"})
        },
        indexes = {
                @Index(name = "idx_claims_patient", columnList = "patient_id"),
                @Index(name = "idx_claims_hospital", columnList = "hospital_id"),
                @Index(name = "idx_claims_policy", columnList = "policy_id"),
                @Index(name = "idx_claims_status", columnList = "status"),
                @Index(name = "idx_claims_claim_date", columnList = "claim_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Claim extends BaseEntity {

    @Column(name = "claim_number", nullable = false, unique = true, length = 40)
    private String claimNumber;

    @Column(name = "claim_fingerprint", nullable = false, unique = true, length = 64)
    private String claimFingerprint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_authorization_id")
    private PreAuthorization preAuthorization;

    @Column(name = "treating_doctor", nullable = false, length = 120)
    private String treatingDoctor;

    @Column(name = "diagnosis_code", nullable = false, length = 20)
    private String diagnosisCode;

    @Column(name = "procedure_code", nullable = false, length = 20)
    private String procedureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_category", nullable = false, length = 30)
    private TreatmentCategory treatmentCategory;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "discharge_date")
    private LocalDate dischargeDate;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(name = "claimed_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "invoice_number", nullable = false, length = 80)
    private String invoiceNumber;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ClaimStatus status;

    @Column(name = "deductible_applied", nullable = false, precision = 14, scale = 2)
    private BigDecimal deductibleApplied = BigDecimal.ZERO;

    @Column(name = "co_payment_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal coPaymentAmount = BigDecimal.ZERO;

    @Column(name = "non_covered_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal nonCoveredAmount = BigDecimal.ZERO;

    @Column(name = "approved_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    @Column(name = "patient_responsibility", nullable = false, precision = 14, scale = 2)
    private BigDecimal patientResponsibility = BigDecimal.ZERO;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "adjudication_notes", length = 1000)
    private String adjudicationNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjudicated_by")
    private User adjudicatedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "adjudicated_at")
    private Instant adjudicatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
