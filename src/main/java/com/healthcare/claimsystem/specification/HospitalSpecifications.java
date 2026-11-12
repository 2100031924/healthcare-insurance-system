package com.healthcare.claimsystem.specification;

import com.healthcare.claimsystem.entity.Hospital;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class HospitalSpecifications {

    private HospitalSpecifications() {
    }

    public static Specification<Hospital> withFilters(Boolean active, String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("hospitalCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("licenseNumber")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
