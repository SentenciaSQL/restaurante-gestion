package com.restosuite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long purchaseOrderId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(length = 80)
    private String documentRef;

    @Column(length = 400)
    private String notes;

    @Column
    private Long receivedBy;

    @Column(length = 80)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant receivedAt;
}
