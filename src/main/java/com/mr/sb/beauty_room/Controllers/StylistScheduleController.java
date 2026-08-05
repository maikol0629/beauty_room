package com.mr.sb.beauty_room.Controllers;

import com.mr.sb.beauty_room.DTOS.stylist.StylistScheduleRequestDto;
import com.mr.sb.beauty_room.Services.IStylistScheduleService;
import com.mr.sb.beauty_room.Services.IStylistService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.StylistSchedule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stylist/{stylistId}/schedule")
@RequiredArgsConstructor
public class StylistScheduleController {

    private final IStylistScheduleService scheduleService;
    private final IStylistService stylistService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<List<StylistSchedule>> getSchedules(@PathVariable long stylistId) {
        if (!stylistService.isExistStylist(stylistId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(scheduleService.findScheduleByStylistId(stylistId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<?> createSchedule(
            @PathVariable long stylistId,
            @Valid @RequestBody StylistScheduleRequestDto request) {
        if (!stylistService.isExistStylist(stylistId)) {
            return ResponseEntity.notFound().build();
        }
        StylistSchedule schedule = StylistSchedule.builder()
                .stylist(Stylist.builder().id(stylistId).build())
                .day(request.getDay())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        scheduleService.saveStylistSchedule(schedule);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<?> updateSchedule(
            @PathVariable long stylistId,
            @PathVariable long scheduleId,
            @Valid @RequestBody StylistScheduleRequestDto request) {
        if (!stylistService.isExistStylist(stylistId)) {
            return ResponseEntity.notFound().build();
        }
        StylistSchedule schedule = StylistSchedule.builder()
                .stylist(Stylist.builder().id(stylistId).build())
                .day(request.getDay())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        if (scheduleService.updateStylistSchedule(scheduleId, schedule)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<?> deleteSchedule(
            @PathVariable long stylistId,
            @PathVariable long scheduleId) {
        if (!stylistService.isExistStylist(stylistId)) {
            return ResponseEntity.notFound().build();
        }
        if (scheduleService.deleteStylistSchedule(scheduleId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
