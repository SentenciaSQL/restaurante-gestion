package com.restosuite.repo;

import com.restosuite.domain.DiningArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DiningAreaRepository extends JpaRepository<DiningArea, Long>, JpaSpecificationExecutor<DiningArea> {

    java.util.List<DiningArea> findByBranchIdOrderBySortOrderAsc(Long branchId);


}
