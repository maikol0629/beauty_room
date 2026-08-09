package com.mr.sb.beauty_room.dto.messaging;

public record ChannelMessage(
        Channel channel,
        String chatId,
        String text,
        String username,
        String firstName,
        Long userId,
        String callbackData) {
}
