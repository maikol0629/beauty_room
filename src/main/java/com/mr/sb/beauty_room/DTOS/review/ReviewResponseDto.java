package com.mr.sb.beauty_room.DTOS.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {
    private Long id;
    private Long clientId;
    private String clientName;
    private Long stylistId;
    private String stylistName;
    private Long appointmentId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
