// prescription/PrescriptionRepository.java
package com.clinicqueue.prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByAppointmentId(Long appointmentId);
}