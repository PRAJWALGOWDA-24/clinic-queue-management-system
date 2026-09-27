package com.clinicqueue.pharmacy;

import com.clinicqueue.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pharmacies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pharmacy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;
}