package com.mr.sb.beauty_room.Controllers;

import com.mr.sb.beauty_room.DTOS.report.AppointmentReportDto;
import com.mr.sb.beauty_room.DTOS.report.RevenueReportDto;
import com.mr.sb.beauty_room.DTOS.report.TopServiceDto;
import com.mr.sb.beauty_room.Services.IReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final IReportService reportService;

    @GetMapping("/appointments")
    public ResponseEntity<AppointmentReportDto> appointmentReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportService.getAppointmentReport(from, to));
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReportDto> revenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportService.getRevenueReport(from, to));
    }

    @GetMapping("/top-services")
    public ResponseEntity<List<TopServiceDto>> topServices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportService.getTopServices(from, to));
    }

    @GetMapping("/active-clients")
    public ResponseEntity<Long> activeClients() {
        return ResponseEntity.ok(reportService.getActiveClients());
    }

    @GetMapping("/average-rating")
    public ResponseEntity<Double> averageRating() {
        return ResponseEntity.ok(reportService.getAverageRating());
    }
}
