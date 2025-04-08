package com.mr.sb.beauty_room.DTOS.stylist;

import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.StylistRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistSaveDto {

    private String name;
    private String email;
    private String phone;
    private long id_stylist_room;


    public boolean isNotEmpty(){

        return !this.name.isEmpty() && !this.email.isEmpty()
                && !this.phone.isEmpty() && this.phone.length()>10;
    }


}
