package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.messaging.Channel;
import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;

/**
 * Normaliza un update del webhook de Telegram a un ChannelMessage. Vive fuera de
 * IMessagingChannel para que la interfaz de canal quede agnóstica del canal.
 */
@Service
public class TelegramWebhookParser {

    public Optional<ChannelMessage> parse(Update update) {
        if (update == null) {
            return Optional.empty();
        }
        if (update.getMessage() != null) {
            Message message = update.getMessage();
            String username = (message.getFrom() != null) ? message.getFrom().getUserName() : null;
            String firstName = (message.getFrom() != null) ? message.getFrom().getFirstName() : null;
            Long userId = (message.getFrom() != null) ? message.getFrom().getId() : null;
            return Optional.of(new ChannelMessage(
                    Channel.TELEGRAM,
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
            return Optional.of(new ChannelMessage(Channel.TELEGRAM, chatId, null, username, firstName, userId, callback.getData()));
        }
        return Optional.empty();
    }
}
