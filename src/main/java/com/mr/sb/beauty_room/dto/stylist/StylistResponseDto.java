package com.mr.sb.beauty_room.dto.stylist;

import com.mr.sb.beauty_room.dto.service.SalonServiceResponseDto;
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
    private String telegramChatId;
    @Builder.Default
    private List<SalonServiceResponseDto> services = new ArrayList<>();
    private StylistRoom stylistRoom;



}
