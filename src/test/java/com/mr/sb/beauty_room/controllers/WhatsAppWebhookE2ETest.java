package com.mr.sb.beauty_room.controllers;

import com.mr.sb.beauty_room.config.WhatsAppProperties;
import com.mr.sb.beauty_room.services.implement.WhatsAppApiClient;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "whatsapp.access-token=test-token",
        "whatsapp.phone-number-id=12345",
        "whatsapp.phone-number=5491101234567",
        "whatsapp.verify-token=test-verify",
        "whatsapp.app-secret=test-secret",
        "telegram.bot.token=test-token",
        "telegram.bot.username=beauty_room_test_bot",
        "telegram.bot.webhook-url="
})
class WhatsAppWebhookE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WhatsAppProperties whatsappProperties;

    @MockBean
    private TelegramClient telegramClient;

    @MockBean
    private WhatsAppApiClient whatsappApiClient;

    @Test
    void verify_withValidToken_shouldReturnChallenge() throws Exception {
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "test-verify")
                        .param("hub.challenge", "challenge-123"))
                .andExpect(status().isOk())
                .andExpect(content().string("challenge-123"));
    }

    @Test
    void verify_withInvalidToken_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong")
                        .param("hub.challenge", "challenge-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void webhook_withValidSignature_shouldProcessTextMessage() throws Exception {
        String body = "{\"entry\":[{\"changes\":[{\"value\":{"
                + "\"contacts\":[{\"profile\":{\"name\":\"Ana\"},\"wa_id\":\"5491101234567\"}],"
                + "\"messages\":[{\"from\":\"5491101234567\",\"type\":\"text\",\"text\":{\"body\":\"estilos-ana-001\"}}]"
                + "}}]}]}";

        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", sign(body))
                        .content(body))
                .andExpect(status().isOk());

        verify(whatsappApiClient, atLeastOnce()).send(any(Map.class));
        verify(telegramClient, never()).execute(any(SendMessage.class));
    }

    @Test
    void webhook_withInvalidSignature_shouldRejectWithUnauthorized() throws Exception {
        String body = "{\"entry\":[]}";

        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=deadbeef")
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(telegramClient, never()).execute(any(SendMessage.class));
    }

    @Test
    void webhook_withoutSignature_shouldRejectWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entry\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    whatsappProperties.getAppSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(expected);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
