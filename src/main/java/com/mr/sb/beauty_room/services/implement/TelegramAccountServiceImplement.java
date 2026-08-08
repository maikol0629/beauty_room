package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.services.ITelegramAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramAccountServiceImplement implements ITelegramAccountService {

    private final ClientRepository clientRepository;
    private final TenantRepository tenantRepository;
    private final StylistRepository stylistRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Long resolveTenant(TelegramMessage msg) {
        String text = msg.text();
        if (text != null && text.startsWith("/start")) {
            String payload = text.substring("/start".length()).trim();
            if (!payload.isEmpty()) {
                return tenantRepository.findByTenantKey(payload)
                        .map(Tenant::getId)
                        .orElse(null);
            }
        }
        Optional<Client> client = clientRepository.findByTelegramChatId(msg.chatId());
        if (client.isPresent() && client.get().getTenant() != null) {
            return client.get().getTenant().getId();
        }
        return stylistRepository.findByTelegramChatId(msg.chatId())
                .map(stylist -> (stylist.getTenant() != null) ? stylist.getTenant().getId() : null)
                .orElse(null);
    }

    @Override
    public Optional<Client> findClientByChat(TelegramMessage msg, Long tenantId) {
        return clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
    }

    @Override
    public Client ensureClient(TelegramMessage msg, Long tenantId) {
        return clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId)
                .orElseGet(() -> {
                    Client client = Client.builder()
                            .email("tg_" + msg.chatId() + "@bot.local")
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .nameClient(displayName(msg))
                            .phone(null)
                            .telegramChatId(msg.chatId())
                            .tenant(Tenant.builder().id(tenantId).build())
                            .build();
                    return clientRepository.save(client);
                });
    }

    @Override
    public Optional<Stylist> findStylistByChat(TelegramMessage msg, Long tenantId) {
        return stylistRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
    }

    @Override
    public Optional<Stylist> findStylistByIdAndTenant(Long stylistId, Long tenantId) {
        return stylistRepository.findByIdAndTenantId(stylistId, tenantId);
    }

    @Override
    public String displayName(TelegramMessage msg) {
        if (msg.firstName() != null && !msg.firstName().isBlank()) {
            return msg.firstName();
        }
        if (msg.username() != null && !msg.username().isBlank()) {
            return msg.username();
        }
        return "Cliente Telegram";
    }

    @Override
    public String safeName(TelegramMessage msg) {
        String name = displayName(msg);
        if (msg.username() != null && !msg.username().isBlank()) {
            return "@" + msg.username();
        }
        return name;
    }
}
