package com.clinicqueue.queue;

import com.clinicqueue.common.dto.ApiResponse;
import com.clinicqueue.queue.dto.CheckInRequest;
import com.clinicqueue.queue.dto.QueueEntryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/checkin")
    public ApiResponse<QueueEntryResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.success("Checked in successfully", queueService.checkIn(request));
    }

    @GetMapping("/doctor/{doctorId}")
    public ApiResponse<Page<QueueEntryResponse>> getQueue(@PathVariable Long doctorId, Pageable pageable) {
        return ApiResponse.success("Queue fetched", queueService.getQueueForDoctor(doctorId, pageable));
    }

    @PostMapping("/doctor/{doctorId}/call-next")
    public ApiResponse<QueueEntryResponse> callNext(@PathVariable Long doctorId) {
        return ApiResponse.success("Next patient called", queueService.callNext(doctorId));
    }

    @PostMapping("/{queueEntryId}/skip")
    public ApiResponse<QueueEntryResponse> skip(@PathVariable Long queueEntryId) {
        return ApiResponse.success("Patient skipped", queueService.skipPatient(queueEntryId));
    }
}