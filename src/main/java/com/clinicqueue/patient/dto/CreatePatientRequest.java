package com.clinicqueue.patient.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreatePatientRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String bloodGroup;
}