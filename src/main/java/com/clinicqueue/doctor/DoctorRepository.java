
package com.clinicqueue.doctor;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    boolean existsByUserId(Long id);
}