package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.telegram.Button;
import com.mr.sb.beauty_room.DTOS.telegram.TelegramMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

public interface IMessagingChannel {

    void sendMessage(String chatId, String text);

    void sendKeyboard(String chatId, String text, List<String> buttons);

    void sendInlineKeyboard(String chatId, String text, List<Button> buttons);

    Optional<TelegramMessage> parseUpdate(Update update);
}
