package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.SalonService;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalonServiceRepository extends CrudRepository<SalonService, Long> {

    @Query("SELECT s FROM SalonService s WHERE s.stylist.id = :id AND s.tenant.id = :tenantId")
    List<SalonService> findByStylistIdAndTenantId(@Param("id") long id, @Param("tenantId") Long tenantId);

    List<SalonService> findByTenantId(Long tenantId);

    Optional<SalonService> findByIdAndTenantId(Long id, Long tenantId);

}
