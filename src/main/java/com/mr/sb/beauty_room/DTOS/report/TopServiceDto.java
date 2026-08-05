package com.mr.sb.beauty_room.DTOS.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopServiceDto {
    private String serviceName;
    private long totalBookings;
    private double totalRevenue;
}
