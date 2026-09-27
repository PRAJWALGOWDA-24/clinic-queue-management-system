package com.clinicqueue.queue.dto;

import com.clinicqueue.queue.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QueueEntryResponse {
    private Long id;
    private String patientName;
    private Integer position;
    private QueueStatus status;
    private Integer skipCount;
}