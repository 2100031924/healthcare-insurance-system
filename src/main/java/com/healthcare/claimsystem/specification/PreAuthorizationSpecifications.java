package com.healthcare.claimsystem.specification;

import com.healthcare.claimsystem.entity.PreAuthorization;
import com.healthcare.claimsystem.entity.enums.PreAuthorizationStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class PreAuthorizationSpecifications {

    private PreAuthorizationSpecifications() {
    }

    public static Specification<PreAuthorization> withFilters(
            PreAuthorizationStatus status,
            Long patientId,
            Long hospitalId,
            String policyNumber,
            String referenceNumber,
            TreatmentCategory treatmentCategory,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (patientId != null) {
                predicates.add(criteriaBuilder.equal(root.get("patient").get("id"), patientId));
            }
            if (hospitalId != null) {
                predicates.add(criteriaBuilder.equal(root.get("hospital").get("id"), hospitalId));
            }
            if (policyNumber != null && !policyNumber.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("policy").get("policyNumber"),
                        policyNumber.trim()
                ));
            }
            if (referenceNumber != null && !referenceNumber.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("referenceNumber")),
                        "%" + referenceNumber.trim().toLowerCase() + "%"
                ));
            }
            if (treatmentCategory != null) {
                predicates.add(criteriaBuilder.equal(root.get("treatmentCategory"), treatmentCategory));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("requestedDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("requestedDate"), toDate));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
