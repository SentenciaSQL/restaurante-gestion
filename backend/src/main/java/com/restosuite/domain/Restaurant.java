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
@Table(name = "restaurants")
public class Restaurant extends com.restosuite.common.Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 200)
    private String legalName;

    @Column(length = 40)
    private String taxId;

    @Column(length = 40)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(length = 300)
    private String address;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 80)
    private String timezone;

    @Column(nullable = false, length = 20)
    private String locale;
}
