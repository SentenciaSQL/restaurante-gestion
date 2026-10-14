package com.restosuite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "table_occupancy")
public class TableOccupancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tableId;

    @Column
    private Long orderId;

    @Column
    private Long waiterId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant openedAt;

    @Column
    private Instant closedAt;

    @Column(length = 300)
    private String notes;
}
