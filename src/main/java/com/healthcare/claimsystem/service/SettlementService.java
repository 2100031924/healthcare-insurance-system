package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.settlement.SettlementRequest;
import com.healthcare.claimsystem.dto.settlement.SettlementResponse;
import com.healthcare.claimsystem.entity.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SettlementService {

    SettlementResponse settle(SettlementRequest request);

    SettlementResponse get(Long id);

    Page<SettlementResponse> list(
            SettlementStatus status,
            Long claimId,
            String claimNumber,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );
}
