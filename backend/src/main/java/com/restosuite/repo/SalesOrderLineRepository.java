package com.restosuite.repo;

import com.restosuite.domain.SalesOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long>, JpaSpecificationExecutor<SalesOrderLine> {

    java.util.List<SalesOrderLine> findByOrderIdOrderByIdAsc(Long orderId);


}
