package com.restosuite.repo;

import com.restosuite.domain.TableReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TableReservationRepository extends JpaRepository<TableReservation, Long>, JpaSpecificationExecutor<TableReservation> {

    java.util.List<TableReservation> findByTableIdOrderByReservedFromDesc(Long tableId);


}
