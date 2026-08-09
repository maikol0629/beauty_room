package com.mr.sb.beauty_room.services.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.sb.beauty_room.dto.messaging.Channel;
import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelMessageParsersTest {

    private final TelegramWebhookParser telegramParser = new TelegramWebhookParser();
    private final WhatsAppPayloadParser whatsappParser = new WhatsAppPayloadParser(new ObjectMapper());

    @Test
    void telegram_message_shouldReturnNormalizedMessage() {
        Update update = new Update();
        update.setMessage(Message.builder()
                .chat(Chat.builder().id(111111111L).type("private").build())
                .text("hola")
                .from(User.builder().id(9L).userName("juan").firstName("Juan").isBot(false).build())
                .build());

        Optional<ChannelMessage> parsed = telegramParser.parse(update);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().channel()).isEqualTo(Channel.TELEGRAM);
        assertThat(parsed.get().chatId()).isEqualTo("111111111");
        assertThat(parsed.get().text()).isEqualTo("hola");
        assertThat(parsed.get().username()).isEqualTo("juan");
        assertThat(parsed.get().userId()).isEqualTo(9L);
    }

    @Test
    void telegram_emptyOrNullUpdate_shouldReturnEmpty() {
        assertThat(telegramParser.parse(new Update())).isEmpty();
        assertThat(telegramParser.parse(null)).isEmpty();
    }

    @Test
    void whatsapp_textMessage_shouldReturnNormalizedMessage() {
        String body = """
                {"entry":[{"changes":[{"value":{
                  "contacts":[{"profile":{"name":"Ana"},"wa_id":"5491101234567"}],
                  "messages":[{"from":"5491101234567","type":"text","text":{"body":"Hola"}}]
                }}]}]}""";

        List<ChannelMessage> parsed = whatsappParser.parse(body);

        assertThat(parsed).hasSize(1);
        ChannelMessage msg = parsed.get(0);
        assertThat(msg.channel()).isEqualTo(Channel.WHATSAPP);
        assertThat(msg.chatId()).isEqualTo("5491101234567");
        assertThat(msg.text()).isEqualTo("Hola");
        assertThat(msg.firstName()).isEqualTo("Ana");
    }

    @Test
    void whatsapp_buttonReply_shouldReturnCallbackData() {
        String body = """
                {"entry":[{"changes":[{"value":{
                  "messages":[{"from":"5491101234567","type":"interactive",
                    "interactive":{"type":"button_reply","button_reply":{"id":"SERVICE:1","title":"Corte"}}}]
                }}]}]}""";

        List<ChannelMessage> parsed = whatsappParser.parse(body);

        assertThat(parsed).hasSize(1);
        ChannelMessage msg = parsed.get(0);
        assertThat(msg.channel()).isEqualTo(Channel.WHATSAPP);
        assertThat(msg.callbackData()).isEqualTo("SERVICE:1");
        assertThat(msg.text()).isNull();
    }

    @Test
    void whatsapp_listReply_shouldReturnCallbackData() {
        String body = """
                {"entry":[{"changes":[{"value":{
                  "messages":[{"from":"5491101234567","type":"interactive",
                    "interactive":{"type":"list_reply","list_reply":{"id":"TIME:10:00","title":"10:00"}}}]
                }}]}]}""";

        List<ChannelMessage> parsed = whatsappParser.parse(body);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).callbackData()).isEqualTo("TIME:10:00");
    }

    @Test
    void whatsapp_nonMessageEvent_shouldReturnEmptyList() {
        String body = """
                {"entry":[{"changes":[{"value":{"statuses":[{"status":"sent"}]}}]}]}""";

        assertThat(whatsappParser.parse(body)).isEmpty();
    }

    @Test
    void whatsapp_malformedBody_shouldReturnEmptyList() {
        assertThat(whatsappParser.parse("not-json")).isEmpty();
    }
}
