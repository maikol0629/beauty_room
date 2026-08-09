package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.messaging.Channel;
import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.services.IChatAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación de IChatAccountService: resuelve tenant por deep link y
 * crea/recupera clientes y estilistas por chat id (Telegram o WhatsApp).
 */
@Service
@RequiredArgsConstructor
public class ChatAccountServiceImplement implements IChatAccountService {

    private final ClientRepository clientRepository;
    private final TenantRepository tenantRepository;
    private final StylistRepository stylistRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Long resolveTenant(ChannelMessage msg) {
        String text = msg.text();
        if (text != null) {
            String trimmed = text.trim();
            if (trimmed.startsWith("/start")) {
                String payload = trimmed.substring("/start".length()).trim();
                if (!payload.isEmpty()) {
                    Long tenantId = findByKey(payload);
                    if (tenantId != null) {
                        return tenantId;
                    }
                }
            } else if (msg.channel() == Channel.WHATSAPP && !trimmed.startsWith("/")) {
                // Deep link de WhatsApp: wa.me/<num>?text=<tenantKey> llega como texto plano.
                Long tenantId = findByKey(trimmed);
                if (tenantId != null) {
                    return tenantId;
                }
            }
        }
        Optional<Client> client = findClientByChatId(msg.chatId());
        if (client.isPresent() && client.get().getTenant() != null) {
            return client.get().getTenant().getId();
        }
        return findStylistByChatId(msg.chatId())
                .map(stylist -> (stylist.getTenant() != null) ? stylist.getTenant().getId() : null)
                .orElse(null);
    }

    private Long findByKey(String key) {
        return tenantRepository.findByTenantKey(key)
                .map(Tenant::getId)
                .orElse(null);
    }

    @Override
    public Optional<Client> findClientByChat(ChannelMessage msg, Long tenantId) {
        return msg.channel() == Channel.WHATSAPP
                ? clientRepository.findByWhatsappChatIdAndTenantId(msg.chatId(), tenantId)
                : clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
    }

    @Override
    public Client ensureClient(ChannelMessage msg, Long tenantId) {
        Optional<Client> existing = msg.channel() == Channel.WHATSAPP
                ? clientRepository.findByWhatsappChatIdAndTenantId(msg.chatId(), tenantId)
                : clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
        return existing.orElseGet(() -> {
            boolean whatsapp = msg.channel() == Channel.WHATSAPP;
            Client client = Client.builder()
                    .email((whatsapp ? "wa_" : "tg_") + msg.chatId() + "@bot.local")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .nameClient(displayName(msg))
                    .phone(null)
                    .telegramChatId(whatsapp ? null : msg.chatId())
                    .whatsappChatId(whatsapp ? msg.chatId() : null)
                    .tenant(Tenant.builder().id(tenantId).build())
                    .build();
            return clientRepository.save(client);
        });
    }

    @Override
    public Optional<Stylist> findStylistByChat(ChannelMessage msg, Long tenantId) {
        return msg.channel() == Channel.WHATSAPP
                ? stylistRepository.findByWhatsappChatIdAndTenantId(msg.chatId(), tenantId)
                : stylistRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
    }

    @Override
    public Optional<Stylist> findStylistByIdAndTenant(Long stylistId, Long tenantId) {
        return stylistRepository.findByIdAndTenantId(stylistId, tenantId);
    }

    @Override
    public String displayName(ChannelMessage msg) {
        if (msg.firstName() != null && !msg.firstName().isBlank()) {
            return msg.firstName();
        }
        if (msg.username() != null && !msg.username().isBlank()) {
            return msg.username();
        }
        return msg.channel() == Channel.WHATSAPP ? "Cliente WhatsApp" : "Cliente Telegram";
    }

    @Override
    public String safeName(ChannelMessage msg) {
        String name = displayName(msg);
        if (msg.username() != null && !msg.username().isBlank()) {
            return "@" + msg.username();
        }
        return name;
    }

    private Optional<Client> findClientByChatId(String chatId) {
        return clientRepository.findByTelegramChatId(chatId)
                .or(() -> clientRepository.findByWhatsappChatId(chatId));
    }

    private Optional<Stylist> findStylistByChatId(String chatId) {
        return stylistRepository.findByTelegramChatId(chatId)
                .or(() -> stylistRepository.findByWhatsappChatId(chatId));
    }
}
