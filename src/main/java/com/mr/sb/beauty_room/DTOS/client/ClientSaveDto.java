package com.mr.sb.beauty_room.DTOS.client;

import com.mr.sb.beauty_room.entities.Appointment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientSaveDto {
    private String name;
    private String email;
    private String phone;
}
