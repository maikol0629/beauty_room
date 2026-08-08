package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    List<Client> findByTenantId(Long tenantId);

    Optional<Client> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT c FROM Client c WHERE c.telegramChatId = :telegramChatId")
    Optional<Client> findByTelegramChatId(@Param("telegramChatId") String telegramChatId);

    @Query("SELECT c FROM Client c WHERE c.telegramChatId = :telegramChatId AND c.tenant.id = :tenantId")
    Optional<Client> findByTelegramChatIdAndTenantId(@Param("telegramChatId") String telegramChatId, @Param("tenantId") Long tenantId);
}
