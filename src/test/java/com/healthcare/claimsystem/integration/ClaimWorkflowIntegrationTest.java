package com.healthcare.claimsystem.integration;

import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PolicyExclusion;
import com.healthcare.claimsystem.entity.enums.Gender;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import com.healthcare.claimsystem.entity.enums.Role;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClaimWorkflowIntegrationTest extends IntegrationTestSupport {

    private Hospital hospital;
    private Patient patient;
    private InsurancePolicy policy;
    private PolicyCoverage hospitalizationCoverage;

    @BeforeEach
    void seedReferenceData() {
        createUser("hospital.workflow", Role.HOSPITAL);
        createUser("officer.workflow", Role.INSURANCE_OFFICER);
        createUser("finance.workflow", Role.FINANCE_OFFICER);

        hospital = new Hospital();
        hospital.setHospitalCode("HSP-INT-1");
        hospital.setName("Integration Test Hospital");
        hospital.setLicenseNumber("LIC-INT-1");
        hospital.setEmail("hospital.integration@healthcare.test");
        hospital.setPhone("+91-9876543210");
        hospital.setAddress("1 Integration Avenue");
        hospital.setContactPerson("Integration Contact");
        hospital.setActive(true);
        hospital = hospitalRepository.save(hospital);

        patient = new Patient();
        patient.setPatientCode("PAT-INT-1");
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1985, 3, 20));
        patient.setGender(Gender.FEMALE);
        patient.setEmail("jane.doe@healthcare.test");
        patient.setPhone("+91-9876501234");
        patient.setAddress("2 Integration Avenue");
        patient.setActive(true);
        patient = patientRepository.save(patient);

        policy = new InsurancePolicy();
        policy.setPolicyNumber("POL-INT-1");
        policy.setPatient(patient);
        policy.setProviderName("SecureLife Insurance");
        policy.setPolicyType(PolicyType.INDIVIDUAL);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(LocalDate.now().minusDays(100));
        policy.setEndDate(LocalDate.now().plusDays(200));
        policy.setSumInsured(new BigDecimal("500000.00"));
        policy.setUtilizedAmount(new BigDecimal("0.00"));
        policy.setDeductibleAmount(new BigDecimal("5000.00"));
        policy.setCoPaymentPercentage(new BigDecimal("10.00"));
        policy.setWaitingPeriodDays(30);
        policy.setPremiumAmount(new BigDecimal("25000.00"));
        policy = policyRepository.save(policy);

        hospitalizationCoverage = new PolicyCoverage();
        hospitalizationCoverage.setPolicy(policy);
        hospitalizationCoverage.setCoverageType(TreatmentCategory.HOSPITALIZATION);
        hospitalizationCoverage.setCoverageLimit(new BigDecimal("300000.00"));
        hospitalizationCoverage.setUtilizedAmount(new BigDecimal("0.00"));
        hospitalizationCoverage.setActive(true);
        hospitalizationCoverage = coverageRepository.save(hospitalizationCoverage);

        PolicyCoverage dentalCoverage = new PolicyCoverage();
        dentalCoverage.setPolicy(policy);
        dentalCoverage.setCoverageType(TreatmentCategory.DENTAL);
        dentalCoverage.setCoverageLimit(new BigDecimal("10000.00"));
        dentalCoverage.setUtilizedAmount(new BigDecimal("0.00"));
        dentalCoverage.setActive(true);
        coverageRepository.save(dentalCoverage);

        PolicyExclusion dentalExclusion = new PolicyExclusion();
        dentalExclusion.setPolicy(policy);
        dentalExclusion.setTreatmentCategory(TreatmentCategory.DENTAL);
        dentalExclusion.setReason("Dental treatment is not covered under this policy");
        exclusionRepository.save(dentalExclusion);
    }

    @Test
    void processesClaimThroughFullLifecycle() throws Exception {
        String hospitalToken = login("hospital.workflow", PASSWORD);
        String officerToken = login("officer.workflow", PASSWORD);
        String financeToken = login("finance.workflow", PASSWORD);
        String adminToken = login("admin", "Admin@12345");

        mockMvc.perform(post("/api/v1/eligibility/verify")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "policyNumber":"POL-INT-1",
                                  "treatmentCategory":"HOSPITALIZATION",
                                  "estimatedCost":100000.00,
                                  "serviceDate":"%s"
                                }
                                """.formatted(patient.getId(), LocalDate.now())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.estimatedInsurerPayable").value(85500.00))
                .andExpect(jsonPath("$.estimatedPatientResponsibility").value(14500.00));

        String preAuthorizationBody = mockMvc.perform(post("/api/v1/pre-authorizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "policyId":%d,
                                  "hospitalId":%d,
                                  "treatingDoctor":"Dr. Alice Smith",
                                  "diagnosisCode":"J18.9",
                                  "procedureCode":"PROC-HOSP-01",
                                  "treatmentCategory":"HOSPITALIZATION",
                                  "treatmentDescription":"Inpatient hospitalization for pneumonia",
                                  "estimatedCost":100000.00,
                                  "requestedDate":"%s"
                                }
                                """.formatted(patient.getId(), policy.getId(), hospital.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long preAuthorizationId = idOf(preAuthorizationBody);

        mockMvc.perform(post("/api/v1/pre-authorizations/{id}/review", preAuthorizationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(post("/api/v1/pre-authorizations/{id}/decision", preAuthorizationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision":"APPROVE",
                                  "approvedAmount":100000.00,
                                  "remarks":"Approved for inpatient care",
                                  "validUntil":"%s"
                                }
                                """.formatted(LocalDate.now().plusDays(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedAmount").value(100000.00));

        String claimPayload = """
                {
                  "patientId":%d,
                  "policyId":%d,
                  "hospitalId":%d,
                  "preAuthorizationId":%d,
                  "treatingDoctor":"Dr. Alice Smith",
                  "diagnosisCode":"J18.9",
                  "procedureCode":"PROC-HOSP-01",
                  "treatmentCategory":"HOSPITALIZATION",
                  "admissionDate":"%s",
                  "dischargeDate":"%s",
                  "claimDate":"%s",
                  "claimedAmount":100000.00,
                  "invoiceNumber":"INV-INT-001",
                  "description":"Inpatient treatment"
                }
                """.formatted(
                patient.getId(),
                policy.getId(),
                hospital.getId(),
                preAuthorizationId,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(8),
                LocalDate.now()
        );

        String claimBody = mockMvc.perform(post("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.preAuthorizationId").value(preAuthorizationId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long claimId = idOf(claimBody);

        mockMvc.perform(post("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimPayload.replace("INV-INT-001", "INV-INT-002")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Duplicate claim")
                ));

        mockMvc.perform(post("/api/v1/claims/{id}/review", claimId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(get("/api/v1/claims/{id}/adjudication-preview", claimId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.covered").value(true))
                .andExpect(jsonPath("$.eligibleAmount").value(100000.00))
                .andExpect(jsonPath("$.deductibleApplied").value(5000.00))
                .andExpect(jsonPath("$.coPaymentAmount").value(9500.00))
                .andExpect(jsonPath("$.approvedAmount").value(85500.00))
                .andExpect(jsonPath("$.patientResponsibility").value(14500.00));

        mockMvc.perform(post("/api/v1/claims/{id}/adjudicate", claimId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision":"APPROVE",
                                  "notes":"Claim approved after verification"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedAmount").value(85500.00))
                .andExpect(jsonPath("$.patientResponsibility").value(14500.00));

        mockMvc.perform(post("/api/v1/settlements")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claimId":%d,
                                  "paymentMode":"BANK_TRANSFER",
                                  "paymentReference":"PAY-INT-001",
                                  "remarks":"Settlement processed"
                                }
                                """.formatted(claimId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.paidAmount").value(85500.00))
                .andExpect(jsonPath("$.patientResponsibility").value(14500.00));

        mockMvc.perform(get("/api/v1/claims/{id}", claimId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        mockMvc.perform(get("/api/v1/policies/{id}", policy.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizedAmount").value(85500.00))
                .andExpect(jsonPath("$.remainingAmount").value(414500.00));

        String coveragesBody = mockMvc.perform(get("/api/v1/policies/{id}/coverages", policy.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<?> utilizedValues = JsonPath.read(
                coveragesBody,
                "$[?(@.coverageType == 'HOSPITALIZATION')].utilizedAmount"
        );
        assertThat(((Number) utilizedValues.get(0)).doubleValue()).isEqualTo(85500.00);

        String auditBody = mockMvc.perform(get("/api/v1/claims/{id}/audit", claimId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> actions = JsonPath.read(auditBody, "$.content[*].action");
        assertThat(actions)
                .contains("CLAIM_SUBMITTED", "CLAIM_REVIEWED", "CLAIM_ADJUDICATED", "CLAIM_SETTLED");

        mockMvc.perform(post("/api/v1/settlements")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claimId":%d,
                                  "paymentMode":"UPI",
                                  "paymentReference":"PAY-INT-002"
                                }
                                """.formatted(claimId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("already been settled")
                ));
    }

    @Test
    void rejectsPreAuthorizationForExcludedTreatment() throws Exception {
        String hospitalToken = login("hospital.workflow", PASSWORD);

        mockMvc.perform(post("/api/v1/pre-authorizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "policyId":%d,
                                  "hospitalId":%d,
                                  "treatingDoctor":"Dr. Alice Smith",
                                  "diagnosisCode":"K02.9",
                                  "procedureCode":"PROC-DENT-01",
                                  "treatmentCategory":"DENTAL",
                                  "treatmentDescription":"Dental restoration",
                                  "estimatedCost":8000.00,
                                  "requestedDate":"%s"
                                }
                                """.formatted(patient.getId(), policy.getId(), hospital.getId(), LocalDate.now())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("excluded")
                ));
    }

    @Test
    void rejectsClaimWhenPreAuthorizationIsNotApproved() throws Exception {
        String hospitalToken = login("hospital.workflow", PASSWORD);

        String preAuthorizationBody = mockMvc.perform(post("/api/v1/pre-authorizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "policyId":%d,
                                  "hospitalId":%d,
                                  "treatingDoctor":"Dr. Alice Smith",
                                  "diagnosisCode":"J18.9",
                                  "procedureCode":"PROC-HOSP-02",
                                  "treatmentCategory":"HOSPITALIZATION",
                                  "treatmentDescription":"Inpatient treatment",
                                  "estimatedCost":50000.00,
                                  "requestedDate":"%s"
                                }
                                """.formatted(patient.getId(), policy.getId(), hospital.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long preAuthorizationId = idOf(preAuthorizationBody);

        mockMvc.perform(post("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "policyId":%d,
                                  "hospitalId":%d,
                                  "preAuthorizationId":%d,
                                  "treatingDoctor":"Dr. Alice Smith",
                                  "diagnosisCode":"J18.9",
                                  "procedureCode":"PROC-HOSP-02",
                                  "treatmentCategory":"HOSPITALIZATION",
                                  "admissionDate":"%s",
                                  "dischargeDate":"%s",
                                  "claimDate":"%s",
                                  "claimedAmount":50000.00,
                                  "invoiceNumber":"INV-INT-010"
                                }
                                """.formatted(
                                patient.getId(),
                                policy.getId(),
                                hospital.getId(),
                                preAuthorizationId,
                                LocalDate.now().minusDays(5),
                                LocalDate.now().minusDays(3),
                                LocalDate.now()
                        )))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("not approved")
                ));
    }

    @Test
    void rejectsDuplicateInvoiceNumber() throws Exception {
        String hospitalToken = login("hospital.workflow", PASSWORD);
        String payload = """
                {
                  "patientId":%d,
                  "policyId":%d,
                  "hospitalId":%d,
                  "treatingDoctor":"Dr. Alice Smith",
                  "diagnosisCode":"J18.9",
                  "procedureCode":"PROC-HOSP-03",
                  "treatmentCategory":"HOSPITALIZATION",
                  "admissionDate":"%s",
                  "dischargeDate":"%s",
                  "claimDate":"%s",
                  "claimedAmount":40000.00,
                  "invoiceNumber":"INV-INT-020"
                }
                """.formatted(
                patient.getId(),
                policy.getId(),
                hospital.getId(),
                LocalDate.now().minusDays(4),
                LocalDate.now().minusDays(2),
                LocalDate.now()
        );

        mockMvc.perform(post("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.replace("40000.00", "41000.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("invoice number")
                ));
    }

    @Test
    void forbidsClaimSubmissionByFinanceOfficer() throws Exception {
        String financeToken = login("finance.workflow", PASSWORD);

        mockMvc.perform(post("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "policyId":%d,
                                  "hospitalId":%d,
                                  "treatingDoctor":"Dr. Alice Smith",
                                  "diagnosisCode":"J18.9",
                                  "procedureCode":"PROC-HOSP-04",
                                  "treatmentCategory":"HOSPITALIZATION",
                                  "admissionDate":"%s",
                                  "dischargeDate":"%s",
                                  "claimDate":"%s",
                                  "claimedAmount":20000.00,
                                  "invoiceNumber":"INV-INT-030"
                                }
                                """.formatted(
                                patient.getId(),
                                policy.getId(),
                                hospital.getId(),
                                LocalDate.now().minusDays(3),
                                LocalDate.now().minusDays(2),
                                LocalDate.now()
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void returnsValidationDetailsForInvalidClaim() throws Exception {
        String hospitalToken = login("hospital.workflow", PASSWORD);

        mockMvc.perform(post("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }
}
