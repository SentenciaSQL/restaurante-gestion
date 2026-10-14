package com.restosuite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "refunds")
public class Refund extends com.restosuite.common.CreatedOnly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal costAmount;

    @Column(nullable = false)
    private boolean restock;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column
    private Long authorizedBy;

    @Column(length = 80)
    private String idempotencyKey;

    @Column
    private Long cashMovementId;
}
