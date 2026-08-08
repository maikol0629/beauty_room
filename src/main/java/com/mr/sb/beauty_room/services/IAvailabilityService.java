package com.mr.sb.beauty_room.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IAvailabilityService {

    List<LocalTime> getAvailableSlots(Long stylistId, Long serviceId, LocalDate date);
}
