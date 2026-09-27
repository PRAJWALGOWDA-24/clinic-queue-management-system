package com.clinicqueue.doctor;

import com.clinicqueue.common.dto.ApiResponse;
import com.clinicqueue.doctor.dto.CreateDoctorRequest;
import com.clinicqueue.doctor.dto.DoctorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        return ApiResponse.success("Doctor created", doctorService.createDoctor(request));
    }

    @GetMapping
    public ApiResponse<Page<DoctorResponse>> getAllDoctors(Pageable pageable) {
        return ApiResponse.success("Doctors fetched", doctorService.getAllDoctors(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<DoctorResponse> getDoctorById(@PathVariable Long id) {
        return ApiResponse.success("Doctor fetched", doctorService.getDoctorById(id));
    }
}