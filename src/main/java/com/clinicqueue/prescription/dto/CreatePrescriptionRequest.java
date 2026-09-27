package com.clinicqueue.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePrescriptionRequest {
    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;

    private String notes;

    @NotEmpty(message = "At least one medicine is required")
    @Valid
    private List<PrescriptionItemRequest> items;
}