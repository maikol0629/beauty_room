package com.mr.sb.beauty_room.DTOS.telegram;

public record TelegramMessage(
        String chatId,
        String text,
        String username,
        String firstName,
        Long userId,
        String callbackData) {
}
