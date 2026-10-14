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
@Table(name = "kitchen_tickets")
public class KitchenTicket extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long branchId;

    @Column
    private Long stationId;

    @Column(nullable = false, length = 80)
    private String stationName;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant readyAt;

    @Column
    private Instant deliveredAt;
}
