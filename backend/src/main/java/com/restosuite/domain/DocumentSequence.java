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
@Table(name = "document_sequences")
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false, length = 30)
    private String docType;

    @Column(nullable = false, length = 12)
    private String prefix;

    @Column(nullable = false)
    private Long nextNumber;

    @Column(nullable = false)
    private Integer padding;
}
