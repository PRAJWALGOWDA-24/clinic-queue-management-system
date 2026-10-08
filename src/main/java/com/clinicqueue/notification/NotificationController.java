package com.clinicqueue.notification;

import com.clinicqueue.common.dto.ApiResponse;
import com.clinicqueue.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ApiResponse<Page<NotificationResponse>> myNotifications(
            @AuthenticationPrincipal UserDetails userDetails, Pageable pageable) {
        return ApiResponse.success("Notifications fetched",
                notificationService.getMyNotifications(userDetails.getUsername(), pageable));
    }

    @GetMapping("/my/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Unread count",
                notificationService.countUnread(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        return ApiResponse.success("Marked as read",
                notificationService.markAsRead(userDetails.getUsername(), id));
    }
}