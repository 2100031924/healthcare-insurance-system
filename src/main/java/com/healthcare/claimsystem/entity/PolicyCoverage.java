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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "policy_coverages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_policy_coverage_type",
                columnNames = {"policy_id", "coverage_type"}
        ),
        indexes = @Index(name = "idx_coverages_policy", columnList = "policy_id")
)
@Getter
@Setter
@NoArgsConstructor
public class PolicyCoverage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_type", nullable = false, length = 30)
    private TreatmentCategory coverageType;

    @Column(name = "coverage_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal coverageLimit;

    @Column(name = "utilized_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
