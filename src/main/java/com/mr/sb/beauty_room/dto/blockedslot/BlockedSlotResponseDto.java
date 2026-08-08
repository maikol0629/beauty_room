package com.mr.sb.beauty_room.dto.blockedslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedSlotResponseDto {
    private Long id;
    private Long stylistId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reason;
}
