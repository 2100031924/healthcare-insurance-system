package com.healthcare.claimsystem.repository;

import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PolicyCoverageRepository extends JpaRepository<PolicyCoverage, Long> {

    List<PolicyCoverage> findByPolicyIdOrderByCoverageTypeAsc(Long policyId);

    Optional<PolicyCoverage> findByPolicyIdAndCoverageType(Long policyId, TreatmentCategory coverageType);

    Optional<PolicyCoverage> findByIdAndPolicyId(Long id, Long policyId);

    boolean existsByPolicyIdAndCoverageType(Long policyId, TreatmentCategory coverageType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PolicyCoverage c where c.policy.id = :policyId and c.coverageType = :coverageType")
    Optional<PolicyCoverage> findByPolicyIdAndCoverageTypeForUpdate(
            @Param("policyId") Long policyId,
            @Param("coverageType") TreatmentCategory coverageType
    );
}
