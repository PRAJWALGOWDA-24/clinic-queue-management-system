package com.clinicqueue.pharmacy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PharmacyOrderRepository extends JpaRepository<PharmacyOrder, Long> {
    Optional<PharmacyOrder> findByPrescriptionId(Long prescriptionId);
    List<PharmacyOrder> findByPharmacyId(Long pharmacyId);
}