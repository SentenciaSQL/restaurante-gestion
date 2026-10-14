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
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "expenses")
public class Expense extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long categoryId;

    @Column
    private Long supplierId;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column
    private Long paymentMethodId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 80)
    private String receiptRef;

    @Column(length = 400)
    private String notes;

    @Column
    private Long createdBy;

    @Column
    private Long paidBy;

    @Column
    private Instant paidAt;

    @Column
    private Long cashMovementId;

    @Column(length = 300)
    private String voidReason;
}
