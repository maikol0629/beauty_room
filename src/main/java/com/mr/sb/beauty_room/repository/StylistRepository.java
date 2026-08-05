package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Stylist;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StylistRepository extends JpaRepository<Stylist, Long> {

    Optional<Stylist> findByEmail(String email);

    List<Stylist> findByTenantId(Long tenantId);

    Optional<Stylist> findByIdAndTenantId(Long id, Long tenantId);
}
