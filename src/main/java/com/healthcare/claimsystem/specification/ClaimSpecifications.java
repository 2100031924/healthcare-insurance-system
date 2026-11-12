package com.healthcare.claimsystem.specification;

import com.healthcare.claimsystem.entity.Claim;
import com.healthcare.claimsystem.entity.enums.ClaimStatus;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ClaimSpecifications {

    private ClaimSpecifications() {
    }

    public static Specification<Claim> withFilters(
            ClaimStatus status,
            Long patientId,
            Long hospitalId,
            String policyNumber,
            String claimNumber,
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
            if (claimNumber != null && !claimNumber.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("claimNumber")),
                        "%" + claimNumber.trim().toLowerCase() + "%"
                ));
            }
            if (treatmentCategory != null) {
                predicates.add(criteriaBuilder.equal(root.get("treatmentCategory"), treatmentCategory));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("claimDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("claimDate"), toDate));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
