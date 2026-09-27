package com.clinicqueue.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    @Query("SELECT s FROM Slot s " +  //to solve N+1 query problem
            "JOIN FETCH s.doctor d " +
            "JOIN FETCH d.user " +
            "WHERE s.doctor.id = :doctorId AND s.date = :date")
    List<Slot> findByDoctorIdAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);
}

/*
package com.clinicqueue.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByDoctorIdAndDate(Long doctorId, LocalDate date);
}*/