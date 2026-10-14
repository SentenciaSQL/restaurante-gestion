package com.restosuite.repo;

import com.restosuite.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IngredientRepository extends JpaRepository<Ingredient, Long>, JpaSpecificationExecutor<Ingredient> {

    java.util.List<Ingredient> findByRestaurantIdOrderByNameAsc(Long restaurantId);
    java.util.Optional<Ingredient> findByIdAndRestaurantId(Long id, Long restaurantId);
    java.util.Optional<Ingredient> findByRestaurantIdAndSkuIgnoreCase(Long restaurantId, String sku);


}
