package com.mr.sb.beauty_room.DTOS.stylist;

import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.StylistRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StylistResponseDto {

    private long id;
    private String name;
    private String email;
    private String phone;
    private List<ServiceResponseDto> services = new ArrayList<>();
    private StylistRoom stylistRoom;



}
