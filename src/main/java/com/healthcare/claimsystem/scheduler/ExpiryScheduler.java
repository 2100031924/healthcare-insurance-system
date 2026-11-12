package com.healthcare.claimsystem.scheduler;

import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import com.healthcare.claimsystem.repository.InsurancePolicyRepository;
import com.healthcare.claimsystem.repository.PreAuthorizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiryScheduler {

    private final InsurancePolicyRepository policyRepository;
    private final PreAuthorizationRepository preAuthorizationRepository;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireRecords() {
        LocalDate today = LocalDate.now();
        int expiredPolicies = policyRepository.expirePolicies(
                PolicyStatus.ACTIVE,
                PolicyStatus.EXPIRED,
                today
        );
        int expiredPreAuthorizations = preAuthorizationRepository.expirePreAuthorizations(
                List.of(PreAuthorizationStatus.APPROVED, PreAuthorizationStatus.PARTIALLY_APPROVED),
                PreAuthorizationStatus.EXPIRED,
                today
        );
        log.info(
                "Expiry job completed: {} policies expired, {} pre-authorizations expired",
                expiredPolicies,
                expiredPreAuthorizations
        );
    }
}
