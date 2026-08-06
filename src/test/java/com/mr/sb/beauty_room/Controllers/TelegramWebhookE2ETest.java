package com.mr.sb.beauty_room.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "telegram.bot.token=test-token",
        "telegram.bot.username=beauty_room_test_bot",
        "telegram.bot.webhook-url="
})
class TelegramWebhookE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelegramClient telegramClient;

    @Test
    void deepLink_shouldResolveTenantAndSendKeyboard() throws Exception {
        mockMvc.perform(post("/api/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"update_id\":1,"
                                + "\"message\":{\"message_id\":1,"
                                + "\"chat\":{\"id\":111111111,\"type\":\"private\"},"
                                + "\"from\":{\"id\":123,\"first_name\":\"Juan\",\"username\":\"juan\",\"is_bot\":false},"
                                + "\"text\":\"/start salon-maria-001\","
                                + "\"date\":1720000000}}"))
                .andExpect(status().isOk());

        SendMessage sent = captureSentMessage();
        assertThat(sent.getChatId()).isEqualTo("111111111");
        assertThat(sent.getText()).contains("Hola");
        assertThat(sent.getReplyMarkup()).isNotNull();
    }

    @Test
    void unknownChatId_shouldSendGuidanceMessage() throws Exception {
        mockMvc.perform(post("/api/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"update_id\":2,"
                                + "\"message\":{\"message_id\":2,"
                                + "\"chat\":{\"id\":999,\"type\":\"private\"},"
                                + "\"text\":\"/start\","
                                + "\"date\":1720000000}}"))
                .andExpect(status().isOk());

        SendMessage sent = captureSentMessage();
        assertThat(sent.getChatId()).isEqualTo("999");
        assertThat(sent.getText()).contains("enlace de tu salón");
        assertThat(sent.getReplyMarkup()).isNull();
    }

    private SendMessage captureSentMessage() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        return captor.getValue();
    }
}
