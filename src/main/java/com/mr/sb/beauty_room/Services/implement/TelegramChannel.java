package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.telegram.Button;
import com.mr.sb.beauty_room.DTOS.telegram.TelegramMessage;
import com.mr.sb.beauty_room.Services.IMessagingChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public Optional<TelegramMessage> parseUpdate(Update update) {
        if (update == null) {
            return Optional.empty();
        }
        if (update.getMessage() != null) {
            Message message = update.getMessage();
            String username = (message.getFrom() != null) ? message.getFrom().getUserName() : null;
            String firstName = (message.getFrom() != null) ? message.getFrom().getFirstName() : null;
            Long userId = (message.getFrom() != null) ? message.getFrom().getId() : null;
            return Optional.of(new TelegramMessage(
                    String.valueOf(message.getChatId()),
                    message.getText(),
                    username,
                    firstName,
                    userId,
                    null));
        }
        if (update.getCallbackQuery() != null) {
            var callback = update.getCallbackQuery();
            String chatId = (callback.getMessage() != null && callback.getMessage().getChat() != null)
                    ? String.valueOf(callback.getMessage().getChat().getId())
                    : null;
            String username = (callback.getFrom() != null) ? callback.getFrom().getUserName() : null;
            String firstName = (callback.getFrom() != null) ? callback.getFrom().getFirstName() : null;
            Long userId = (callback.getFrom() != null) ? callback.getFrom().getId() : null;
            if (chatId == null) {
                return Optional.empty();
            }
            return Optional.of(new TelegramMessage(chatId, null, username, firstName, userId, callback.getData()));
        }
        return Optional.empty();
    }
}
