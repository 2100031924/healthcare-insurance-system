package com.healthcare.claimsystem.rules;

import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InsuranceRulesEngineTest {

    private final InsuranceRulesEngine rulesEngine = new InsuranceRulesEngine();

    private InsurancePolicy policy;
    private PolicyCoverage coverage;
    private LocalDate serviceDate;

    @BeforeEach
    void setUp() {
        serviceDate = LocalDate.now();
        policy = new InsurancePolicy();
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(serviceDate.minusDays(100));
        policy.setEndDate(serviceDate.plusDays(200));
        policy.setSumInsured(new BigDecimal("100000.00"));
        policy.setUtilizedAmount(new BigDecimal("0.00"));
        policy.setDeductibleAmount(new BigDecimal("1000.00"));
        policy.setCoPaymentPercentage(new BigDecimal("10.00"));
        policy.setWaitingPeriodDays(30);

        coverage = new PolicyCoverage();
        coverage.setCoverageType(TreatmentCategory.HOSPITALIZATION);
        coverage.setCoverageLimit(new BigDecimal("50000.00"));
        coverage.setUtilizedAmount(new BigDecimal("0.00"));
        coverage.setActive(true);
    }

    @Test
    void calculatesDeductibleAndCoPayment() {
        AdjudicationCalculation result = rulesEngine.calculate(
                new BigDecimal("10000.00"),
                serviceDate,
                policy,
                coverage,
                false,
                null,
                null
        );

        assertThat(result.covered()).isTrue();
        assertThat(result.eligibleAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.nonCoveredAmount()).isEqualByComparingTo("0.00");
        assertThat(result.deductibleApplied()).isEqualByComparingTo("1000.00");
        assertThat(result.coPaymentAmount()).isEqualByComparingTo("900.00");
        assertThat(result.approvedAmount()).isEqualByComparingTo("8100.00");
        assertThat(result.patientResponsibility()).isEqualByComparingTo("1900.00");
    }

    @Test
    void appliesPreAuthorizationLimit() {
        AdjudicationCalculation result = rulesEngine.calculate(
                new BigDecimal("10000.00"),
                serviceDate,
                policy,
                coverage,
                false,
                null,
                new BigDecimal("5000.00")
        );

        assertThat(result.covered()).isTrue();
        assertThat(result.eligibleAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.nonCoveredAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.deductibleApplied()).isEqualByComparingTo("1000.00");
        assertThat(result.coPaymentAmount()).isEqualByComparingTo("400.00");
        assertThat(result.approvedAmount()).isEqualByComparingTo("3600.00");
        assertThat(result.patientResponsibility()).isEqualByComparingTo("6400.00");
    }

    @Test
    void appliesCategoryCoverageLimit() {
        coverage.setCoverageLimit(new BigDecimal("3000.00"));

        AdjudicationCalculation result = rulesEngine.calculate(
                new BigDecimal("10000.00"),
                serviceDate,
                policy,
                coverage,
                false,
                null,
                null
        );

        assertThat(result.eligibleAmount()).isEqualByComparingTo("3000.00");
        assertThat(result.nonCoveredAmount()).isEqualByComparingTo("7000.00");
        assertThat(result.deductibleApplied()).isEqualByComparingTo("1000.00");
        assertThat(result.coPaymentAmount()).isEqualByComparingTo("200.00");
        assertThat(result.approvedAmount()).isEqualByComparingTo("1800.00");
        assertThat(result.patientResponsibility()).isEqualByComparingTo("8200.00");
    }

    @Test
    void rejectsExcludedTreatment() {
        AdjudicationCalculation result = rulesEngine.calculate(
                new BigDecimal("10000.00"),
                serviceDate,
                policy,
                coverage,
                true,
                "Dental treatment is not covered",
                null
        );

        assertThat(result.covered()).isFalse();
        assertThat(result.approvedAmount()).isEqualByComparingTo("0.00");
        assertThat(result.patientResponsibility()).isEqualByComparingTo("10000.00");
        assertThat(result.remarks()).contains("Dental treatment is not covered");
    }

    @Test
    void rejectsServiceBeforeWaitingPeriod() {
        AdjudicationCalculation result = rulesEngine.calculate(
                new BigDecimal("10000.00"),
                policy.getStartDate().plusDays(10),
                policy,
                coverage,
                false,
                null,
                null
        );

        assertThat(result.covered()).isFalse();
        assertThat(result.approvedAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsExhaustedPolicyCoverage() {
        policy.setUtilizedAmount(new BigDecimal("100000.00"));

        AdjudicationCalculation result = rulesEngine.calculate(
                new BigDecimal("10000.00"),
                serviceDate,
                policy,
                coverage,
                false,
                null,
                null
        );

        assertThat(result.covered()).isFalse();
        assertThat(result.approvedAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsMissingCoverage() {
        AdjudicationCalculation result = rulesEngine.calculate(
                new BigDecimal("10000.00"),
                serviceDate,
                policy,
                null,
                false,
                null,
                null
        );

        assertThat(result.covered()).isFalse();
        assertThat(result.remarks()).anyMatch(remark -> remark.contains("No active coverage"));
    }
}
