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
@Table(name = "ingredients")
public class Ingredient extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false, length = 40)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private Long unitId;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal minStock;

    @Column(nullable = false)
    private boolean active;
}
