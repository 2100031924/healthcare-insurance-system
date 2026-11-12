package com.healthcare.claimsystem.repository;

import com.healthcare.claimsystem.entity.ClaimSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ClaimSettlementRepository extends JpaRepository<ClaimSettlement, Long>, JpaSpecificationExecutor<ClaimSettlement> {

    Optional<ClaimSettlement> findBySettlementNumber(String settlementNumber);

    Optional<ClaimSettlement> findByClaimId(Long claimId);

    boolean existsByClaimId(Long claimId);

    boolean existsBySettlementNumber(String settlementNumber);

    boolean existsByPaymentReference(String paymentReference);
}
