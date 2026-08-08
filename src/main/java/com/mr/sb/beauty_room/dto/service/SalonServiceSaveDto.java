package com.mr.sb.beauty_room.dto.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonServiceSaveDto {
    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @Min(5000)
    private float price;

    @Min(15)
    private long duration;

    @NotNull
    private Long stylistId;

}
