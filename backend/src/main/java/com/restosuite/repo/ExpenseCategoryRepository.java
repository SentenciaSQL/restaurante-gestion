package com.restosuite.repo;

import com.restosuite.domain.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long>, JpaSpecificationExecutor<ExpenseCategory> {

    java.util.List<ExpenseCategory> findByRestaurantIdOrderByNameAsc(Long restaurantId);


}
