package com.restosuite.repo;

import com.restosuite.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    java.util.List<AuditLog> findTop20ByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);


}
