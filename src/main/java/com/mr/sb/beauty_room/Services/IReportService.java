package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.report.AppointmentReportDto;
import com.mr.sb.beauty_room.DTOS.report.RevenueReportDto;
import com.mr.sb.beauty_room.DTOS.report.TopServiceDto;

import java.time.LocalDateTime;
import java.util.List;

public interface IReportService {
    AppointmentReportDto getAppointmentReport(LocalDateTime from, LocalDateTime to);
    RevenueReportDto getRevenueReport(LocalDateTime from, LocalDateTime to);
    List<TopServiceDto> getTopServices(LocalDateTime from, LocalDateTime to);
    long getActiveClients();
    double getAverageRating();
}
