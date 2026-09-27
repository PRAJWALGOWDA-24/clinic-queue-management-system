package com.clinicqueue.clinicsettings;

import com.clinicqueue.common.audit.BaseEntity;
import com.clinicqueue.doctor.Doctor;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clinic_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "doctor_id", nullable = false, unique = true)
    private Doctor doctor;

    @Builder.Default
    private Integer graceMinutes = 5;

    @Builder.Default
    private Integer maxSkips = 2;

    @Builder.Default
    private Integer reminderAtPosition = 2;
}