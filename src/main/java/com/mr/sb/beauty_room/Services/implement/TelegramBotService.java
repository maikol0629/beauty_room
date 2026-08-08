package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.Config.TelegramBotProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final TelegramBotProperties properties;
    private final TelegramUpdateHandler telegramUpdateHandler;
    private final ObjectProvider<TelegramClient> telegramClientProvider;

    public String getBotUsername() {
        return properties.getUsername();
    }

    public String getBotToken() {
        return properties.getToken();
    }

    public String getBotPath() {
        return properties.getPath();
    }

    public BotApiMethod<?> onWebhookUpdate(Update update) {
        return telegramUpdateHandler.handle(update);
    }

    public void registerWebhook() {
        String webhookUrl = properties.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("telegram.bot.webhook-url vacío: se omite setWebhook. Usa ngrok o una URL HTTPS pública (ej: https://host/api/telegram/webhook).");
            return;
        }
        TelegramClient telegramClient = telegramClientProvider.getIfAvailable();
        if (telegramClient == null) {
            log.warn("Telegram client no configurado: no se puede registrar el webhook.");
            return;
        }
        try {
            telegramClient.execute(new SetWebhook(webhookUrl));
            log.info("Webhook registrado en Telegram: {}", webhookUrl);
        } catch (Exception e) {
            log.error("Error registrando webhook en Telegram ({}): {}", webhookUrl, e.getMessage(), e);
        }
    }
}
