package com.healthcare.claimsystem.integration;

import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.entity.enums.Role;
import com.healthcare.claimsystem.repository.AuditLogRepository;
import com.healthcare.claimsystem.repository.ClaimRepository;
import com.healthcare.claimsystem.repository.ClaimSettlementRepository;
import com.healthcare.claimsystem.repository.HospitalRepository;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.repository.PolicyCoverageRepository;
import com.healthcare.claimsystem.repository.PolicyExclusionRepository;
import com.healthcare.claimsystem.repository.PreAuthorizationRepository;
import com.healthcare.claimsystem.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "Password@123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected HospitalRepository hospitalRepository;

    @Autowired
    protected PatientRepository patientRepository;

    @Autowired
    protected InsurancePolicyRepository policyRepository;

    @Autowired
    protected PolicyCoverageRepository coverageRepository;

    @Autowired
    protected PolicyExclusionRepository exclusionRepository;

    @Autowired
    protected PreAuthorizationRepository preAuthorizationRepository;

    @Autowired
    protected ClaimRepository claimRepository;

    @Autowired
    protected ClaimSettlementRepository settlementRepository;

    @Autowired
    protected AuditLogRepository auditLogRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        settlementRepository.deleteAll();
        auditLogRepository.deleteAll();
        claimRepository.deleteAll();
        preAuthorizationRepository.deleteAll();
        coverageRepository.deleteAll();
        exclusionRepository.deleteAll();
        policyRepository.deleteAll();
        patientRepository.deleteAll();
        hospitalRepository.deleteAll();
        List<User> removableUsers = userRepository.findAll().stream()
                .filter(user -> !"admin".equals(user.getUsername()))
                .toList();
        userRepository.deleteAll(removableUsers);
    }

    protected User createUser(String username, Role role) {
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username).orElseThrow();
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@healthcare.test");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setFullName(username.replace('.', ' ').toUpperCase());
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    protected String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static long idOf(String responseBody) {
        Number id = JsonPath.read(responseBody, "$.id");
        return id.longValue();
    }
}
