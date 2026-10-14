package com.restosuite.repo;

import com.restosuite.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RefundRepository extends JpaRepository<Refund, Long>, JpaSpecificationExecutor<Refund> {

    java.util.List<Refund> findByOrderIdOrderByIdAsc(Long orderId);
    java.util.List<Refund> findByPaymentId(Long paymentId);
    java.util.Optional<Refund> findByIdempotencyKey(String idempotencyKey);


}
