package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.StylistRoom;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StylistRoomRepository extends CrudRepository<StylistRoom, Long> {
    List<StylistRoom> findByTenantId(Long tenantId);
    Optional<StylistRoom> findByIdAndTenantId(Long id, Long tenantId);
}
