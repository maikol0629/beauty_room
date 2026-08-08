package com.mr.sb.beauty_room.controllers;

import com.mr.sb.beauty_room.dto.notification.NotificationRequestDto;
import com.mr.sb.beauty_room.services.INotificationService;
import com.mr.sb.beauty_room.entities.Notification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notification> send(@Valid @RequestBody NotificationRequestDto request) {
        return ResponseEntity.ok(notificationService.sendNotification(request));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'STYLIST', 'ADMIN')")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
    }
}
