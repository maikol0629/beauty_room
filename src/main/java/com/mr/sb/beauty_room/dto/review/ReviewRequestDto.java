package com.mr.sb.beauty_room.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDto {
    @NotNull
    private Long clientId;

    @NotNull
    private Long stylistId;

    private Long appointmentId;

    @Min(1)
    @Max(5)
    private int rating;

    private String comment;
}
