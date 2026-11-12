package com.healthcare.claimsystem.repository;

import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long>, JpaSpecificationExecutor<InsurancePolicy> {

    Optional<InsurancePolicy> findByPolicyNumber(String policyNumber);

    boolean existsByPolicyNumber(String policyNumber);

    boolean existsByPolicyNumberAndIdNot(String policyNumber, Long id);

    List<InsurancePolicy> findByPatientIdOrderByEndDateDesc(Long patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from InsurancePolicy p where p.id = :id")
    Optional<InsurancePolicy> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InsurancePolicy p set p.status = :expired where p.status = :active and p.endDate < :today")
    int expirePolicies(
            @Param("active") PolicyStatus active,
            @Param("expired") PolicyStatus expired,
            @Param("today") LocalDate today
    );
}
