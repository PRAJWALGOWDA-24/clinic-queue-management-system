package com.clinicqueue.queue;

import com.clinicqueue.appointment.Appointment;
import com.clinicqueue.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QueueEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QueueStatus status = QueueStatus.WAITING;

    private LocalDateTime checkedInAt;

    @Builder.Default
    private Integer skipCount = 0;
}



