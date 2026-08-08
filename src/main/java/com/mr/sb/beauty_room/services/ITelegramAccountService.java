package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Stylist;

import java.util.Optional;

/**
 * Resolución de tenant y cuentas (clientes/estilistas) para el bot de Telegram.
 */
public interface ITelegramAccountService {

    Long resolveTenant(TelegramMessage msg);

    Optional<Client> findClientByChat(TelegramMessage msg, Long tenantId);

    Client ensureClient(TelegramMessage msg, Long tenantId);

    Optional<Stylist> findStylistByChat(TelegramMessage msg, Long tenantId);

    Optional<Stylist> findStylistByIdAndTenant(Long stylistId, Long tenantId);

    String displayName(TelegramMessage msg);

    String safeName(TelegramMessage msg);
}
