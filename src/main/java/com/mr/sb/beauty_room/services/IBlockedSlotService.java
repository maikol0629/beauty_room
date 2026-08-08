package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.blockedslot.BlockedSlotRequestDto;
import com.mr.sb.beauty_room.dto.blockedslot.BlockedSlotResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface IBlockedSlotService {
    BlockedSlotResponseDto create(BlockedSlotRequestDto request);
    List<BlockedSlotResponseDto> findByStylistId(Long stylistId);
    boolean delete(Long id);
    boolean isSlotBlocked(Long tenantId, Long stylistId, LocalDateTime start, LocalDateTime end);
}
