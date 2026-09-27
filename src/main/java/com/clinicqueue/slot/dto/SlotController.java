package com.clinicqueue.slot.dto;

import com.clinicqueue.common.dto.ApiResponse;
import com.clinicqueue.slot.SlotService;
import com.clinicqueue.slot.dto.CreateSlotRequest;
import com.clinicqueue.slot.dto.SlotResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ApiResponse<SlotResponse> createSlot(@Valid @RequestBody CreateSlotRequest request) {
        return ApiResponse.success("Slot created", slotService.createSlot(request));
    }

    @GetMapping
    public ApiResponse<List<SlotResponse>> getSlots(
            @RequestParam Long doctorId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success("Slots fetched", slotService.getSlotsByDoctorAndDate(doctorId, date));
    }
}