package com.mr.sb.beauty_room.DTOS.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceResponseDto {

    private long id;
    private String name;
    private String description;
    private float price;
    private int duration;
    private long stylistId;
}
