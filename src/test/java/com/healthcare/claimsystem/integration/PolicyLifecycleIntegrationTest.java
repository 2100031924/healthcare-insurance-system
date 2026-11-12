package com.healthcare.claimsystem.integration;

import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.Gender;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import com.healthcare.claimsystem.entity.enums.Role;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyLifecycleIntegrationTest extends IntegrationTestSupport {

    @Test
    void managesHospitalPatientPolicyCoverageAndRenewal() throws Exception {
        String adminToken = login("admin", "Admin@12345");
        createUser("hospital.lifecycle", Role.HOSPITAL);
        createUser("officer.lifecycle", Role.INSURANCE_OFFICER);
        String hospitalToken = login("hospital.lifecycle", PASSWORD);
        String officerToken = login("officer.lifecycle", PASSWORD);

        String hospitalBody = mockMvc.perform(post("/api/v1/hospitals")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hospitalCode":"HSP-LIFE-1",
                                  "name":"Lifecycle Hospital",
                                  "licenseNumber":"LIC-LIFE-1",
                                  "email":"lifecycle.hospital@healthcare.test",
                                  "phone":"+91-9876543210",
                                  "address":"10 Lifecycle Road",
                                  "contactPerson":"Lifecycle Contact",
                                  "active":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospitalCode").value("HSP-LIFE-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long hospitalId = idOf(hospitalBody);

        String patientBody = mockMvc.perform(post("/api/v1/patients")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Lifecycle",
                                  "lastName":"Patient",
                                  "dateOfBirth":"1990-05-10",
                                  "gender":"MALE",
                                  "email":"lifecycle.patient@healthcare.test",
                                  "phone":"+91-9876501234",
                                  "address":"11 Lifecycle Road",
                                  "active":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientCode").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long patientId = idOf(patientBody);

        String policyBody = mockMvc.perform(post("/api/v1/policies")
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "providerName":"Lifecycle Insurance",
                                  "policyType":"INDIVIDUAL",
                                  "startDate":"%s",
                                  "endDate":"%s",
                                  "sumInsured":200000.00,
                                  "deductibleAmount":2000.00,
                                  "coPaymentPercentage":5.00,
                                  "waitingPeriodDays":0,
                                  "premiumAmount":12000.00
                                }
                                """.formatted(patientId, LocalDate.now(), LocalDate.now().plusDays(365))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.policyNumber").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long policyId = idOf(policyBody);
        String policyNumber = JsonPath.read(policyBody, "$.policyNumber");

        mockMvc.perform(post("/api/v1/policies/{id}/coverages", policyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageType":"HOSPITALIZATION",
                                  "coverageLimit":150000.00,
                                  "active":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverageType").value("HOSPITALIZATION"))
                .andExpect(jsonPath("$.remainingAmount").value(150000.00));

        mockMvc.perform(post("/api/v1/policies/{id}/coverages", policyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageType":"HOSPITALIZATION",
                                  "coverageLimit":150000.00
                                }
                                """))
                .andExpect(status().isConflict());

        String exclusionBody = mockMvc.perform(post("/api/v1/policies/{id}/exclusions", policyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "treatmentCategory":"VISION",
                                  "reason":"Vision correction is excluded"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long exclusionId = idOf(exclusionBody);

        mockMvc.perform(post("/api/v1/eligibility/verify")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "policyNumber":"%s",
                                  "treatmentCategory":"HOSPITALIZATION",
                                  "estimatedCost":20000.00,
                                  "serviceDate":"%s"
                                }
                                """.formatted(patientId, policyNumber, LocalDate.now())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.estimatedInsurerPayable").value(17100.00))
                .andExpect(jsonPath("$.estimatedPatientResponsibility").value(2900.00));

        mockMvc.perform(get("/api/v1/policies")
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .param("status", "ACTIVE")
                        .param("patientId", String.valueOf(patientId))
                        .param("sort", "createdAt,desc")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].policyNumber").value(policyNumber));

        mockMvc.perform(post("/api/v1/policies/{id}/renew", policyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newEndDate":"%s",
                                  "additionalSumInsured":50000.00,
                                  "premiumAmount":15000.00
                                }
                                """.formatted(LocalDate.now().plusDays(730))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.sumInsured").value(250000.00))
                .andExpect(jsonPath("$.premiumAmount").value(15000.00))
                .andExpect(jsonPath("$.renewalCount").value(1))
                .andExpect(jsonPath("$.lastRenewedAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/policies/{id}/status", policyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUSPENDED","reason":"Investigation in progress"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.cancellationReason").value("Investigation in progress"));

        mockMvc.perform(patch("/api/v1/policies/{id}/status", policyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/v1/policies/{id}/exclusions/{exclusionId}", policyId, exclusionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/policies/{id}/exclusions", policyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/hospitals/{id}", hospitalId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalCode").value("HSP-LIFE-1"));
    }

    @Test
    void rejectsPolicyWithEndDateBeforeStartDate() throws Exception {
        createUser("officer.validation", Role.INSURANCE_OFFICER);
        String officerToken = login("officer.validation", PASSWORD);
        createUser("hospital.validation", Role.HOSPITAL);
        String hospitalToken = login("hospital.validation", PASSWORD);

        String patientBody = mockMvc.perform(post("/api/v1/patients")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Invalid",
                                  "lastName":"Policy",
                                  "dateOfBirth":"1990-01-01",
                                  "gender":"FEMALE",
                                  "phone":"9876501234"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long patientId = idOf(patientBody);

        mockMvc.perform(post("/api/v1/policies")
                        .header(HttpHeaders.AUTHORIZATION, bearer(officerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":%d,
                                  "providerName":"Invalid Insurance",
                                  "policyType":"INDIVIDUAL",
                                  "startDate":"%s",
                                  "endDate":"%s",
                                  "sumInsured":100000.00,
                                  "deductibleAmount":0.00,
                                  "coPaymentPercentage":0.00,
                                  "waitingPeriodDays":0,
                                  "premiumAmount":5000.00
                                }
                                """.formatted(patientId, LocalDate.now(), LocalDate.now().minusDays(1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Policy end date must not be before the start date"));
    }

    @Test
    void forbidsHospitalFromCreatingPolicy() throws Exception {
        createUser("hospital.rbac", Role.HOSPITAL);
        String hospitalToken = login("hospital.rbac", PASSWORD);

        mockMvc.perform(post("/api/v1/policies")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":1,
                                  "providerName":"Forbidden Insurance",
                                  "policyType":"INDIVIDUAL",
                                  "startDate":"%s",
                                  "endDate":"%s",
                                  "sumInsured":100000.00,
                                  "deductibleAmount":0.00,
                                  "coPaymentPercentage":0.00,
                                  "waitingPeriodDays":0,
                                  "premiumAmount":5000.00
                                }
                                """.formatted(LocalDate.now(), LocalDate.now().plusDays(365))))
                .andExpect(status().isForbidden());
    }

    @Test
    void paginatesAndFiltersClaims() throws Exception {
        String adminToken = login("admin", "Admin@12345");

        com.healthcare.claimsystem.entity.Hospital hospital = new Hospital();
        hospital.setHospitalCode("HSP-FILTER-1");
        hospital.setName("Filter Hospital");
        hospital.setLicenseNumber("LIC-FILTER-1");
        hospital.setEmail("filter.hospital@healthcare.test");
        hospital.setPhone("+91-9876543210");
        hospital.setAddress("20 Filter Road");
        hospital.setContactPerson("Filter Contact");
        hospital.setActive(true);
        hospital = hospitalRepository.save(hospital);

        com.healthcare.claimsystem.entity.Patient patient = new Patient();
        patient.setPatientCode("PAT-FILTER-1");
        patient.setFirstName("Filter");
        patient.setLastName("Patient");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender(Gender.MALE);
        patient.setPhone("+91-9876501234");
        patient.setActive(true);
        patient = patientRepository.save(patient);

        com.healthcare.claimsystem.entity.InsurancePolicy policy = new InsurancePolicy();
        policy.setPolicyNumber("POL-FILTER-1");
        policy.setPatient(patient);
        policy.setProviderName("Filter Insurance");
        policy.setPolicyType(PolicyType.INDIVIDUAL);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(LocalDate.now().minusDays(100));
        policy.setEndDate(LocalDate.now().plusDays(200));
        policy.setSumInsured(new BigDecimal("300000.00"));
        policy.setUtilizedAmount(new BigDecimal("0.00"));
        policy.setDeductibleAmount(new BigDecimal("0.00"));
        policy.setCoPaymentPercentage(new BigDecimal("0.00"));
        policy.setWaitingPeriodDays(0);
        policy.setPremiumAmount(new BigDecimal("10000.00"));
        policy = policyRepository.save(policy);

        seedClaim(hospital, patient, policy, "CLM-FILTER-1", "SUB-1", "INV-FILTER-1",
                ClaimStatus.SUBMITTED, LocalDate.now().minusDays(2));
        seedClaim(hospital, patient, policy, "CLM-FILTER-2", "SUB-2", "INV-FILTER-2",
                ClaimStatus.SUBMITTED, LocalDate.now().minusDays(1));
        seedClaim(hospital, patient, policy, "CLM-FILTER-3", "SUB-3", "INV-FILTER-3",
                ClaimStatus.APPROVED, LocalDate.now());

        mockMvc.perform(get("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("status", "SUBMITTED")
                        .param("patientId", String.valueOf(patient.getId()))
                        .param("sort", "claimDate,asc")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].claimNumber").value("CLM-FILTER-1"))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("status", "SUBMITTED")
                        .param("sort", "claimDate,asc")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].claimNumber").value("CLM-FILTER-2"))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/v1/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("claimNumber", "CLM-FILTER-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    private void seedClaim(
            Hospital hospital,
            Patient patient,
            InsurancePolicy policy,
            String claimNumber,
            String fingerprint,
            String invoiceNumber,
            ClaimStatus status,
            LocalDate claimDate
    ) {
        Claim claim = new Claim();
        claim.setClaimNumber(claimNumber);
        claim.setClaimFingerprint(fingerprint);
        claim.setPatient(patient);
        claim.setPolicy(policy);
        claim.setHospital(hospital);
        claim.setTreatingDoctor("Dr. Filter");
        claim.setDiagnosisCode("J18.9");
        claim.setProcedureCode("PROC-FILTER");
        claim.setTreatmentCategory(TreatmentCategory.HOSPITALIZATION);
        claim.setAdmissionDate(claimDate.minusDays(2));
        claim.setDischargeDate(claimDate.minusDays(1));
        claim.setClaimDate(claimDate);
        claim.setClaimedAmount(new BigDecimal("10000.00"));
        claim.setInvoiceNumber(invoiceNumber);
        claim.setStatus(status);
        claim.setDeductibleApplied(new BigDecimal("0.00"));
        claim.setCoPaymentAmount(new BigDecimal("0.00"));
        claim.setNonCoveredAmount(new BigDecimal("0.00"));
        claim.setApprovedAmount(new BigDecimal("0.00"));
        claim.setPatientResponsibility(new BigDecimal("0.00"));
        claimRepository.save(claim);
    }
}
