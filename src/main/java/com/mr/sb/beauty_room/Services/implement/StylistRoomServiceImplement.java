package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.stylist_room.StylistRoomResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist_room.StylistRoomRequestDto;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IStylistRoomService;
import com.mr.sb.beauty_room.entities.StylistRoom;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.StylistRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class StylistRoomServiceImplement implements IStylistRoomService {

    private final StylistRoomRepository stylistRoomRepository;

    @Override
    public List<StylistRoomResponseDto> findAll() {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return StreamSupport.stream(stylistRoomRepository.findByTenantId(tenantId).spliterator(), false)
                .map(this::toDto)
                .toList();
    }

    @Override
    public StylistRoomResponseDto findById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return stylistRoomRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public StylistRoomResponseDto save(StylistRoomRequestDto request) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        StylistRoom room = new StylistRoom();
        room.setName_room(request.getName());
        room.setAddress(request.getAddress());
        room.setTenant(Tenant.builder().id(tenantId).build());
        return toDto(stylistRoomRepository.save(room));
    }

    @Override
    public StylistRoomResponseDto update(long id, StylistRoomRequestDto request) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<StylistRoom> opt = stylistRoomRepository.findByIdAndTenantId(id, tenantId);
        if (opt.isEmpty()) {
            return null;
        }
        StylistRoom room = opt.get();
        room.setName_room(request.getName());
        room.setAddress(request.getAddress());
        return toDto(stylistRoomRepository.save(room));
    }

    @Override
    public boolean deleteById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        if (stylistRoomRepository.findByIdAndTenantId(id, tenantId).isPresent()) {
            stylistRoomRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private StylistRoomResponseDto toDto(StylistRoom room) {
        return StylistRoomResponseDto.builder()
                .id(room.getId())
                .name(room.getName_room())
                .address(room.getAddress())
                .build();
    }
}
