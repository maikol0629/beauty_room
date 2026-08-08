package com.mr.sb.beauty_room.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDto {
    private String period;
    private double totalRevenue;
    private long totalPayments;
}
