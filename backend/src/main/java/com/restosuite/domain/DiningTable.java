package com.restosuite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dining_tables")
public class DiningTable extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long areaId;

    @Column(name = "number", nullable = false, length = 20)
    private String tableNumber;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private Long assignedWaiterId;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 300)
    private String notes;
}
