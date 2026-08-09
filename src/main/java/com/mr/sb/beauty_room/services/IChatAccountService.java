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

    /**
     * Indica si el tenant está usable (ACTIVE y trial no vencido). Usado por el
     * bot para bloquear mensajes de salones suspendidos/vencidos.
     */
    boolean isTenantUsable(Long tenantId);

    Optional<Client> findClientByChat(ChannelMessage msg, Long tenantId);

    Client ensureClient(ChannelMessage msg, Long tenantId);

    Optional<Stylist> findStylistByChat(ChannelMessage msg, Long tenantId);

    Optional<Stylist> findStylistByIdAndTenant(Long stylistId, Long tenantId);

    /**
     * Vincula el chat del mensaje a un estilista usando el código secreto del
     * deep link (start=vincular-<código>). Guarda el chatId (Telegram o
     * WhatsApp según el canal) y consume el código para que no sea reutilizable.
     */
    Optional<Stylist> linkStylistByCode(ChannelMessage msg, String code);

    String displayName(ChannelMessage msg);

    String safeName(ChannelMessage msg);
}
