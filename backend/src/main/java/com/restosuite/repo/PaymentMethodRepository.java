package com.restosuite.repo;

import com.restosuite.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long>, JpaSpecificationExecutor<PaymentMethod> {

    java.util.List<PaymentMethod> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);
    java.util.Optional<PaymentMethod> findByIdAndRestaurantId(Long id, Long restaurantId);


}
