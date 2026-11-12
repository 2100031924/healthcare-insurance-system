package com.healthcare.claimsystem.repository;

import com.healthcare.claimsystem.entity.PolicyExclusion;
import com.healthcare.claimsystem.entity.enums.TreatmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyExclusionRepository extends JpaRepository<PolicyExclusion, Long> {

    List<PolicyExclusion> findByPolicyIdOrderByTreatmentCategoryAsc(Long policyId);

    boolean existsByPolicyIdAndTreatmentCategory(Long policyId, TreatmentCategory treatmentCategory);

    Optional<PolicyExclusion> findByPolicyIdAndTreatmentCategory(Long policyId, TreatmentCategory treatmentCategory);

    Optional<PolicyExclusion> findByIdAndPolicyId(Long id, Long policyId);
}
