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
@Table(name = "stock_movements")
public class StockMovement extends com.restosuite.common.CreatedOnly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long ingredientId;

    @Column(nullable = false, length = 30)
    private String movementType;

    @Column(nullable = false, length = 3)
    private String direction;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(length = 40)
    private String referenceType;

    @Column(length = 40)
    private String referenceId;

    @Column(length = 300)
    private String reason;

    @Column
    private Long userId;

    @Column(length = 80)
    private String idempotencyKey;
}
