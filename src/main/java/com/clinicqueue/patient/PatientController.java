package com.clinicqueue.patient;

import com.clinicqueue.common.dto.ApiResponse;
import com.clinicqueue.patient.dto.CreatePatientRequest;
import com.clinicqueue.patient.dto.PatientResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public ApiResponse<PatientResponse> createPatient(@Valid @RequestBody CreatePatientRequest request) {
        return ApiResponse.success("Patient created", patientService.createPatient(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PatientResponse> getPatientById(@PathVariable Long id) {
        return ApiResponse.success("Patient fetched", patientService.getPatientById(id));
    }
}