package com.clinicqueue.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrescriptionItemRequest {
    @NotBlank(message = "Medicine name is required")
    private String medicineName;

    @NotBlank(message = "Dose is required")
    private String dose;

    @NotNull(message = "Days is required")
    @Positive(message = "Days must be positive")
    private Integer days;
}