package com.restosuite.repo;

import com.restosuite.domain.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long>, JpaSpecificationExecutor<UnitOfMeasure> {

    java.util.List<UnitOfMeasure> findByRestaurantIdOrderByNameAsc(Long restaurantId);


}
