package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Notification;
import com.mr.sb.beauty_room.entities.NotificationStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends CrudRepository<Notification, Long> {
    List<Notification> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId);
    List<Notification> findByStatusAndTenantId(NotificationStatus status, Long tenantId);
}
