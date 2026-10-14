package com.restosuite.repo;

import com.restosuite.domain.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long>, JpaSpecificationExecutor<GoodsReceipt> {

    java.util.Optional<GoodsReceipt> findByIdempotencyKey(String idempotencyKey);
    java.util.List<GoodsReceipt> findByPurchaseOrderId(Long purchaseOrderId);


}
