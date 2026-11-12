package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.eligibility.EligibilityRequest;
import com.healthcare.claimsystem.dto.eligibility.EligibilityResponse;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PolicyExclusion;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.rules.AdjudicationCalculation;
import com.healthcare.claimsystem.rules.InsuranceRulesEngine;
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EligibilityServiceImpl implements EligibilityService {

    private final PatientRepository patientRepository;
    private final InsurancePolicyRepository policyRepository;
    private final PolicyCoverageRepository coverageRepository;
    private final PolicyExclusionRepository exclusionRepository;
    private final InsuranceRulesEngine rulesEngine;
    private final AuditService auditService;

    @Override
    @Transactional
    public EligibilityResponse verify(EligibilityRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));
        InsurancePolicy policy = policyRepository.findByPolicyNumber(request.policyNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "policyNumber", request.policyNumber()));
        if (!policy.getPatient().getId().equals(patient.getId())) {
            throw new BusinessRuleException("Policy does not belong to the specified patient");
        }
        if (!patient.isActive()) {
            throw new BusinessRuleException("Patient is inactive");
        }

        LocalDate serviceDate = request.serviceDate() == null ? LocalDate.now() : request.serviceDate();
        Optional<PolicyCoverage> coverageOptional = coverageRepository
                .findByPolicyIdAndCoverageType(policy.getId(), request.treatmentCategory());
        Optional<PolicyExclusion> exclusionOptional = exclusionRepository
                .findByPolicyIdAndTreatmentCategory(policy.getId(), request.treatmentCategory());
        LocalDate waitingPeriodEnd = policy.getStartDate().plusDays(policy.getWaitingPeriodDays());
        BigDecimal remainingCoverage = policy.getSumInsured()
                .subtract(policy.getUtilizedAmount())
                .max(BigDecimal.ZERO);
        BigDecimal categoryLimit = coverageOptional.map(PolicyCoverage::getCoverageLimit).orElse(null);
        BigDecimal categoryUtilized = coverageOptional.map(PolicyCoverage::getUtilizedAmount).orElse(null);
        BigDecimal categoryRemaining = coverageOptional
                .map(coverage -> coverage.getCoverageLimit().subtract(coverage.getUtilizedAmount()).max(BigDecimal.ZERO))
                .orElse(null);

        List<String> reasons = new ArrayList<>();
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            reasons.add("Policy status is " + policy.getStatus().name().toLowerCase());
        }
        if (serviceDate.isBefore(policy.getStartDate()) || serviceDate.isAfter(policy.getEndDate())) {
            reasons.add("Service date is outside the policy period");
        }
        if (serviceDate.isBefore(waitingPeriodEnd)) {
            reasons.add("Policy waiting period ends on " + waitingPeriodEnd);
        }
        if (coverageOptional.isEmpty()) {
            reasons.add("No coverage exists for " + request.treatmentCategory());
        } else if (!coverageOptional.get().isActive()) {
            reasons.add("Coverage for " + request.treatmentCategory() + " is inactive");
        } else if (categoryRemaining.signum() <= 0) {
            reasons.add("Coverage limit for " + request.treatmentCategory() + " is exhausted");
        }
        if (remainingCoverage.signum() <= 0) {
            reasons.add("Policy coverage limit is exhausted");
        }
        exclusionOptional.ifPresent(exclusion -> reasons.add(exclusion.getReason()));

        boolean eligible = reasons.isEmpty();
        BigDecimal estimatedPayable = null;
        BigDecimal estimatedPatientResponsibility = null;
        if (request.estimatedCost() != null) {
            if (eligible) {
                PolicyCoverage coverage = coverageOptional.orElse(null);
                AdjudicationCalculation calculation = rulesEngine.calculate(
                        request.estimatedCost(),
                        serviceDate,
                        policy,
                        coverage,
                        false,
                        null,
                        null
                );
                estimatedPayable = calculation.approvedAmount();
                estimatedPatientResponsibility = calculation.patientResponsibility();
            } else {
                estimatedPayable = BigDecimal.ZERO.setScale(2);
                estimatedPatientResponsibility = request.estimatedCost();
            }
        }

        auditService.recordEntity(
                AuditEntityType.POLICY,
                policy.getId(),
                AuditAction.ELIGIBILITY_VERIFIED,
                "Eligibility verified for patient " + patient.getPatientCode()
                        + ", category " + request.treatmentCategory()
                        + ", eligible " + eligible
                        + (reasons.isEmpty() ? "" : ", reasons: " + String.join("; ", reasons))
        );

        return new EligibilityResponse(
                eligible,
                patient.getId(),
                patient.getPatientCode(),
                patient.getFirstName() + " " + patient.getLastName(),
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getStatus(),
                policy.getStartDate(),
                policy.getEndDate(),
                serviceDate,
                waitingPeriodEnd,
                policy.getSumInsured(),
                policy.getUtilizedAmount(),
                remainingCoverage,
                request.treatmentCategory(),
                categoryLimit,
                categoryUtilized,
                categoryRemaining,
                policy.getDeductibleAmount(),
                policy.getCoPaymentPercentage(),
                request.estimatedCost(),
                estimatedPayable,
                estimatedPatientResponsibility,
                List.copyOf(reasons),
                Instant.now()
        );
    }
}
