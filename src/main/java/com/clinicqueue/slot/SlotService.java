package com.clinicqueue.slot;

import com.clinicqueue.common.exception.BadRequestException;
import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.doctor.Doctor;
import com.clinicqueue.doctor.DoctorRepository;
import com.clinicqueue.slot.dto.CreateSlotRequest;
import com.clinicqueue.slot.dto.SlotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final SlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SlotResponse createSlot(CreateSlotRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + request.getDoctorId()));

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        Slot slot = Slot.builder()
                .doctor(doctor)
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isBooked(false)
                .build();

        slotRepository.save(slot);

        // a new slot changes the list for this doctor/date — clear the stale cache
        invalidateCache(request.getDoctorId(), request.getDate());

        return toResponse(slot);
    }

    @SuppressWarnings("unchecked")
    public List<SlotResponse> getSlotsByDoctorAndDate(Long doctorId, LocalDate date) {

        String cacheKey = buildCacheKey(doctorId, date);

        // 1. try the cache first
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (List<SlotResponse>) cached;
        }

        // 2. cache miss — go to MySQL
        List<SlotResponse> slots = slotRepository.findByDoctorIdAndDate(doctorId, date)
                .stream()
                .map(this::toResponse)
                .toList();

        // 3. save to cache for next time, expire in 2 minutes
        redisTemplate.opsForValue().set(cacheKey, slots, 2, TimeUnit.MINUTES);

        return slots;
    }

    public void invalidateCache(Long doctorId, LocalDate date) {
        redisTemplate.delete(buildCacheKey(doctorId, date));
    }

    private String buildCacheKey(Long doctorId, LocalDate date) {
        return "doctor:slots:" + doctorId + ":" + date;
    }

    private SlotResponse toResponse(Slot slot) {
        return SlotResponse.builder()
                .id(slot.getId())
                .doctorId(slot.getDoctor().getId())
                .doctorName(slot.getDoctor().getUser().getFullName())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .isBooked(slot.isBooked())
                .build();
    }
}