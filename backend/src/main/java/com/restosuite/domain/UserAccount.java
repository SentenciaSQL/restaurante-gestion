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
@Table(name = "users")
public class UserAccount extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restaurantId;

    @Column
    private Long branchId;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(length = 40)
    private String phone;

    @Column(nullable = false)
    private boolean active;
}
