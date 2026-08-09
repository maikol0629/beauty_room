package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.messaging.Channel;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.services.IConversationStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelRouterTest {

    @Mock
    private TelegramChannel telegramChannel;
    @Mock
    private WhatsAppChannel whatsappChannel;
    @Mock
    private IConversationStateService conversationStateService;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private StylistRepository stylistRepository;

    @InjectMocks
    private ChannelRouter router;

    @Test
    void sendMessage_withWhatsappConversationState_shouldRouteToWhatsAppEvenIfClientUnknown() {
        String chatId = "5491101234567";
        when(conversationStateService.findByChatId(chatId))
                .thenReturn(Optional.of(ConversationState.builder().chatId(chatId).channel(Channel.WHATSAPP).build()));

        router.sendMessage(chatId, "hola");

        verify(whatsappChannel).sendMessage(eq(chatId), eq("hola"));
        verify(telegramChannel, never()).sendMessage(eq(chatId), eq("hola"));
    }

    @Test
    void sendMessage_withTelegramConversationState_shouldRouteToTelegram() {
        String chatId = "111111111";
        when(conversationStateService.findByChatId(chatId))
                .thenReturn(Optional.of(ConversationState.builder().chatId(chatId).channel(Channel.TELEGRAM).build()));

        router.sendMessage(chatId, "hola");

        verify(telegramChannel).sendMessage(eq(chatId), eq("hola"));
        verify(whatsappChannel, never()).sendMessage(eq(chatId), eq("hola"));
    }

    @Test
    void sendMessage_withoutState_butWhatsappClientRegistered_shouldRouteToWhatsApp() {
        String chatId = "5491107654321";
        when(conversationStateService.findByChatId(chatId)).thenReturn(Optional.empty());
        when(clientRepository.findByWhatsappChatId(chatId)).thenReturn(Optional.of(new Client()));

        router.sendMessage(chatId, "hola");

        verify(whatsappChannel).sendMessage(eq(chatId), eq("hola"));
        verify(telegramChannel, never()).sendMessage(eq(chatId), eq("hola"));
    }

    @Test
    void sendMessage_withoutState_andUnknownChatId_shouldDefaultToTelegram() {
        String chatId = "999";
        when(conversationStateService.findByChatId(chatId)).thenReturn(Optional.empty());
        when(clientRepository.findByWhatsappChatId(chatId)).thenReturn(Optional.empty());
        when(stylistRepository.findByWhatsappChatId(chatId)).thenReturn(Optional.empty());

        router.sendMessage(chatId, "hola");

        verify(telegramChannel).sendMessage(eq(chatId), eq("hola"));
        verify(whatsappChannel, never()).sendMessage(eq(chatId), eq("hola"));
    }
}
