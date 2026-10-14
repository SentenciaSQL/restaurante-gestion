package com.restosuite.repo;

import com.restosuite.domain.CashSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CashSessionRepository extends JpaRepository<CashSession, Long>, JpaSpecificationExecutor<CashSession> {

    java.util.Optional<CashSession> findByBranchIdAndStatus(Long branchId, String status);


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from CashSession s where s.id = :id")
    java.util.Optional<CashSession> lockById(Long id);

}
