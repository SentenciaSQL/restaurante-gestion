package com.restosuite.repo;

import com.restosuite.domain.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaxRateRepository extends JpaRepository<TaxRate, Long>, JpaSpecificationExecutor<TaxRate> {

    java.util.List<TaxRate> findByRestaurantIdOrderByNameAsc(Long restaurantId);


}
