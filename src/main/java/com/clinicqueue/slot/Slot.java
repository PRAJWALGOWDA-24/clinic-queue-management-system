package com.clinicqueue.slot;

import com.clinicqueue.common.audit.BaseEntity;
import com.clinicqueue.doctor.Doctor;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "slots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_slot_doctor_date_start",
                columnNames = {"doctor_id", "date", "start_time"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Slot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private boolean isBooked = false;

    @Version
    private Integer version;
}
