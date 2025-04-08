package com.mr.sb.beauty_room.DTOS.service;

import com.mr.sb.beauty_room.entities.Stylist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSaveDto {

    private String name;
    private String description;
    private float price;
    private long duration;
    private long id_stylist;



}
