package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.entities.StylistSchedule;

import java.util.List;

public interface IStylistScheduleService {

    boolean saveStylistSchedule(StylistSchedule stylistSchedule);
    List<StylistSchedule> findScheduleByStylistId(long stylistId);
}
