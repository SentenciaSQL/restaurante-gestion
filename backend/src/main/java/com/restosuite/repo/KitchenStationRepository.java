package com.restosuite.repo;

import com.restosuite.domain.KitchenStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KitchenStationRepository extends JpaRepository<KitchenStation, Long>, JpaSpecificationExecutor<KitchenStation> {

    java.util.List<KitchenStation> findByBranchIdOrderBySortOrderAsc(Long branchId);


}
