package com.clinicqueue.queue;

import com.clinicqueue.appointment.Appointment;
import com.clinicqueue.appointment.AppointmentRepository;
import com.clinicqueue.auditlog.AuditLogService;
import com.clinicqueue.clinicsettings.ClinicSettings;
import com.clinicqueue.clinicsettings.ClinicSettingsRepository;
import com.clinicqueue.common.exception.BadRequestException;
import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.queue.dto.CheckInRequest;
import com.clinicqueue.queue.dto.QueueEntryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicSettingsRepository clinicSettingsRepository;
    private final AuditLogService auditLogService;

    public QueueEntryResponse checkIn(CheckInRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (queueEntryRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new BadRequestException("Patient already checked in for this appointment");  //this si idempotency
        }

        Long doctorId = appointment.getSlot().getDoctor().getId();

        int currentWaitingCount = queueEntryRepository  //How position gets calculated   ,,
                .findByAppointment_Slot_Doctor_IdAndStatus(doctorId, QueueStatus.WAITING, Pageable.unpaged())
                .getContent()
                .size();    //Real-world: imagine walking into the waiting room and asking "how many people are ahead of me?"
        // Someone counts the people currently sitting there (currentWaitingCount), and you become the next number after that.
        // We don't store a running counter anywhere — every time someone checks in, we freshly COUNT who's currently waiting. This avoids a counter ever drifting out of sync with reality (a concept we covered earlier).

        QueueEntry entry = QueueEntry.builder()
                .appointment(appointment)
                .position(currentWaitingCount + 1)
                .status(QueueStatus.WAITING)
                .checkedInAt(LocalDateTime.now())
                .skipCount(0)
                .build();

        queueEntryRepository.save(entry);
        auditLogService.log(appointment.getPatient().getUser().getId(), "CHECK_IN", "QueueEntry", entry.getId());

        return toResponse(entry);
    }

    public Page<QueueEntryResponse> getQueueForDoctor(Long doctorId, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("position").ascending());
        return queueEntryRepository
                .findByAppointment_Slot_Doctor_IdAndStatus(doctorId, QueueStatus.WAITING, sorted)
                .map(this::toResponse);
    }

    public QueueEntryResponse callNext(Long doctorId) {
        List<QueueEntry> waiting = queueEntryRepository  //Call Next — the doctor's buzzer
                .findByAppointment_Slot_Doctor_IdAndStatus(doctorId, QueueStatus.WAITING,
                        PageRequest.of(0, 1, Sort.by("position").ascending()))
                .getContent();

        if (waiting.isEmpty()) {
            throw new BadRequestException("No patients waiting in queue");
        }

        //Real-world: the doctor presses a buzzer. The system doesn't care WHO specifically —
        // it just always grabs whoever currently has the LOWEST position number among people still WAITING. This is why (from your earlier question) the doctor is never
        // idle because of a skip — whoever's skipped simply falls out
        // of this list, and calling "next" automatically picks up whoever's now at the front.

        QueueEntry next = waiting.get(0);
        next.setStatus(QueueStatus.CALLED);
        queueEntryRepository.save(next);
        auditLogService.log(next.getAppointment().getPatient().getUser().getId(), "CALL_NEXT", "QueueEntry", next.getId());

        return toResponse(next);
        /*
        the doctor does NOT sit idle. Here's why — callNext() doesn't care
        about anyone's original time, it just always grabs whoever is at position = 1 among people still WAITING:
    */
    }



    public QueueEntryResponse skipPatient(Long queueEntryId) {
        QueueEntry entry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found"));

        ClinicSettings settings = clinicSettingsRepository.findAll().stream()
                .filter(s -> s.getDoctor().getId().equals(entry.getAppointment().getSlot().getDoctor().getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Clinic settings not configured for this doctor"));

        entry.setSkipCount(entry.getSkipCount() + 1);

        if (entry.getSkipCount() >= settings.getMaxSkips()) {
            entry.setStatus(QueueStatus.CANCELLED);
        } else {
            entry.setStatus(QueueStatus.SKIPPED);
            entry.setPosition(entry.getPosition() + 3);  //if after patet  skipped thne they came mean then kepp them  at next 3 position
        }
//Real-world: "Token #4, please come to counter" — nobody shows up after calling twice. The receptionist marks them skipped,
// pushes them back 3 spots in line ("we'll try you again after 3 more people"), and increments their skip count. If they've already
// been skipped maxSkips times (a per-doctor configurable rule, from clinic_settings), instead of skipping again, we just cancel their visit entirely — they've had too many chances.
        queueEntryRepository.save(entry);
        auditLogService.log(entry.getAppointment().getPatient().getUser().getId(), "SKIP_PATIENT", "QueueEntry", entry.getId());

        return toResponse(entry);
    }

    private QueueEntryResponse toResponse(QueueEntry entry) {
        return QueueEntryResponse.builder()
                .id(entry.getId())
                .patientName(entry.getAppointment().getPatient().getUser().getFullName())
                .position(entry.getPosition())
                .status(entry.getStatus())
                .skipCount(entry.getSkipCount())
                .build();
    }
}