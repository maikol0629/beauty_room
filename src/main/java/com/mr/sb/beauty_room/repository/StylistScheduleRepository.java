package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.StylistSchedule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface StylistScheduleRepository extends CrudRepository<StylistSchedule, Long> {
    List<StylistSchedule> findByStylistIdAndTenantId(Long id, Long tenantId);
    List<StylistSchedule> findByStylistIdAndDayAndTenantId(Long stylistId, DayOfWeek day, Long tenantId);
    Optional<StylistSchedule> findByIdAndTenantId(Long id, Long tenantId);
}
