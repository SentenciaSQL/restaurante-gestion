package com.restosuite.repo;

import com.restosuite.domain.RecipeLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeLineRepository extends JpaRepository<RecipeLine, Long>, JpaSpecificationExecutor<RecipeLine> {

    java.util.List<RecipeLine> findByRecipeId(Long recipeId);
    void deleteByRecipeId(Long recipeId);


}
