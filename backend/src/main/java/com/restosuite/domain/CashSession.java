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
@Table(name = "cash_sessions")
public class CashSession extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long openedBy;

    @Column
    private Long closedBy;

    @Column(nullable = false)
    private Instant openedAt;

    @Column
    private Instant closedAt;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal openingFloat;

    @Column(precision = 14, scale = 2)
    private BigDecimal expectedCash;

    @Column(precision = 14, scale = 2)
    private BigDecimal countedCash;

    @Column(precision = 14, scale = 2)
    private BigDecimal difference;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 400)
    private String notes;

    @Column(length = 400)
    private String closeNotes;
}
