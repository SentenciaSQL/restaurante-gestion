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
@Table(name = "units")
public class UnitOfMeasure extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 12)
    private String symbol;

    @Column(nullable = false, length = 20)
    private String family;

    @Column
    private Long baseUnitId;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal factorToBase;

    @Column(nullable = false)
    private boolean active;
}
