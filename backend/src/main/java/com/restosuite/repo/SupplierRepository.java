package com.restosuite.repo;

import com.restosuite.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    java.util.List<Supplier> findByRestaurantIdOrderByNameAsc(Long restaurantId);
    java.util.Optional<Supplier> findByIdAndRestaurantId(Long id, Long restaurantId);


}
