package com.clinicqueue.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PrescriptionResponse {
    private Long id;
    private String patientName;
    private String doctorName;
    private String notes;
    private List<PrescriptionItemRequest> items;
    private String pharmacyName;
    private String orderStatus;
}