package com.restosuite.repo;

import com.restosuite.domain.SalesOrderLineModifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesOrderLineModifierRepository extends JpaRepository<SalesOrderLineModifier, Long>, JpaSpecificationExecutor<SalesOrderLineModifier> {

    java.util.List<SalesOrderLineModifier> findByOrderLineId(Long orderLineId);


}
