package com.restosuite.repo;

import com.restosuite.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    java.util.Optional<PurchaseOrder> findByBranchIdAndNumber(Long branchId, String number);


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PurchaseOrder p where p.id = :id")
    java.util.Optional<PurchaseOrder> lockById(Long id);

}
