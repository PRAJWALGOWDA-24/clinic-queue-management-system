// pharmacy/PharmacyController.java
package com.clinicqueue.pharmacy;

import com.clinicqueue.common.dto.ApiResponse;
import com.clinicqueue.pharmacy.dto.CreatePharmacyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacies")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyRepository pharmacyRepository;

    @PostMapping
    public ApiResponse<Pharmacy> create(@Valid @RequestBody CreatePharmacyRequest request) {
        Pharmacy pharmacy = Pharmacy.builder()
                .name(request.getName())
                .address(request.getAddress())
                .build();
        pharmacyRepository.save(pharmacy);
        return ApiResponse.success("Pharmacy created", pharmacy);
    }

    @GetMapping
    public ApiResponse<List<Pharmacy>> getAll() {
        return ApiResponse.success("Pharmacies fetched", pharmacyRepository.findAll());
    }
}