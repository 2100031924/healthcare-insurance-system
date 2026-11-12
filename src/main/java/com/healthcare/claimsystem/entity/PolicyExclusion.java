package com.healthcare.claimsystem.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "policy_exclusions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_policy_exclusion_type",
                columnNames = {"policy_id", "treatment_category"}
        ),
        indexes = @Index(name = "idx_exclusions_policy", columnList = "policy_id")
)
@Getter
@Setter
@NoArgsConstructor
public class PolicyExclusion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_category", nullable = false, length = 30)
    private TreatmentCategory treatmentCategory;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;
}
