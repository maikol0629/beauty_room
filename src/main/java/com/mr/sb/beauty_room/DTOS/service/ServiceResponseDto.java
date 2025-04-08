package com.mr.sb.beauty_room.DTOS.service;

import com.mr.sb.beauty_room.entities.Stylist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceResponseDto {

    long idService;
    private String name;
    private String description;
    private float price;
    private int duration;
    private long id_stylist;
}
