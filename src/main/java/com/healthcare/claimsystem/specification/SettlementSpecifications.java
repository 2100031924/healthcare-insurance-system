package com.healthcare.claimsystem.specification;

import com.healthcare.claimsystem.entity.ClaimSettlement;
import com.healthcare.claimsystem.entity.enums.SettlementStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class SettlementSpecifications {

    private SettlementSpecifications() {
    }

    public static Specification<ClaimSettlement> withFilters(
            SettlementStatus status,
            Long claimId,
            String claimNumber,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (claimId != null) {
                predicates.add(criteriaBuilder.equal(root.get("claim").get("id"), claimId));
            }
            if (claimNumber != null && !claimNumber.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("claim").get("claimNumber")),
                        "%" + claimNumber.trim().toLowerCase() + "%"
                ));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("processedAt"),
                        fromDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                ));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("processedAt"),
                        toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
