package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.Services.IStylistScheduleService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.StylistSchedule;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.StylistScheduleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
public class StylistSheduleServiceImplement implements IStylistScheduleService {

    private final StylistScheduleRepository stylistScheduleRepository;
    private final StylistRepository stylistRepository;


@Autowired
 public StylistSheduleServiceImplement(StylistScheduleRepository stylistScheduleRepository, StylistRepository stylistRepository) {
        this.stylistScheduleRepository = stylistScheduleRepository;
        this.stylistRepository = stylistRepository;
 }

    @Override
    public List<StylistSchedule> findScheduleByStylistId(long stylistId) {
        return stylistScheduleRepository.findByStylistId(stylistId);
    }
    @Transactional
    @Override
    public boolean saveStylistSchedule(StylistSchedule schedule) {
        validateTime(schedule.getStartTime(), schedule.getEndTime());
        validateOverlap(schedule);

        Stylist stylist = stylistRepository.findById(schedule.getStylist().getId())
                .orElseThrow(() -> new IllegalArgumentException("Stylist no encontrado"));

        schedule.setStylist(stylist);
        stylistScheduleRepository.save(schedule);
        return true;
    }

    private void validateTime(LocalTime startTime, LocalTime endTime) {
        if (startTime.isBefore(LocalTime.of(7, 0)) || endTime.isAfter(LocalTime.of(22, 0))) {
            throw new IllegalArgumentException("El horario debe estar entre 7:00 AM y 10:00 PM");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("La hora de inicio debe ser antes de la hora de fin");
        }
    }

    private void validateOverlap(StylistSchedule schedule) {
        List<StylistSchedule> existingSchedules = stylistScheduleRepository.findByStylistIdAndDay(
                schedule.getStylist().getId(), schedule.getDay());

        boolean overlaps = existingSchedules.stream().anyMatch(existing ->
                schedule.getStartTime().isBefore(existing.getEndTime()) &&
                        schedule.getEndTime().isAfter(existing.getStartTime()));

        if (overlaps) {
            throw new IllegalArgumentException("El horario se solapa con otro existente para este estilista");
        }
    }
}
