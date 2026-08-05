package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IStylistScheduleService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.StylistSchedule;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.StylistScheduleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class StylistSheduleServiceImplement implements IStylistScheduleService {

    private final StylistScheduleRepository stylistScheduleRepository;
    private final StylistRepository stylistRepository;



 public StylistSheduleServiceImplement(StylistScheduleRepository stylistScheduleRepository, StylistRepository stylistRepository) {
        this.stylistScheduleRepository = stylistScheduleRepository;
        this.stylistRepository = stylistRepository;
 }

    @Override
    public List<StylistSchedule> findScheduleByStylistId(long stylistId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return stylistScheduleRepository.findByStylistIdAndTenantId(stylistId, tenantId);
    }

    @Override
    public Optional<StylistSchedule> findScheduleById(long scheduleId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return stylistScheduleRepository.findByIdAndTenantId(scheduleId, tenantId);
    }

    @Transactional
    @Override
    public boolean saveStylistSchedule(StylistSchedule schedule) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        validateTime(schedule.getStartTime(), schedule.getEndTime());
        validateOverlap(schedule, tenantId);

        Stylist stylist = stylistRepository.findByIdAndTenantId(schedule.getStylist().getId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Stylist no encontrado"));

        schedule.setStylist(stylist);
        schedule.setTenant(Tenant.builder().id(tenantId).build());
        stylistScheduleRepository.save(schedule);
        return true;
    }

    @Transactional
    @Override
    public boolean updateStylistSchedule(Long scheduleId, StylistSchedule updated) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<StylistSchedule> existing = stylistScheduleRepository.findByIdAndTenantId(scheduleId, tenantId);
        if (existing.isEmpty()) {
            return false;
        }
        StylistSchedule schedule = existing.get();
        schedule.setDay(updated.getDay());
        schedule.setStartTime(updated.getStartTime());
        schedule.setEndTime(updated.getEndTime());
        validateTime(schedule.getStartTime(), schedule.getEndTime());
        validateOverlap(schedule, tenantId);
        stylistScheduleRepository.save(schedule);
        return true;
    }

    @Transactional
    @Override
    public boolean deleteStylistSchedule(Long scheduleId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        if (stylistScheduleRepository.findByIdAndTenantId(scheduleId, tenantId).isPresent()) {
            stylistScheduleRepository.deleteById(scheduleId);
            return true;
        }
        return false;
    }

    private void validateTime(LocalTime startTime, LocalTime endTime) {
        if (startTime.isBefore(LocalTime.of(7, 0)) || endTime.isAfter(LocalTime.of(22, 0))) {
            throw new IllegalArgumentException("El horario debe estar entre 7:00 AM y 10:00 PM");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("La hora de inicio debe ser antes de la hora de fin");
        }
    }

    private void validateOverlap(StylistSchedule schedule, Long tenantId) {
        List<StylistSchedule> existingSchedules = stylistScheduleRepository.findByStylistIdAndDayAndTenantId(
                schedule.getStylist().getId(), schedule.getDay(), tenantId);

        boolean overlaps = existingSchedules.stream().anyMatch(existing ->
                !existing.getId().equals(schedule.getId()) &&
                schedule.getStartTime().isBefore(existing.getEndTime()) &&
                        schedule.getEndTime().isAfter(existing.getStartTime()));

        if (overlaps) {
            throw new IllegalArgumentException("El horario se solapa con otro existente para este estilista");
        }
    }
}
