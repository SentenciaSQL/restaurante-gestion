package com.restosuite.repo;

import com.restosuite.domain.GoodsReceiptLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLine, Long>, JpaSpecificationExecutor<GoodsReceiptLine> {

    java.util.List<GoodsReceiptLine> findByReceiptId(Long receiptId);


}
