package com.restosuite.repo;

import com.restosuite.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    java.util.Optional<Role> findByCode(String code);
    java.util.List<Role> findAllByOrderByNameAsc();


    @org.springframework.data.jpa.repository.Query(value = "select p.code from permissions p join role_permissions rp on rp.permission_id = p.id where rp.role_id = :roleId order by p.code", nativeQuery = true)
    java.util.List<String> permissionCodes(Long roleId);

}
