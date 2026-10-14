package com.restosuite.repo;

import com.restosuite.domain.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ModifierRepository extends JpaRepository<Modifier, Long>, JpaSpecificationExecutor<Modifier> {

    java.util.List<Modifier> findByRestaurantIdOrderByNameAsc(Long restaurantId);


}
