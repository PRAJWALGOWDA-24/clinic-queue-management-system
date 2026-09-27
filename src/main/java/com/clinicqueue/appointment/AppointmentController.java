package com.clinicqueue.appointment;

import com.clinicqueue.appointment.dto.AppointmentResponse;
import com.clinicqueue.appointment.dto.BookSlotRequest;
import com.clinicqueue.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/book")
    public ApiResponse<AppointmentResponse> bookSlot(@Valid @RequestBody BookSlotRequest request) {
        return ApiResponse.success("Slot booked successfully", appointmentService.bookSlot(request));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancelAppointment(@PathVariable Long id) {
        return ApiResponse.success("Appointment cancelled", appointmentService.cancelAppointment(id));
    }
}