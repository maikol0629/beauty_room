package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.blocked_slot.BlockedSlotRequestDto;
import com.mr.sb.beauty_room.DTOS.blocked_slot.BlockedSlotResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface IBlockedSlotService {
    BlockedSlotResponseDto create(BlockedSlotRequestDto request);
    List<BlockedSlotResponseDto> findByStylistId(Long stylistId);
    boolean delete(Long id);
    boolean isSlotBlocked(Long tenantId, Long stylistId, LocalDateTime start, LocalDateTime end);
}
