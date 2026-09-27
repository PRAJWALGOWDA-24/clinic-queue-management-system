package com.clinicqueue.pharmacy;

import com.clinicqueue.common.dto.ApiResponse;
import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.pharmacy.dto.PharmacyOrderResponse;
import com.clinicqueue.pharmacy.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy-orders")
@RequiredArgsConstructor
public class PharmacyOrderController {

    private final PharmacyOrderRepository pharmacyOrderRepository;

    @GetMapping("/pharmacy/{pharmacyId}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ApiResponse<List<PharmacyOrderResponse>> getOrdersForPharmacy(@PathVariable Long pharmacyId) {
        List<PharmacyOrderResponse> orders = pharmacyOrderRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success("Orders fetched", orders);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ApiResponse<PharmacyOrderResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        PharmacyOrder order = pharmacyOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(request.getStatus());
        pharmacyOrderRepository.save(order);
        return ApiResponse.success("Status updated", toResponse(order));
    }

    private PharmacyOrderResponse toResponse(PharmacyOrder order) {
        return PharmacyOrderResponse.builder()
                .id(order.getId())
                .pharmacyName(order.getPharmacy().getName())
                .status(order.getStatus().name())
                .build();
    }
}