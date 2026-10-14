package com.restosuite.repo;

import com.restosuite.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReceiptRepository extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {

    java.util.Optional<Receipt> findByOrderId(Long orderId);
    java.util.Optional<Receipt> findByNumber(String number);


}
