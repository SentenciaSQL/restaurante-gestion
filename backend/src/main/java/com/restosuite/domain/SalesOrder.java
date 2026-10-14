package com.restosuite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "sales_orders")
public class SalesOrder extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false, length = 40)
    private String orderNumber;

    @Column(nullable = false, length = 20)
    private String orderType;

    @Column
    private Long tableId;

    @Column
    private Long waiterId;

    @Column(length = 160)
    private String customerName;

    @Column(length = 40)
    private String customerPhone;

    @Column(length = 300)
    private String deliveryAddress;

    @Column
    private Integer guestCount;

    @Column(nullable = false, length = 20)
    private String preparationStatus;

    @Column(nullable = false, length = 20)
    private String accountStatus;

    @Column(nullable = false)
    private boolean pricesIncludeTax;

    @Column(nullable = false, length = 20)
    private String inventoryTrigger;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal tipAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal headerDiscount;

    @Column(length = 400)
    private String notes;

    @Column(length = 300)
    private String cancelReason;

    @Column(nullable = false)
    private Instant openedAt;

    @Column
    private Instant closedAt;

    @Column
    private Instant cancelledAt;

    @Column
    private Long createdBy;
}
