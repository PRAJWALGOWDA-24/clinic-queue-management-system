package com.clinicqueue.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDoctorRequest {

    @NotNull(message = "User ID is required")
    private Long userId; // the existing registered user (role=DOCTOR) to attach this profile to

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotNull(message = "Consultation fee is required")
    @Positive(message = "Fee must be positive")
    private Double consultationFee;

    @NotNull(message = "Average consult minutes is required")
    @Positive(message = "Must be positive")
    private Integer avgConsultMinutes;
}