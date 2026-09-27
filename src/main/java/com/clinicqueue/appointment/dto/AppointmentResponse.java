package com.clinicqueue.appointment.dto;

import com.clinicqueue.appointment.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private Long slotId;
    private String doctorName;
    private String patientName;
    private LocalDate date;
    private LocalTime startTime;
    private AppointmentStatus status;
}