package com.clinicqueue.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookSlotRequest {

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;
}