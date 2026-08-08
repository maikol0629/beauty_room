package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramChannelTest {

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private ObjectProvider<TelegramClient> telegramClientProvider;

    private TelegramChannel channel;

    @BeforeEach
    void setUp() {
        channel = new TelegramChannel(telegramClientProvider);
    }

    @Test
    void parseUpdate_message_shouldReturnNormalizedMessage() {
        Update update = new Update();
        update.setMessage(Message.builder()
                .chat(Chat.builder().id(111111111L).type("private").build())
                .text("hola")
                .from(User.builder().id(9L).userName("juan").firstName("Juan").isBot(false).build())
                .build());

        Optional<TelegramMessage> parsed = channel.parseUpdate(update);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().chatId()).isEqualTo("111111111");
        assertThat(parsed.get().text()).isEqualTo("hola");
        assertThat(parsed.get().username()).isEqualTo("juan");
        assertThat(parsed.get().userId()).isEqualTo(9L);
    }

    @Test
    void parseUpdate_emptyUpdate_shouldReturnEmpty() {
        assertThat(channel.parseUpdate(new Update())).isEmpty();
        assertThat(channel.parseUpdate(null)).isEmpty();
    }

    @Test
    void sendKeyboard_shouldBuildMessageWithReplyMarkup() throws Exception {
        when(telegramClientProvider.getIfAvailable()).thenReturn(telegramClient);
        channel.sendKeyboard("1", "texto", List.of("A", "B"));

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        SendMessage sent = captor.getValue();

        assertThat(sent.getChatId()).isEqualTo("1");
        assertThat(sent.getText()).isEqualTo("texto");
        assertThat(sent.getReplyMarkup()).isNotNull();
    }

    @Test
    void sendMessage_withoutClient_shouldNotThrow() {
        ObjectProvider<TelegramClient> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        TelegramChannel bare = new TelegramChannel(emptyProvider);
        bare.sendMessage("1", "hola");
        bare.sendKeyboard("1", "texto", List.of("A"));
    }

    @Test
    void sendMessage_shouldBuildMessageAndExecute() throws Exception {
        when(telegramClientProvider.getIfAvailable()).thenReturn(telegramClient);
        channel.sendMessage("2", "adios");

        verify(telegramClient).execute(any(SendMessage.class));
    }
}
