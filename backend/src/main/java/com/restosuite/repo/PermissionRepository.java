package com.restosuite.repo;

import com.restosuite.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

    java.util.List<Permission> findAllByOrderByModuleAscNameAsc();
    java.util.Optional<Permission> findByCode(String code);


}
