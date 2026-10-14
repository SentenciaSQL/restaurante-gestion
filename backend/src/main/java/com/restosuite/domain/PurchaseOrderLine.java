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
@Table(name = "purchase_order_lines")
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long purchaseOrderId;

    @Column(nullable = false)
    private Long ingredientId;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityOrdered;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityReceived;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal taxRate;
}
