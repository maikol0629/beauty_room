package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.messaging.Button;
import com.mr.sb.beauty_room.dto.messaging.Channel;
import com.mr.sb.beauty_room.dto.messaging.TemplateMessage;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.services.IConversationStateService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Router de canales: el bean único de IMessagingChannel que reciben los services.
 * Decide el canal destino según el canal guardado en ConversationState (cada chat
 * recuerda por qué canal habla) o, si no hay estado, según si el chatId está
 * registrado como whatsappChatId (Client/Stylist) o no (Telegram). Mantiene la
 * lógica de negocio agnóstica del canal, como define el DECISION_LOG.
 */
@Service
@Primary
@RequiredArgsConstructor
public class ChannelRouter implements IMessagingChannel {

    private static final Logger log = LoggerFactory.getLogger(ChannelRouter.class);

    private final TelegramChannel telegramChannel;
    private final WhatsAppChannel whatsappChannel;
    private final IConversationStateService conversationStateService;
    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;

    private IMessagingChannel channelFor(String chatId) {
        Optional<ConversationState> state = conversationStateService.findByChatId(chatId);
        if (state.isPresent() && state.get().getChannel() != null) {
            return state.get().getChannel() == Channel.WHATSAPP ? whatsappChannel : telegramChannel;
        }
        boolean isWhatsapp = clientRepository.findByWhatsappChatId(chatId).isPresent()
                || stylistRepository.findByWhatsappChatId(chatId).isPresent();
        return isWhatsapp ? whatsappChannel : telegramChannel;
    }

    @Override
    public void sendMessage(String chatId, String text) {
        channelFor(chatId).sendMessage(chatId, text);
    }

    @Override
    public void sendKeyboard(String chatId, String text, List<String> buttons) {
        channelFor(chatId).sendKeyboard(chatId, text, buttons);
    }

    @Override
    public void sendInlineKeyboard(String chatId, String text, List<Button> buttons) {
        channelFor(chatId).sendInlineKeyboard(chatId, text, buttons);
    }

    @Override
    public void sendTemplate(String chatId, TemplateMessage template) {
        channelFor(chatId).sendTemplate(chatId, template);
    }
}
