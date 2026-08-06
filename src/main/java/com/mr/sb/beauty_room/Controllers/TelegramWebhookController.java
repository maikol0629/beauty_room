package com.mr.sb.beauty_room.Controllers;

import com.mr.sb.beauty_room.Services.implement.TelegramBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Tag(name = "Telegram", description = "Webhook del bot de Telegram")
public class TelegramWebhookController {

    private final TelegramBotService telegramBotService;

    @PostMapping(value = "/webhook", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recibe updates de Telegram", description = "Telegram hace POST acá con cada update. Debe ser una URL pública HTTPS.")
    public BotApiMethod<?> webhook(@RequestBody Update update) {
        return telegramBotService.onWebhookUpdate(update);
    }
}
