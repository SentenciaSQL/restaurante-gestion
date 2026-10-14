package com.restosuite.repo;

import com.restosuite.domain.SalesOrderDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesOrderDiscountRepository extends JpaRepository<SalesOrderDiscount, Long>, JpaSpecificationExecutor<SalesOrderDiscount> {

    java.util.List<SalesOrderDiscount> findByOrderIdOrderByIdAsc(Long orderId);


}
