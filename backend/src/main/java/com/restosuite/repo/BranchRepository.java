package com.restosuite.repo;

import com.restosuite.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BranchRepository extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {

    java.util.List<Branch> findByRestaurantIdOrderByNameAsc(Long restaurantId);
    java.util.Optional<Branch> findByIdAndRestaurantId(Long id, Long restaurantId);
    java.util.Optional<Branch> findByRestaurantIdAndCode(Long restaurantId, String code);


}
