package com.restosuite.repo;

import com.restosuite.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    java.util.List<Product> findByRestaurantIdAndActiveTrueOrderByNameAsc(Long restaurantId);
    java.util.Optional<Product> findByIdAndRestaurantId(Long id, Long restaurantId);
    java.util.Optional<Product> findByRestaurantIdAndSkuIgnoreCase(Long restaurantId, String sku);


    @org.springframework.data.jpa.repository.Query(value = "select modifier_id from product_modifiers where product_id = :productId", nativeQuery = true)
    java.util.List<Long> modifierIds(Long productId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "delete from product_modifiers where product_id = :productId", nativeQuery = true)
    void clearModifiers(Long productId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "insert into product_modifiers(product_id, modifier_id) values (:productId, :modifierId)", nativeQuery = true)
    void addModifier(Long productId, Long modifierId);

}
