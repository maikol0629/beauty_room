package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Notification;
import com.mr.sb.beauty_room.entities.NotificationStatus;
import com.mr.sb.beauty_room.entities.NotificationType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends CrudRepository<Notification, Long> {
    List<Notification> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId);
    List<Notification> findByStatusAndTenantId(NotificationStatus status, Long tenantId);
    boolean existsByAppointmentIdAndType(Long appointmentId, NotificationType type);
    boolean existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(Long userId, NotificationType type, LocalDateTime createdAt);
}
