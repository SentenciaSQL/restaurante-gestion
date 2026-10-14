package com.restosuite.repo;

import com.restosuite.domain.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long>, JpaSpecificationExecutor<PurchaseOrderLine> {

    java.util.List<PurchaseOrderLine> findByPurchaseOrderId(Long purchaseOrderId);


}
