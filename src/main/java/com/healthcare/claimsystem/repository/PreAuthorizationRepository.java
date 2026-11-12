package com.healthcare.claimsystem.repository;

import com.healthcare.claimsystem.entity.PreAuthorization;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface PreAuthorizationRepository extends JpaRepository<PreAuthorization, Long>, JpaSpecificationExecutor<PreAuthorization> {

    Optional<PreAuthorization> findByReferenceNumber(String referenceNumber);

    boolean existsByReferenceNumber(String referenceNumber);

    boolean existsByPatientIdAndProcedureCodeAndStatusIn(
            Long patientId,
            String procedureCode,
            Collection<PreAuthorizationStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PreAuthorization p where p.id = :id")
    Optional<PreAuthorization> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PreAuthorization p set p.status = :expired where p.status in :statuses and p.validUntil < :today")
    int expirePreAuthorizations(
            @Param("statuses") Collection<PreAuthorizationStatus> statuses,
            @Param("expired") PreAuthorizationStatus expired,
            @Param("today") LocalDate today
    );
}
