package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.BlockedSlot;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BlockedSlotRepository extends CrudRepository<BlockedSlot, Long> {
    List<BlockedSlot> findByStylistIdAndTenantId(Long stylistId, Long tenantId);
    List<BlockedSlot> findByStylistIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndTenantId(
            Long stylistId, LocalDateTime endDate, LocalDateTime startDate, Long tenantId);
}
