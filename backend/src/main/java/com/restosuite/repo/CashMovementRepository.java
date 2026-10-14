package com.restosuite.repo;

import com.restosuite.domain.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long>, JpaSpecificationExecutor<CashMovement> {

    java.util.List<CashMovement> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    java.util.Optional<CashMovement> findByIdempotencyKey(String idempotencyKey);


}
