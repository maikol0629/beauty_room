package com.mr.sb.beauty_room.services;
import com.mr.sb.beauty_room.dto.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.dto.stylist.StylistSaveDto;

import java.util.List;

public interface IStylistService {

    List<StylistResponseDto> findAll();
    StylistResponseDto findById(long id);
    boolean save (StylistSaveDto stylistSaveDto);
    boolean deleteById(long id);
    boolean update (StylistSaveDto stylistSaveDto, long id);
    boolean isExistStylist(long id);

    /**
     * Devuelve el código de vinculación vigente de un estilista o null si no
     * tiene (o ya fue consumido).
     */
    String findVincularCode(long id);

    /**
     * Genera (o regenera) el código secreto del deep link de vinculación de un
     * estilista. Devuelve el nuevo código o null si el estilista no existe.
     */
    String regenerateVincularCode(long id);
}
