package com.mr.sb.beauty_room.dto.stylistroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistRoomResponseDto {
    private long id;
    private String name;
    private String address;
}
