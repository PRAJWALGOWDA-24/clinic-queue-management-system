package com.clinicqueue.prescription;

import com.clinicqueue.appointment.Appointment;
import com.clinicqueue.appointment.AppointmentRepository;
import com.clinicqueue.common.exception.BadRequestException;
import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.notification.NotificationService;
import com.clinicqueue.pharmacy.OrderStatus;
import com.clinicqueue.pharmacy.Pharmacy;
import com.clinicqueue.pharmacy.PharmacyOrder;
import com.clinicqueue.pharmacy.PharmacyOrderRepository;
import com.clinicqueue.pharmacy.PharmacyRepository;
import com.clinicqueue.prescription.dto.CreatePrescriptionRequest;
import com.clinicqueue.prescription.dto.PrescriptionItemRequest;
import com.clinicqueue.prescription.dto.PrescriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final NotificationService notificationService;
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PharmacyOrderRepository pharmacyOrderRepository;

    @Transactional
    public PrescriptionResponse createPrescription(CreatePrescriptionRequest request) {

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (prescriptionRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new BadRequestException("Prescription already exists for this appointment");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found"));

        Prescription prescription = Prescription.builder()
                .appointment(appointment)
                .notes(request.getNotes())
                .build();

        List<PrescriptionItem> items = request.getItems().stream()
                .map(i -> PrescriptionItem.builder()
                        .prescription(prescription)
                        .medicineName(i.getMedicineName())
                        .dose(i.getDose())
                        .days(i.getDays())
                        .build())
                .toList();

        prescription.setItems(items);
        prescriptionRepository.save(prescription); // cascade saves items too

        PharmacyOrder order = PharmacyOrder.builder()
                .prescription(prescription)
                .pharmacy(pharmacy)
                .status(OrderStatus.RECEIVED)
                .build();
        pharmacyOrderRepository.save(order);
        notificationService.notifyUser(appointment.getPatient().getUser().getId(),
                "Your prescription was sent to " + pharmacy.getName() + ".");
        return toResponse(prescription, order);
    }

    public PrescriptionResponse getByAppointmentId(Long appointmentId) {
        Prescription prescription = prescriptionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        PharmacyOrder order = pharmacyOrderRepository.findByPrescriptionId(prescription.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy order not found"));
        return toResponse(prescription, order);
    }

    private PrescriptionResponse toResponse(Prescription prescription, PharmacyOrder order) {
        List<PrescriptionItemRequest> items = prescription.getItems().stream()
                .map(i -> {
                    PrescriptionItemRequest dto = new PrescriptionItemRequest();
                    dto.setMedicineName(i.getMedicineName());
                    dto.setDose(i.getDose());
                    dto.setDays(i.getDays());
                    return dto;
                }).toList();

        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .patientName(prescription.getAppointment().getPatient().getUser().getFullName())
                .doctorName(prescription.getAppointment().getSlot().getDoctor().getUser().getFullName())
                .notes(prescription.getNotes())
                .items(items)
                .pharmacyName(order.getPharmacy().getName())
                .orderStatus(order.getStatus().name())
                .build();
    }
}