package com.clinicqueue.doctor;

import com.clinicqueue.common.exception.BadRequestException;
import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.doctor.dto.CreateDoctorRequest;
import com.clinicqueue.doctor.dto.DoctorResponse;
import com.clinicqueue.user.Role;
import com.clinicqueue.user.User;
import com.clinicqueue.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public DoctorResponse createDoctor(CreateDoctorRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        if (user.getRole() != Role.DOCTOR) {
            throw new BadRequestException("User must have role DOCTOR to create a doctor profile");
        }

        if (doctorRepository.existsByUserId(user.getId())) {
            throw new BadRequestException("Doctor profile already exists for this user");
        }

        Doctor doctor = Doctor.builder()
                .user(user)
                .specialization(request.getSpecialization())
                .consultationFee(request.getConsultationFee())
                .avgConsultMinutes(request.getAvgConsultMinutes())
                .build();

        doctorRepository.save(doctor);
        return toResponse(doctor);
    }

    public Page<DoctorResponse> getAllDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable).map(this::toResponse);
    }

    public DoctorResponse getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
        return toResponse(doctor);
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .fullName(doctor.getUser().getFullName())
                .email(doctor.getUser().getEmail())
                .specialization(doctor.getSpecialization())
                .consultationFee(doctor.getConsultationFee())
                .avgConsultMinutes(doctor.getAvgConsultMinutes())
                .build();
    }
}