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
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false, length = 40)
    private String number;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDate orderDate;

    @Column
    private LocalDate expectedDate;

    @Column(length = 400)
    private String notes;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Column
    private Long createdBy;
}
