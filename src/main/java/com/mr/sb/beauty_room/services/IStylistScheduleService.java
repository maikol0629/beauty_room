package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.entities.StylistSchedule;

import java.util.List;
import java.util.Optional;

public interface IStylistScheduleService {

    boolean saveStylistSchedule(StylistSchedule stylistSchedule);
    List<StylistSchedule> findScheduleByStylistId(long stylistId);
    Optional<StylistSchedule> findScheduleById(long scheduleId);
    boolean updateStylistSchedule(Long scheduleId, StylistSchedule schedule);
    boolean deleteStylistSchedule(Long scheduleId);
}
