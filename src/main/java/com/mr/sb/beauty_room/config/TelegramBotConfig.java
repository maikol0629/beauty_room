package com.mr.sb.beauty_room.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.webhook.starter.SpringTelegramWebhookBot;
import com.mr.sb.beauty_room.services.implement.TelegramBotService;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram.bot", name = "token")
public class TelegramBotConfig {

    @Bean
    public TelegramClient telegramClient(TelegramBotProperties properties) {
        return new OkHttpTelegramClient(properties.getToken());
    }

    @Bean
    public SpringTelegramWebhookBot springTelegramWebhookBot(TelegramBotService telegramBotService) {
        return SpringTelegramWebhookBot.builder()
                .botPath(telegramBotService.getBotPath())
                .updateHandler(telegramBotService::onWebhookUpdate)
                .setWebhook(telegramBotService::registerWebhook)
                .deleteWebhook(() -> {})
                .build();
    }
}
