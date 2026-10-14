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
@Table(name = "sales_order_lines")
public class SalesOrderLine extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column
    private Long productId;

    @Column
    private Long variantId;

    @Column(nullable = false, length = 160)
    private String productName;

    @Column(length = 80)
    private String variantName;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal modifierAmount;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal enteredAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineDiscount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal taxRate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal costAmount;

    @Column(nullable = false)
    private boolean inventoryDeducted;

    @Column(nullable = false)
    private boolean restocked;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 300)
    private String notes;

    @Column
    private Long stationId;

    @Column(length = 80)
    private String stationName;

    @Column(length = 300)
    private String cancelReason;

    @Column
    private Instant sentAt;

    @Column
    private Instant readyAt;

    @Column
    private Instant deliveredAt;

    @Column
    private Instant cancelledAt;
}
