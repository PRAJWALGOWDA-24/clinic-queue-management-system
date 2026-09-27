package com.clinicqueue.prescription;

import com.clinicqueue.common.dto.ApiResponse;
import com.clinicqueue.prescription.dto.CreatePrescriptionRequest;
import com.clinicqueue.prescription.dto.PrescriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    public ApiResponse<PrescriptionResponse> create(@Valid @RequestBody CreatePrescriptionRequest request) {
        return ApiResponse.success("Prescription created", prescriptionService.createPrescription(request));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<PrescriptionResponse> getByAppointment(@PathVariable Long appointmentId) {
        return ApiResponse.success("Prescription fetched", prescriptionService.getByAppointmentId(appointmentId));
    }
}