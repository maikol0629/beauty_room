package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.client.ClientResponseDto;
import com.mr.sb.beauty_room.dto.client.ClientSaveDto;

import java.util.List;

public interface IClientService {

    List<ClientResponseDto> findAll();
    ClientResponseDto findById(long id);
    boolean save(ClientSaveDto clientSaveDto);
    boolean deleteById(long id);
    boolean update (ClientSaveDto clientSaveDto, long id);
}
