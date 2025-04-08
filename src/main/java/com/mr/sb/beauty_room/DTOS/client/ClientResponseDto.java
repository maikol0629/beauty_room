package com.mr.sb.beauty_room.DTOS.client;

import com.mr.sb.beauty_room.entities.Appointment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponseDto {

    private long id;
    private String name;
    private String email;
    private String phone;
    private List<Appointment> appointments = new ArrayList<>();

}
