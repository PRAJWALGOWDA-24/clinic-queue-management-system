package com.clinicqueue.queue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Page<QueueEntry> findByAppointment_Slot_Doctor_IdAndStatus(Long doctorId, QueueStatus status, Pageable pageable);

    Optional<QueueEntry> findByAppointmentId(Long appointmentId);
}