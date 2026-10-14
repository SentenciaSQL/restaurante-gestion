package com.restosuite.repo;

import com.restosuite.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    java.util.List<Payment> findByOrderIdOrderByIdAsc(Long orderId);
    java.util.Optional<Payment> findByIdempotencyKey(String idempotencyKey);


}
