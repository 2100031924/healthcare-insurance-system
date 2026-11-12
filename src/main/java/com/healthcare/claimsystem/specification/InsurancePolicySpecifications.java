package com.healthcare.claimsystem.specification;

import com.healthcare.claimsystem.entity.InsurancePolicy;
import com.healthcare.claimsystem.entity.enums.PolicyStatus;
import com.healthcare.claimsystem.entity.enums.PolicyType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class InsurancePolicySpecifications {

    private InsurancePolicySpecifications() {
    }

    public static Specification<InsurancePolicy> withFilters(
            Long patientId,
            PolicyStatus status,
            PolicyType policyType,
            String policyNumber,
            String providerName,
            LocalDate expiringBefore
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (patientId != null) {
                predicates.add(criteriaBuilder.equal(root.get("patient").get("id"), patientId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (policyType != null) {
                predicates.add(criteriaBuilder.equal(root.get("policyType"), policyType));
            }
            if (policyNumber != null && !policyNumber.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("policyNumber")),
                        "%" + policyNumber.trim().toLowerCase() + "%"
                ));
            }
            if (providerName != null && !providerName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("providerName")),
                        "%" + providerName.trim().toLowerCase() + "%"
                ));
            }
            if (expiringBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), expiringBefore));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
