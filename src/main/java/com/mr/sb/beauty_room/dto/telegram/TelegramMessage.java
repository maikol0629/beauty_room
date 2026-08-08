package com.mr.sb.beauty_room.dto.telegram;

public record TelegramMessage(
        String chatId,
        String text,
        String username,
        String firstName,
        Long userId,
        String callbackData) {
}
