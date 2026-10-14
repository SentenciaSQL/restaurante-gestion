package com.restosuite.repo;

import com.restosuite.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long>, JpaSpecificationExecutor<Warehouse> {

    java.util.List<Warehouse> findByBranchIdOrderByNameAsc(Long branchId);
    java.util.Optional<Warehouse> findByBranchIdAndCode(Long branchId, String code);


}
