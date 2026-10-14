package com.restosuite.repo;

import com.restosuite.domain.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long>, JpaSpecificationExecutor<DiningTable> {

    java.util.List<DiningTable> findByAreaIdOrderByTableNumberAsc(Long areaId);
    java.util.Optional<DiningTable> findByAreaIdAndTableNumber(Long areaId, String tableNumber);


}
