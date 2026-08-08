package com.mr.sb.beauty_room.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReportDto {
    private String period;
    private long totalAppointments;
    private long completed;
    private long cancelled;
    private long pending;
    private long confirmed;
}
