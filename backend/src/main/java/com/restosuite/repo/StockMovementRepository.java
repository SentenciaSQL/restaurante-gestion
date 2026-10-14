package com.restosuite.repo;

import com.restosuite.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    java.util.Optional<StockMovement> findByIdempotencyKey(String idempotencyKey);
    java.util.List<StockMovement> findTop200ByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
    java.util.List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);


}
