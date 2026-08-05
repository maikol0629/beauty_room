package com.mr.sb.beauty_room.DTOS.stylist_room;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistRoomRequestDto {
    @NotBlank
    private String name;

    @NotBlank
    private String address;
}
