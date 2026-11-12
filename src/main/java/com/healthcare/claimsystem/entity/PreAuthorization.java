package com.healthcare.claimsystem.entity;

import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "pre_authorizations",
        indexes = {
                @Index(name = "idx_preauth_patient", columnList = "patient_id"),
                @Index(name = "idx_preauth_hospital", columnList = "hospital_id"),
                @Index(name = "idx_preauth_policy", columnList = "policy_id"),
                @Index(name = "idx_preauth_status", columnList = "status"),
                @Index(name = "idx_preauth_requested_date", columnList = "requested_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PreAuthorization extends BaseEntity {

    @Column(name = "reference_number", nullable = false, unique = true, length = 40)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "treating_doctor", nullable = false, length = 120)
    private String treatingDoctor;

    @Column(name = "diagnosis_code", nullable = false, length = 20)
    private String diagnosisCode;

    @Column(name = "procedure_code", nullable = false, length = 20)
    private String procedureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_category", nullable = false, length = 30)
    private TreatmentCategory treatmentCategory;

    @Column(name = "treatment_description", nullable = false, length = 500)
    private String treatmentDescription;

    @Column(name = "estimated_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "requested_date", nullable = false)
    private LocalDate requestedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PreAuthorizationStatus status;

    @Column(name = "approved_amount", precision = 14, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "decision_date")
    private Instant decisionDate;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
