package com.restosuite.repo;

import com.restosuite.domain.SalesOrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesOrderEventRepository extends JpaRepository<SalesOrderEvent, Long>, JpaSpecificationExecutor<SalesOrderEvent> {

    java.util.List<SalesOrderEvent> findByOrderIdOrderByCreatedAtAsc(Long orderId);


}
