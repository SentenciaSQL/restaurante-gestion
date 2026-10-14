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
@Table(name = "table_reservations")
public class TableReservation extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tableId;

    @Column(nullable = false, length = 160)
    private String customerName;

    @Column(length = 40)
    private String phone;

    @Column(nullable = false)
    private Integer partySize;

    @Column(nullable = false)
    private Instant reservedFrom;

    @Column(nullable = false)
    private Instant reservedUntil;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 300)
    private String notes;

    @Column
    private Long createdBy;
}
