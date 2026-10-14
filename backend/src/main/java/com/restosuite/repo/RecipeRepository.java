package com.restosuite.repo;

import com.restosuite.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    java.util.Optional<Recipe> findByProductIdAndVariantId(Long productId, Long variantId);
    java.util.Optional<Recipe> findByProductIdAndVariantIdIsNull(Long productId);
    java.util.List<Recipe> findByProductId(Long productId);


}
