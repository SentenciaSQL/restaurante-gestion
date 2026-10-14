package com.restosuite.repo;

import com.restosuite.domain.KitchenTicketLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KitchenTicketLineRepository extends JpaRepository<KitchenTicketLine, Long>, JpaSpecificationExecutor<KitchenTicketLine> {

    java.util.List<KitchenTicketLine> findByTicketId(Long ticketId);
    java.util.List<KitchenTicketLine> findByOrderLineId(Long orderLineId);


}
