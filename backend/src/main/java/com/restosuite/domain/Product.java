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
@Table(name = "products")
public class Product extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 40)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 600)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 20)
    private String productType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private Long taxRateId;

    @Column(nullable = false)
    private Integer prepMinutes;

    @Column
    private Long stationId;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean trackInventory;
}
