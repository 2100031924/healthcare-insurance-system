package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.policy.CoverageRequest;
import com.healthcare.claimsystem.dto.policy.CoverageResponse;
import com.healthcare.claimsystem.dto.policy.ExclusionRequest;
import com.healthcare.claimsystem.dto.policy.ExclusionResponse;
import com.healthcare.claimsystem.dto.policy.PolicyRenewRequest;
import com.healthcare.claimsystem.dto.policy.PolicyRequest;
import com.healthcare.claimsystem.dto.policy.PolicyResponse;
import com.healthcare.claimsystem.dto.policy.PolicyStatusRequest;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface PolicyService {

    PolicyResponse create(PolicyRequest request);

    PolicyResponse update(Long id, PolicyRequest request);

    PolicyResponse get(Long id);

    Page<PolicyResponse> list(
            Long patientId,
            PolicyStatus status,
            PolicyType policyType,
            String policyNumber,
            String providerName,
            LocalDate expiringBefore,
            Pageable pageable
    );

    PolicyResponse renew(Long id, PolicyRenewRequest request);

    PolicyResponse changeStatus(Long id, PolicyStatusRequest request);

    CoverageResponse addCoverage(Long policyId, CoverageRequest request);

    CoverageResponse updateCoverage(Long policyId, Long coverageId, CoverageRequest request);

    List<CoverageResponse> getCoverages(Long policyId);

    ExclusionResponse addExclusion(Long policyId, ExclusionRequest request);

    List<ExclusionResponse> getExclusions(Long policyId);

    void removeExclusion(Long policyId, Long exclusionId);
}
