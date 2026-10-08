package com.clinicqueue.queue;

import com.clinicqueue.appointment.Appointment;
import com.clinicqueue.appointment.AppointmentRepository;
import com.clinicqueue.appointment.AppointmentStatus;
import com.clinicqueue.auditlog.AuditLogService;
import com.clinicqueue.clinicsettings.ClinicSettings;
import com.clinicqueue.clinicsettings.ClinicSettingsRepository;
import com.clinicqueue.common.exception.BadRequestException;
import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.notification.NotificationService;
import com.clinicqueue.queue.dto.CheckInRequest;
import com.clinicqueue.queue.dto.QueueEntryResponse;
import com.clinicqueue.slot.Slot;
import com.clinicqueue.slot.SlotRepository;
import com.clinicqueue.slot.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final int SKIP_PLACES_BACK = 3;

    private final QueueEntryRepository queueEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicSettingsRepository clinicSettingsRepository;
    private final SlotRepository slotRepository;
    private final SlotService slotService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public QueueEntryResponse checkIn(CheckInRequest request) {

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Only a BOOKED appointment can enter the queue.
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BadRequestException("Only BOOKED appointments can be checked in");
        }

        // IDEMPOTENCY:
        // If the patient has already checked in for this appointment,
        // do not create another QueueEntry.
        //
        // Without this check:
        // First request  -> QueueEntry created
        // Second request -> another QueueEntry could be created
        //
        // This makes the check-in operation safe against duplicate requests.
        if (queueEntryRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new BadRequestException("Patient already checked in for this appointment");
        }

        Long doctorId = appointment.getSlot().getDoctor().getId();

        /*
         * How position gets calculated:
         *
         * We get all patients who are currently WAITING for this doctor,
         * already ordered by their position.
         *
         * Real-world example:
         *
         * Waiting room:
         * Patient A -> position 1
         * Patient B -> position 2
         * Patient C -> position 3
         *
         * New patient checks in.
         * The system sees the last position = 3,
         * so the new patient gets position = 4.
         *
         * We do not maintain a separate running counter.
         * We calculate the position from the current waiting list,
         * which keeps the queue synchronized with the actual WAITING patients.
         */
        List<QueueEntry> waiting = getWaitingList(doctorId);

        int position = waiting.isEmpty()
                ? 1
                : waiting.get(waiting.size() - 1).getPosition() + 1;

        QueueEntry entry = QueueEntry.builder()
                .appointment(appointment)
                .position(position)
                .status(QueueStatus.WAITING)
                .checkedInAt(LocalDateTime.now())
                .skipCount(0)
                .build();

        queueEntryRepository.save(entry);

        Long patientUserId = appointment.getPatient().getUser().getId();

        auditLogService.log(
                patientUserId,
                "CHECK_IN",
                "QueueEntry",
                entry.getId()
        );

        notificationService.notifyUser(
                patientUserId,
                "You are checked in. Your position in the queue is "
                        + entry.getPosition() + "."
        );

        return toResponse(entry);
    }

    public Page<QueueEntryResponse> getQueueForDoctor(
            Long doctorId,
            Pageable pageable
    ) {

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("position").ascending()
        );

        return queueEntryRepository
                .findByAppointment_Slot_Doctor_IdAndStatus(
                        doctorId,
                        QueueStatus.WAITING,
                        sorted
                )
                .map(this::toResponse);
    }

    @Transactional
    public QueueEntryResponse callNext(Long doctorId) {

        /*
         * Call Next — the doctor's buzzer.
         *
         * The doctor does not need to manually select a patient.
         * The system gets the current WAITING list and picks
         * whoever has the lowest position.
         *
         * Example:
         *
         * Patient A -> position 1
         * Patient B -> position 2
         * Patient C -> position 3
         *
         * Doctor presses "Call Next".
         * Patient A is called.
         *
         * Patient A is then changed from WAITING to CALLED.
         * The remaining WAITING patients move up:
         *
         * Patient B -> position 1
         * Patient C -> position 2
         */
        List<QueueEntry> waiting = getWaitingList(doctorId);

        if (waiting.isEmpty()) {
            throw new BadRequestException("No patients waiting in queue");
        }

        QueueEntry next = waiting.get(0);

        next.setStatus(QueueStatus.CALLED);
        queueEntryRepository.save(next);

        // Everyone still WAITING moves up one place.
        renumber(waiting.subList(1, waiting.size()));

        Long patientUserId = next.getAppointment().getPatient().getUser().getId();

        auditLogService.log(
                patientUserId,
                "CALL_NEXT",
                "QueueEntry",
                next.getId()
        );

        notificationService.notifyUser(
                patientUserId,
                "It is your turn. Please go to the doctor now."
        );

        return toResponse(next);
    }

    @Transactional
    public QueueEntryResponse skipPatient(Long queueEntryId) {

        QueueEntry entry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found"));

        /*
         * A patient can only be skipped if they are currently:
         *
         * WAITING -> patient is waiting in the queue
         * CALLED  -> doctor/receptionist called the patient but they did not respond
         *
         * A CANCELLED or already completed entry should not be skipped again.
         */
        if (entry.getStatus() != QueueStatus.WAITING
                && entry.getStatus() != QueueStatus.CALLED) {

            throw new BadRequestException(
                    "Only WAITING or CALLED patients can be skipped"
            );
        }

        Appointment appointment = entry.getAppointment();

        Long doctorId = appointment.getSlot().getDoctor().getId();

        Long patientUserId = appointment.getPatient().getUser().getId();

        /*
         * ClinicSettings contains the doctor's queue rules.
         *
         * For example:
         *
         * graceMinutes = how long the doctor/receptionist waits
         * maxSkips     = how many times the patient can be skipped
         *
         * Example:
         * maxSkips = 2
         *
         * First skip  -> patient gets another chance
         * Second skip -> appointment gets cancelled
         */
        ClinicSettings settings = clinicSettingsRepository.findAll().stream()
                .filter(s -> s.getDoctor().getId().equals(doctorId))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Clinic settings not configured for this doctor"
                        )
                );

        // Every time the patient is skipped, increase the skip counter.
        entry.setSkipCount(entry.getSkipCount() + 1);

        if (entry.getSkipCount() >= settings.getMaxSkips()) {

            /*
             * Too many skips:
             *
             * 1. Cancel the queue entry
             * 2. Cancel the appointment
             * 3. Free the slot
             * 4. Invalidate the Redis/cache entry
             * 5. Renumber the remaining queue
             *
             * Everything happens inside one @Transactional method,
             * so the database changes are treated as one transaction.
             */
            entry.setStatus(QueueStatus.CANCELLED);
            queueEntryRepository.save(entry);

            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);

            Slot slot = appointment.getSlot();

            slot.setBooked(false);
            slotRepository.save(slot);

            // The slot's cached data is now outdated,
            // so remove/invalidate it from the cache.
            slotService.invalidateCache(
                    doctorId,
                    slot.getDate()
            );

            // The cancelled patient is removed from the active queue,
            // so the remaining patients close the gap.
            renumber(getWaitingList(doctorId));

            auditLogService.log(
                    patientUserId,
                    "SKIP_PATIENT",
                    "QueueEntry",
                    entry.getId()
            );

            auditLogService.log(
                    patientUserId,
                    "CANCEL_APPOINTMENT",
                    "Appointment",
                    appointment.getId()
            );

            notificationService.notifyUser(
                    patientUserId,
                    "Your appointment was cancelled because you were skipped too many times."
            );

        } else {

            /*
             * Patient still has another chance.
             *
             * Example:
             *
             * Current queue:
             * A -> 1
             * B -> 2
             * C -> 3
             * D -> 4
             * E -> 5
             *
             * Suppose B is skipped.
             *
             * SKIP_PLACES_BACK = 3
             *
             * B is moved behind up to 3 other waiting patients.
             *
             * The queue is then renumbered:
             *
             * A -> 1
             * C -> 2
             * D -> 3
             * E -> 4
             * B -> 5
             *
             * The patient gets another chance later.
             */

            List<QueueEntry> others =
                    new ArrayList<>(getWaitingList(doctorId));

            // Remove the current patient before inserting them again.
            others.removeIf(e -> e.getId().equals(entry.getId()));

            // Put the patient SKIP_PLACES_BACK positions behind.
            int insertAt = Math.min(
                    SKIP_PLACES_BACK,
                    others.size()
            );

            entry.setStatus(QueueStatus.WAITING);

            others.add(insertAt, entry);

            // Recalculate positions:
            // 1, 2, 3, 4, 5...
            renumber(others);

            auditLogService.log(
                    patientUserId,
                    "SKIP_PATIENT",
                    "QueueEntry",
                    entry.getId()
            );

            notificationService.notifyUser(
                    patientUserId,
                    "You were skipped. Your new position is "
                            + entry.getPosition() + "."
            );
        }

        return toResponse(entry);
    }

    /*
     * Gets only the patients who are currently WAITING.
     *
     * The repository method already sorts them by position:
     *
     * position 1
     * position 2
     * position 3
     * ...
     */
    private List<QueueEntry> getWaitingList(Long doctorId) {

        return queueEntryRepository
                .findByAppointment_Slot_Doctor_IdAndStatusOrderByPositionAsc(
                        doctorId,
                        QueueStatus.WAITING
                );
    }

    /*
     * Renumbers the queue so positions are always:
     *
     * 1, 2, 3, 4, 5...
     *
     * Example:
     *
     * Before:
     * A -> 1
     * C -> 3
     * D -> 4
     *
     * After:
     * A -> 1
     * C -> 2
     * D -> 3
     *
     * This keeps the queue positions consistent after
     * someone is called, skipped, or cancelled.
     */
    private void renumber(List<QueueEntry> ordered) {

        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setPosition(i + 1);
        }

        queueEntryRepository.saveAll(ordered);
    }

    /*
     * Converts the database Entity into a Response DTO.
     *
     * We do not return QueueEntry directly to the client.
     * Instead, we expose only the fields the API needs.
     */
    private QueueEntryResponse toResponse(QueueEntry entry) {

        return QueueEntryResponse.builder()
                .id(entry.getId())
                .patientName(
                        entry.getAppointment()
                                .getPatient()
                                .getUser()
                                .getFullName()
                )
                .position(entry.getPosition())
                .status(entry.getStatus())
                .skipCount(entry.getSkipCount())
                .build();
    }
}