package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationDecisionRequest;
import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationRequest;
import com.healthcare.claimsystem.dto.preauthorization.PreAuthorizationResponse;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PreAuthorizationService {

    PreAuthorizationResponse request(PreAuthorizationRequest request);

    PreAuthorizationResponse get(Long id);

    Page<PreAuthorizationResponse> list(
            PreAuthorizationStatus status,
            Long patientId,
            Long hospitalId,
            String policyNumber,
            String referenceNumber,
            TreatmentCategory treatmentCategory,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    PreAuthorizationResponse review(Long id);

    PreAuthorizationResponse decide(Long id, PreAuthorizationDecisionRequest request);

    PreAuthorizationResponse cancel(Long id);
}
