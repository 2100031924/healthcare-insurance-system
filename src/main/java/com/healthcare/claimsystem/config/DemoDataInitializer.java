package com.healthcare.claimsystem.config;

import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.PolicyCoverage;
import com.healthcare.claimsystem.entity.PolicyExclusion;
import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.entity.enums.Gender;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import com.healthcare.claimsystem.entity.enums.Role;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import com.healthcare.claimsystem.repository.HospitalRepository;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Order(2)
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
@Slf4j
public class DemoDataInitializer implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "Demo@12345";

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PatientRepository patientRepository;
    private final InsurancePolicyRepository policyRepository;
    private final PolicyCoverageRepository coverageRepository;
    private final PolicyExclusionRepository exclusionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createUserIfMissing("hospital1", "hospital1@healthcare.com", "City General Hospital", Role.HOSPITAL);
        createUserIfMissing("officer1", "officer1@healthcare.com", "Claims Officer", Role.INSURANCE_OFFICER);
        createUserIfMissing("finance1", "finance1@healthcare.com", "Finance Officer", Role.FINANCE_OFFICER);

        Hospital hospital = hospitalRepository.findByHospitalCode("HSP-001").orElseGet(() -> {
            Hospital created = new Hospital();
            created.setHospitalCode("HSP-001");
            created.setName("City General Hospital");
            created.setLicenseNumber("LIC-HSP-001");
            created.setEmail("contact@citygeneral.example");
            created.setPhone("+91-9876543210");
            created.setAddress("12 Health Avenue, Bengaluru");
            created.setContactPerson("Ravi Kumar");
            created.setActive(true);
            return hospitalRepository.save(created);
        });

        Patient patient = patientRepository.findByPatientCode("PAT-0001").orElseGet(() -> {
            Patient created = new Patient();
            created.setPatientCode("PAT-0001");
            created.setFirstName("John");
            created.setLastName("Doe");
            created.setDateOfBirth(LocalDate.of(1985, 6, 15));
            created.setGender(Gender.MALE);
            created.setEmail("john.doe@example.com");
            created.setPhone("+91-9876501234");
            created.setAddress("45 Lake Road, Bengaluru");
            created.setActive(true);
            return patientRepository.save(created);
        });

        InsurancePolicy policy = policyRepository.findByPolicyNumber("POL-2026-000001").orElseGet(() -> {
            InsurancePolicy created = new InsurancePolicy();
            created.setPolicyNumber("POL-2026-000001");
            created.setPatient(patient);
            created.setProviderName("SecureLife Insurance");
            created.setPolicyType(PolicyType.INDIVIDUAL);
            created.setStatus(PolicyStatus.ACTIVE);
            created.setStartDate(LocalDate.now().minusDays(90));
            created.setEndDate(LocalDate.now().plusDays(275));
            created.setSumInsured(new BigDecimal("500000.00"));
            created.setUtilizedAmount(BigDecimal.ZERO.setScale(2));
            created.setDeductibleAmount(new BigDecimal("5000.00"));
            created.setCoPaymentPercentage(new BigDecimal("10.00"));
            created.setWaitingPeriodDays(30);
            created.setPremiumAmount(new BigDecimal("25000.00"));
            return policyRepository.save(created);
        });

        createCoverageIfMissing(policy, TreatmentCategory.CONSULTATION, "50000.00");
        createCoverageIfMissing(policy, TreatmentCategory.HOSPITALIZATION, "300000.00");
        createCoverageIfMissing(policy, TreatmentCategory.SURGERY, "400000.00");
        createCoverageIfMissing(policy, TreatmentCategory.MEDICATION, "30000.00");
        createCoverageIfMissing(policy, TreatmentCategory.DIAGNOSTIC, "40000.00");
        createCoverageIfMissing(policy, TreatmentCategory.EMERGENCY, "200000.00");

        createExclusionIfMissing(policy, TreatmentCategory.DENTAL, "Dental treatment is not covered under this policy");
        createExclusionIfMissing(policy, TreatmentCategory.VISION, "Vision correction procedures are not covered");

        log.info("Demo data is available for hospital {} and patient {}", hospital.getHospitalCode(), patient.getPatientCode());
    }

    private void createUserIfMissing(String username, String email, String fullName, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setFullName(fullName);
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private void createCoverageIfMissing(
            InsurancePolicy policy,
            TreatmentCategory category,
            String limit
    ) {
        if (coverageRepository.existsByPolicyIdAndCoverageType(policy.getId(), category)) {
            return;
        }
        PolicyCoverage coverage = new PolicyCoverage();
        coverage.setPolicy(policy);
        coverage.setCoverageType(category);
        coverage.setCoverageLimit(new BigDecimal(limit));
        coverage.setUtilizedAmount(BigDecimal.ZERO.setScale(2));
        coverage.setActive(true);
        coverageRepository.save(coverage);
    }

    private void createExclusionIfMissing(
            InsurancePolicy policy,
            TreatmentCategory category,
            String reason
    ) {
        if (exclusionRepository.existsByPolicyIdAndTreatmentCategory(policy.getId(), category)) {
            return;
        }
        PolicyExclusion exclusion = new PolicyExclusion();
        exclusion.setPolicy(policy);
        exclusion.setTreatmentCategory(category);
        exclusion.setReason(reason);
        exclusionRepository.save(exclusion);
    }
}
