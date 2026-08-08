package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.blockedslot.BlockedSlotRequestDto;
import com.mr.sb.beauty_room.dto.blockedslot.BlockedSlotResponseDto;
import com.mr.sb.beauty_room.exceptions.TenantNotResolvedException;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.services.IBlockedSlotService;
import com.mr.sb.beauty_room.entities.BlockedSlot;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.BlockedSlotRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class BlockedSlotServiceImplement implements IBlockedSlotService {

    private final BlockedSlotRepository blockedSlotRepository;
    private final StylistRepository stylistRepository;

    @Override
    public BlockedSlotResponseDto create(BlockedSlotRequestDto request) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Stylist stylist = stylistRepository.findByIdAndTenantId(request.getStylistId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Estilista no encontrado"));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        BlockedSlot blockedSlot = BlockedSlot.builder()
                .stylist(stylist)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .tenant(Tenant.builder().id(tenantId).build())
                .build();

        return toDto(blockedSlotRepository.save(blockedSlot));
    }

    @Override
    public List<BlockedSlotResponseDto> findByStylistId(Long stylistId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return StreamSupport.stream(blockedSlotRepository.findByStylistIdAndTenantId(stylistId, tenantId).spliterator(), false)
                .map(this::toDto)
                .toList();
    }

    @Override
    public boolean delete(Long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        if (blockedSlotRepository.findById(id)
                .filter(b -> b.getTenant() != null && b.getTenant().getId().equals(tenantId))
                .isPresent()) {
            blockedSlotRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSlotBlocked(Long tenantId, Long stylistId, LocalDateTime start, LocalDateTime end) {
        List<BlockedSlot> blocked = blockedSlotRepository
                .findByStylistIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndTenantId(stylistId, end, start, tenantId);
        return !blocked.isEmpty();
    }

    private BlockedSlotResponseDto toDto(BlockedSlot blockedSlot) {
        return BlockedSlotResponseDto.builder()
                .id(blockedSlot.getId())
                .stylistId(blockedSlot.getStylist().getId())
                .startDate(blockedSlot.getStartDate())
                .endDate(blockedSlot.getEndDate())
                .reason(blockedSlot.getReason())
                .build();
    }
}
