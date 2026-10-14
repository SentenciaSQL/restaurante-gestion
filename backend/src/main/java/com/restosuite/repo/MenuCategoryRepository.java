package com.restosuite.repo;

import com.restosuite.domain.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long>, JpaSpecificationExecutor<MenuCategory> {

    java.util.List<MenuCategory> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);


}
