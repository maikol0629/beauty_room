package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantKey(String tenantKey);
}
