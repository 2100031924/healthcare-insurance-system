package com.healthcare.claimsystem.specification;

import com.healthcare.claimsystem.entity.AuditLog;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> withFilters(
            Long claimId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (claimId != null) {
                predicates.add(criteriaBuilder.equal(root.get("claim").get("id"), claimId));
            }
            if (entityType != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), entityType));
            }
            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        fromDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                ));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("createdAt"),
                        toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
