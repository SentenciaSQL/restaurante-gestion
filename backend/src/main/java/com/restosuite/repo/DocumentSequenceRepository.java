package com.restosuite.repo;

import com.restosuite.domain.DocumentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long>, JpaSpecificationExecutor<DocumentSequence> {

    java.util.Optional<DocumentSequence> findByBranchIdAndDocType(Long branchId, String docType);


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from DocumentSequence s where s.branchId = :branchId and s.docType = :docType")
    java.util.Optional<DocumentSequence> lock(Long branchId, String docType);

}
