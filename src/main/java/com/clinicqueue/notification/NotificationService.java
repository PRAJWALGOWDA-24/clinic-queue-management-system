package com.clinicqueue.notification;

import com.clinicqueue.common.exception.ResourceNotFoundException;
import com.clinicqueue.notification.dto.NotificationResponse;
import com.clinicqueue.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // called from other services; userId = users.id (NOT patientId)
    public void notifyUser(Long userId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }

    public Page<NotificationResponse> getMyNotifications(String email, Pageable pageable) {
        Long userId = findUserId(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    public long countUnread(String email) {
        return notificationRepository.countUnread(findUserId(email));
    }

    @Transactional
    public NotificationResponse markAsRead(String email, Long notificationId) {
        Long userId = findUserId(email);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // ownership check: you can only mark YOUR OWN notifications
        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    private Long findUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}