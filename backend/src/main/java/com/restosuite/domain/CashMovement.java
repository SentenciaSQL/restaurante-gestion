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
@Table(name = "cash_movements")
public class CashMovement extends com.restosuite.common.CreatedOnly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 30)
    private String movementType;

    @Column(nullable = false, length = 3)
    private String direction;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean affectsCash;

    @Column
    private Long paymentMethodId;

    @Column(length = 40)
    private String referenceType;

    @Column(length = 40)
    private String referenceId;

    @Column(length = 300)
    private String description;

    @Column
    private Long userId;

    @Column(length = 80)
    private String idempotencyKey;
}
