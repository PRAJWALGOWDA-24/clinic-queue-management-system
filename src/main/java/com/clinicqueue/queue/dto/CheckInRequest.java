package com.clinicqueue.queue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInRequest {
    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;
}