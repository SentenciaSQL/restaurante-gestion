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
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends com.restosuite.common.CreatedOnly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column
    private Long sessionId;

    @Column(nullable = false)
    private Long methodId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal tipAmount;

    @Column(length = 80)
    private String reference;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 80)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant paidAt;

    @Column
    private Long userId;

    @Column(length = 300)
    private String voidReason;

    @Column
    private Instant voidedAt;

    @Column
    private Long voidedBy;

    @Column
    private Long cashMovementId;
}
