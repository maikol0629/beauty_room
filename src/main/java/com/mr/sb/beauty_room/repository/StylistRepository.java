package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Stylist;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StylistRepository extends JpaRepository<Stylist, Long> {

    Optional<Stylist> findByEmail(String email);

    List<Stylist> findByTenantId(Long tenantId);

    Optional<Stylist> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT s FROM Stylist s WHERE s.telegramChatId = :telegramChatId")
    Optional<Stylist> findByTelegramChatId(@Param("telegramChatId") String telegramChatId);

    @Query("SELECT s FROM Stylist s WHERE s.telegramChatId = :telegramChatId AND s.tenant.id = :tenantId")
    Optional<Stylist> findByTelegramChatIdAndTenantId(@Param("telegramChatId") String telegramChatId, @Param("tenantId") Long tenantId);
}
