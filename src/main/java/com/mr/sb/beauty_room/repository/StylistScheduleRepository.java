package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.StylistSchedule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface StylistScheduleRepository extends CrudRepository<StylistSchedule, Long> {
    List<StylistSchedule> findByStylistId(Long id);
    List<StylistSchedule> findByStylistIdAndDay(Long stylistId, DayOfWeek day);
}
