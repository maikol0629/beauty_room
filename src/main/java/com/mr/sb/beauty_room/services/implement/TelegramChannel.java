package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.messaging.Button;
import com.mr.sb.beauty_room.dto.messaging.TemplateMessage;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramChannel implements IMessagingChannel {

    private static final Logger log = LoggerFactory.getLogger(TelegramChannel.class);
    private static final int INLINE_BUTTONS_PER_ROW = 3;

    private final ObjectProvider<TelegramClient> telegramClientProvider;

    private TelegramClient clientOrNull() {
        return telegramClientProvider.getIfAvailable();
    }

    @Override
    public void sendMessage(String chatId, String text) {
        TelegramClient telegramClient = clientOrNull();
        if (telegramClient == null) {
            log.warn("Telegram client no configurado (falta telegram.bot.token). Mensaje no enviado a chat {}: {}", chatId, text);
            return;
        }
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();
            telegramClient.execute(message);
        } catch (Exception e) {
            log.error("Error enviando mensaje a chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    @Override
    public void sendKeyboard(String chatId, String text, List<String> buttons) {
        TelegramClient telegramClient = clientOrNull();
        if (telegramClient == null) {
            log.warn("Telegram client no configurado (falta telegram.bot.token). Keyboard no enviado a chat {}: {}", chatId, text);
            return;
        }
        try {
            List<KeyboardRow> rows = buttons.stream().map(KeyboardRow::new).toList();
            ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
                    .keyboard(rows)
                    .resizeKeyboard(true)
                    .build();
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .replyMarkup(keyboard)
                    .build();
            telegramClient.execute(message);
        } catch (Exception e) {
            log.error("Error enviando keyboard a chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    @Override
    public void sendInlineKeyboard(String chatId, String text, List<Button> buttons) {
        TelegramClient telegramClient = clientOrNull();
        if (telegramClient == null) {
            log.warn("Telegram client no configurado (falta telegram.bot.token). Inline keyboard no enviado a chat {}: {}", chatId, text);
            return;
        }
        try {
            List<InlineKeyboardRow> rows = new ArrayList<>();
            for (int i = 0; i < buttons.size(); i += INLINE_BUTTONS_PER_ROW) {
                List<Button> chunk = buttons.subList(i, Math.min(i + INLINE_BUTTONS_PER_ROW, buttons.size()));
                List<InlineKeyboardButton> rowButtons = new ArrayList<>();
                for (Button b : chunk) {
                    rowButtons.add(InlineKeyboardButton.builder()
                            .text(b.text())
                            .callbackData(b.callbackData())
                            .build());
                }
                rows.add(new InlineKeyboardRow(rowButtons));
            }
            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboard(rows)
                    .build();
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .replyMarkup(keyboard)
                    .build();
            telegramClient.execute(message);
        } catch (Exception e) {
            log.error("Error enviando inline keyboard a chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    @Override
    public void sendTemplate(String chatId, TemplateMessage template) {
        if (template.buttons() == null || template.buttons().isEmpty()) {
            sendMessage(chatId, template.fallbackText());
        } else {
            sendInlineKeyboard(chatId, template.fallbackText(), template.buttons());
        }
    }
}
