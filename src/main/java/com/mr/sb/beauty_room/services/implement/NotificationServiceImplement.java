package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.notification.NotificationRequestDto;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.services.INotificationService;
import com.mr.sb.beauty_room.entities.Notification;
import com.mr.sb.beauty_room.entities.NotificationStatus;
import com.mr.sb.beauty_room.entities.NotificationType;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImplement implements INotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification sendNotification(NotificationRequestDto request) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .tenant(Tenant.builder().id(tenantId).build())
                .build();

        notificationRepository.save(notification);

        log.info("=== NOTIFICACION ({}) ===", request.getType());
        log.info("Para usuario: {}", request.getUserId());
        log.info("Asunto: {}", request.getSubject());
        log.info("Mensaje: {}", request.getMessage());
        log.info("=========================");

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);

        return notification;
    }

    @Override
    public void sendAppointmentConfirmation(Long userId, String clientName, String stylistName, String date) {
        NotificationRequestDto request = NotificationRequestDto.builder()
                .userId(userId)
                .type(NotificationType.EMAIL)
                .subject("Cita confirmada")
                .message(String.format(
                        "Hola %s, tu cita con %s para el %s ha sido confirmada.",
                        clientName, stylistName, date))
                .build();
        sendNotification(request);
    }

    @Override
    public void sendAppointmentCancellation(Long userId, String clientName, String date) {
        NotificationRequestDto request = NotificationRequestDto.builder()
                .userId(userId)
                .type(NotificationType.EMAIL)
                .subject("Cita cancelada")
                .message(String.format(
                        "Hola %s, tu cita del %s ha sido cancelada.",
                        clientName, date))
                .build();
        sendNotification(request);
    }

    @Override
    public void sendAppointmentReminder(Long userId, String stylistName, String date) {
        NotificationRequestDto request = NotificationRequestDto.builder()
                .userId(userId)
                .type(NotificationType.SMS)
                .subject("Recordatorio de cita")
                .message(String.format(
                        "Recordatorio: tienes una cita con %s el %s.",
                        stylistName, date))
                .build();
        sendNotification(request);
    }

    @Override
    public List<Notification> getNotificationsByUser(Long userId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId);
    }
}
