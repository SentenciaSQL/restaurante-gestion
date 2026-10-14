package com.restosuite.repo;

import com.restosuite.domain.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long>, JpaSpecificationExecutor<StockLevel> {

    java.util.Optional<StockLevel> findByWarehouseIdAndIngredientId(Long warehouseId, Long ingredientId);
    java.util.List<StockLevel> findByWarehouseId(Long warehouseId);


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from StockLevel s where s.warehouseId = :warehouseId and s.ingredientId = :ingredientId")
    java.util.Optional<StockLevel> lock(Long warehouseId, Long ingredientId);
}
