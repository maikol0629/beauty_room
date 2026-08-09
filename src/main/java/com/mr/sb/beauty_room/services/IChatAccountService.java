package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Stylist;

import java.util.Optional;

/**
 * Resolución de tenant y cuentas (clientes/estilistas) para el bot, por canal
 * (Telegram o WhatsApp).
 */
public interface IChatAccountService {

    Long resolveTenant(ChannelMessage msg);

    Optional<Client> findClientByChat(ChannelMessage msg, Long tenantId);

    Client ensureClient(ChannelMessage msg, Long tenantId);

    Optional<Stylist> findStylistByChat(ChannelMessage msg, Long tenantId);

    Optional<Stylist> findStylistByIdAndTenant(Long stylistId, Long tenantId);

    String displayName(ChannelMessage msg);

    String safeName(ChannelMessage msg);
}
