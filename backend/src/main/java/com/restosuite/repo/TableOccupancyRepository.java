package com.restosuite.repo;

import com.restosuite.domain.TableOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TableOccupancyRepository extends JpaRepository<TableOccupancy, Long>, JpaSpecificationExecutor<TableOccupancy> {

    java.util.List<TableOccupancy> findByTableIdOrderByOpenedAtDesc(Long tableId);


}
