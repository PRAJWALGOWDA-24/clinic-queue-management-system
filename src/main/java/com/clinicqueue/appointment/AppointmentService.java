package com.clinicqueue.appointment;

import com.clinicqueue.appointment.dto.AppointmentResponse;
import com.clinicqueue.appointment.dto.BookSlotRequest;
import com.clinicqueue.auditlog.AuditLogService;
import com.clinicqueue.common.exception.BadRequestException;
import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.patient.Patient;
import com.clinicqueue.patient.PatientRepository;
import com.clinicqueue.slot.Slot;
import com.clinicqueue.slot.SlotRepository;
import com.clinicqueue.slot.SlotService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final PatientRepository patientRepository;
    private final AuditLogService auditLogService;
    private final SlotService slotService;

    @Transactional
    public AppointmentResponse bookSlot(BookSlotRequest request) {
        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + request.getSlotId()));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + request.getPatientId()));

        if (slot.isBooked()) {  //very impo , interview what are all problmes u faced  , see onenote
            throw new BadRequestException("This slot is already booked");
        }

        try {
            slot.setBooked(true);
            slotRepository.saveAndFlush(slot);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {  //this is were we egt when 2 uses trying to bok same slot at a same time but person a booked at microsec then goes to version 1 and
            // person b micros ec later tries ook then they got this  above exce[tion
            throw new BadRequestException("This slot was just booked by someone else. Please choose another slot.");
        }

        Appointment appointment = Appointment.builder()
                .slot(slot)
                .patient(patient)
                .status(AppointmentStatus.BOOKED)
                .build();

        appointmentRepository.save(appointment);

        // slot just got booked — the cached "available slots" list is now stale, clear it
        slotService.invalidateCache(slot.getDoctor().getId(), slot.getDate());

        auditLogService.log(patient.getUser().getId(), "BOOK_APPOINTMENT", "Appointment", appointment.getId());

        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        Slot slot = appointment.getSlot();
        slot.setBooked(false);
        slotRepository.save(slot);

        // cancelling frees the slot — cache is stale again, clear it
        slotService.invalidateCache(slot.getDoctor().getId(), slot.getDate());

        auditLogService.log(appointment.getPatient().getUser().getId(), "CANCEL_APPOINTMENT", "Appointment", appointment.getId());

        return toResponse(appointment);
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .slotId(appointment.getSlot().getId())
                .doctorName(appointment.getSlot().getDoctor().getUser().getFullName())
                .patientName(appointment.getPatient().getUser().getFullName())
                .date(appointment.getSlot().getDate())
                .startTime(appointment.getSlot().getStartTime())
                .status(appointment.getStatus())
                .build();
    }
}

/*

"Walk me through what happens when a slot gets booked."

"The service checks isBooked first as a fast guard. Then it sets isBooked=true and calls saveAndFlush(), which forces Hibernate to immediately run the UPDATE with a WHERE version = ? clause. If two requests race, only one's UPDATE matches, and the loser gets an ObjectOptimisticLockingFailureException, which I catch and convert into a clean error message."

Q: "Why saveAndFlush() and not just save()?"

"save() might defer the actual SQL execution to the end of the transaction. saveAndFlush() forces it to run immediately, so if there's a version conflict, it surfaces right there in my try/catch — not silently later where I can't handle it cleanly."

Q: "What if two people book at the exact same time — what does the LOSER actually see?"

"They get a 400 Bad Request with a message like 'This slot was just booked by someone else' — never a raw 500 error or a confusing stack trace."

Q: "Is this the same as a database lock (like SELECT FOR UPDATE)?"

"No — that's pessimistic locking, where you lock the row upfront and make other requests WAIT. My approach is optimistic locking — no waiting, no locks held; we just detect a conflict after the fact, using the version number, and reject the loser."

Q: "Why optimistic instead of pessimistic here?"

"Optimistic locking is lighter — no requests get blocked waiting for a lock. Since slot-booking conflicts are RARE (most bookings don't collide), optimistic locking is more efficient than pessimistically locking every single booking request just in case."

Q: "What's a real limitation or gap in your current implementation?"

"Right now, nothing stops the same doctor from having two overlapping slots created in the first place — @Version only prevents double-booking of the SAME slot row, not duplicate slot creation. I'd add a @UniqueConstraint on (doctor_id, date, startTime) to close that gap."
 */