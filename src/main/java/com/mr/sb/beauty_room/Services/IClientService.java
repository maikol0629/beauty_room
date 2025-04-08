package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.client.ClientResponseDto;
import com.mr.sb.beauty_room.DTOS.client.ClientSaveDto;
import com.mr.sb.beauty_room.entities.Client;

import java.util.List;
import java.util.Optional;

public interface IClientService {

    List<ClientResponseDto> findAll();
    ClientResponseDto findById(long id);
    boolean save(ClientSaveDto clientSaveDto);
    boolean deleteById(long id);
    boolean update (ClientSaveDto clientSaveDto, long id);
}
