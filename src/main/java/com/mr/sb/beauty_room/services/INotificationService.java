package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.notification.NotificationRequestDto;
import com.mr.sb.beauty_room.entities.Notification;

import java.util.List;

public interface INotificationService {
    Notification sendNotification(NotificationRequestDto request);
    void sendAppointmentConfirmation(Long userId, String clientName, String stylistName, String date);
    void sendAppointmentCancellation(Long userId, String clientName, String date);
    void sendAppointmentReminder(Long userId, String stylistName, String date);
    List<Notification> getNotificationsByUser(Long userId);
}
