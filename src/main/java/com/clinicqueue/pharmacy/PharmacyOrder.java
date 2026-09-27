package com.clinicqueue.pharmacy;

import com.clinicqueue.common.audit.BaseEntity;
import com.clinicqueue.prescription.Prescription;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pharmacy_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PharmacyOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "prescription_id", nullable = false, unique = true)
    private Prescription prescription;

    @ManyToOne
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.RECEIVED;
}


