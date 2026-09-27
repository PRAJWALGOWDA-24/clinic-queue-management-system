package com.clinicqueue.patient;

import com.clinicqueue.common.exception.BadRequestException;
import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.patient.dto.CreatePatientRequest;
import com.clinicqueue.patient.dto.PatientResponse;
import com.clinicqueue.user.Role;
import com.clinicqueue.user.User;
import com.clinicqueue.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public PatientResponse createPatient(CreatePatientRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        if (user.getRole() != Role.PATIENT) {
            throw new BadRequestException("User must have role PATIENT to create a patient profile");
        }

        if (patientRepository.existsByUserId(user.getId())) {
            throw new BadRequestException("Patient profile already exists for this user");
        }

        Patient patient = Patient.builder()
                .user(user)
                .dateOfBirth(request.getDateOfBirth())
                .bloodGroup(request.getBloodGroup())
                .build();

        patientRepository.save(patient);

        return toResponse(patient);
    }

    public PatientResponse getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
        return toResponse(patient);
    }

    private PatientResponse toResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .fullName(patient.getUser().getFullName())
                .email(patient.getUser().getEmail())
                .dateOfBirth(patient.getDateOfBirth())
                .bloodGroup(patient.getBloodGroup())
                .build();
    }
}