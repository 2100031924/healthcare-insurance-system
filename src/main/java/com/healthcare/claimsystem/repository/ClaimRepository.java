package com.healthcare.claimsystem.repository;

import com.healthcare.claimsystem.entity.Claim;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    Optional<Claim> findByClaimFingerprint(String claimFingerprint);

    boolean existsByClaimFingerprint(String claimFingerprint);

    boolean existsByHospitalIdAndInvoiceNumber(Long hospitalId, String invoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Claim c where c.id = :id")
    Optional<Claim> findByIdForUpdate(@Param("id") Long id);
}
