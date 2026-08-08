package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.report.AppointmentReportDto;
import com.mr.sb.beauty_room.dto.report.RevenueReportDto;
import com.mr.sb.beauty_room.dto.report.TopServiceDto;

import java.time.LocalDateTime;
import java.util.List;

public interface IReportService {
    AppointmentReportDto getAppointmentReport(LocalDateTime from, LocalDateTime to);
    RevenueReportDto getRevenueReport(LocalDateTime from, LocalDateTime to);
    List<TopServiceDto> getTopServices(LocalDateTime from, LocalDateTime to);
    long getActiveClients();
    double getAverageRating();
}
