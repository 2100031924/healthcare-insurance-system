package com.healthcare.claimsystem.rules;

import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class InsuranceRulesEngine {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public AdjudicationCalculation calculate(
            BigDecimal claimedAmount,
            LocalDate serviceDate,
            InsurancePolicy policy,
            PolicyCoverage coverage,
            boolean excluded,
            String exclusionReason,
            BigDecimal preAuthorizationLimit
    ) {
        List<String> remarks = new ArrayList<>();
        BigDecimal claimed = money(claimedAmount);

        if (policy.getStatus() == PolicyStatus.CANCELLED || policy.getStatus() == PolicyStatus.SUSPENDED) {
            remarks.add("Policy is " + policy.getStatus().name().toLowerCase());
            return notCovered(claimed, remarks);
        }
        if (serviceDate.isBefore(policy.getStartDate()) || serviceDate.isAfter(policy.getEndDate())) {
            remarks.add("Service date is outside the policy period");
            return notCovered(claimed, remarks);
        }
        LocalDate waitingPeriodEnd = policy.getStartDate().plusDays(policy.getWaitingPeriodDays());
        if (serviceDate.isBefore(waitingPeriodEnd)) {
            remarks.add("Policy waiting period ends on " + waitingPeriodEnd);
            return notCovered(claimed, remarks);
        }
        if (excluded) {
            remarks.add(exclusionReason == null ? "Treatment category is excluded" : exclusionReason);
            return notCovered(claimed, remarks);
        }
        if (coverage == null || !coverage.isActive()) {
            remarks.add("No active coverage exists for the treatment category");
            return notCovered(claimed, remarks);
        }

        BigDecimal policyRemaining = policy.getSumInsured().subtract(policy.getUtilizedAmount()).max(BigDecimal.ZERO);
        BigDecimal categoryRemaining = coverage.getCoverageLimit().subtract(coverage.getUtilizedAmount()).max(BigDecimal.ZERO);
        if (policyRemaining.signum() <= 0) {
            remarks.add("Policy coverage limit has been exhausted");
            return notCovered(claimed, remarks);
        }
        if (categoryRemaining.signum() <= 0) {
            remarks.add("Treatment category coverage limit has been exhausted");
            return notCovered(claimed, remarks);
        }

        BigDecimal eligible = claimed.min(policyRemaining).min(categoryRemaining);
        if (eligible.compareTo(claimed) < 0) {
            remarks.add("Coverage limits reduced the eligible amount");
        }

        if (preAuthorizationLimit != null) {
            if (preAuthorizationLimit.signum() <= 0) {
                remarks.add("Pre-authorization has no approved amount");
                return notCovered(claimed, remarks);
            }
            if (preAuthorizationLimit.compareTo(eligible) < 0) {
                eligible = preAuthorizationLimit;
                remarks.add("Pre-authorization approved amount applied");
            }
        }

        BigDecimal deductible = money(policy.getDeductibleAmount()).min(eligible);
        if (deductible.signum() > 0) {
            remarks.add("Deductible applied: " + deductible.toPlainString());
        }
        BigDecimal afterDeductible = eligible.subtract(deductible);
        BigDecimal coPayment = afterDeductible
                .multiply(money(policy.getCoPaymentPercentage()))
                .divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
        coPayment = money(coPayment);
        if (coPayment.signum() > 0) {
            remarks.add("Co-payment applied: " + coPayment.toPlainString());
        }
        BigDecimal approved = money(afterDeductible.subtract(coPayment));
        BigDecimal nonCovered = money(claimed.subtract(eligible));
        BigDecimal patientResponsibility = money(claimed.subtract(approved));

        return new AdjudicationCalculation(
                claimed,
                money(eligible),
                nonCovered,
                money(deductible),
                coPayment,
                approved,
                patientResponsibility,
                true,
                List.copyOf(remarks)
        );
    }

    private AdjudicationCalculation notCovered(BigDecimal claimed, List<String> remarks) {
        return new AdjudicationCalculation(
                claimed,
                BigDecimal.ZERO.setScale(2),
                claimed,
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                claimed,
                false,
                List.copyOf(remarks)
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
