package com.restosuite.repo;

import com.restosuite.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {

    java.util.Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    org.springframework.data.domain.Page<UserAccount> findByRestaurantId(Long restaurantId, org.springframework.data.domain.Pageable pageable);


    @org.springframework.data.jpa.repository.Query(value = "select r.code from roles r join user_roles ur on ur.role_id = r.id where ur.user_id = :userId", nativeQuery = true)
    java.util.List<String> roleCodes(Long userId);

    @org.springframework.data.jpa.repository.Query(value = "select distinct p.code from permissions p join role_permissions rp on rp.permission_id = p.id join user_roles ur on ur.role_id = rp.role_id where ur.user_id = :userId", nativeQuery = true)
    java.util.List<String> permissionCodes(Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "delete from user_roles where user_id = :userId", nativeQuery = true)
    void clearRoles(Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "insert into user_roles(user_id, role_id) values (:userId, :roleId)", nativeQuery = true)
    void addRole(Long userId, Long roleId);

}
