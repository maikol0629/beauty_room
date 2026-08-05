package com.mr.sb.beauty_room.DTOS.stylist;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistScheduleRequestDto {
    @NotNull
    private DayOfWeek day;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;
}
