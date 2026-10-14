package com.restosuite.repo;

import com.restosuite.domain.KitchenTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, Long>, JpaSpecificationExecutor<KitchenTicket> {

    java.util.List<KitchenTicket> findByOrderId(Long orderId);


}
