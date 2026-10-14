package com.restosuite.repo;

import com.restosuite.domain.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    java.util.Optional<SalesOrder> findByBranchIdAndOrderNumber(Long branchId, String orderNumber);
    java.util.Optional<SalesOrder> findByTableIdAndAccountStatus(Long tableId, String accountStatus);


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from SalesOrder o where o.id = :id")
    java.util.Optional<SalesOrder> lockById(Long id);

}
