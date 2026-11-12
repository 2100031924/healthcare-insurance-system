package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.audit.AuditLogResponse;
import com.healthcare.claimsystem.dto.claim.AdjudicationPreviewResponse;
import com.healthcare.claimsystem.dto.claim.ClaimAdjudicationRequest;
import com.healthcare.claimsystem.dto.claim.ClaimRequest;
import com.healthcare.claimsystem.dto.claim.ClaimResponse;
import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ClaimService {

    ClaimResponse submit(ClaimRequest request);

    ClaimResponse get(Long id);

    Page<ClaimResponse> list(
            ClaimStatus status,
            Long patientId,
            Long hospitalId,
            String policyNumber,
            String claimNumber,
            TreatmentCategory treatmentCategory,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    ClaimResponse review(Long id);

    AdjudicationPreviewResponse previewAdjudication(Long id);

    ClaimResponse adjudicate(Long id, ClaimAdjudicationRequest request);

    ClaimResponse cancel(Long id);

    Page<AuditLogResponse> getAudit(Long id, Pageable pageable);
}
