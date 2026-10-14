package com.restosuite.repo;

import com.restosuite.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {

    java.util.List<ProductVariant> findByProductIdOrderBySortOrderAsc(Long productId);


}
