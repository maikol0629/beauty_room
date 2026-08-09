package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.config.TelegramBotProperties;
import com.mr.sb.beauty_room.config.WhatsAppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceTest {

    private QrCodeServiceImplement serviceWithUsername(String username) {
        TelegramBotProperties props = new TelegramBotProperties();
        props.setUsername(username);
        return new QrCodeServiceImplement(props, new WhatsAppProperties());
    }

    @Test
    void generatePng_shouldReturnPngImageBytes() {
        QrCodeServiceImplement service = serviceWithUsername("beauty_room_bot");
        byte[] png = service.generatePng("https://t.me/beauty_room_bot?start=salon-maria-001", 320, 320);
        assertThat(png).isNotEmpty();
        assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    void buildPublicAgendaUrl_shouldBuildDeepLink() {
        QrCodeServiceImplement service = serviceWithUsername("beauty_room_bot");
        assertThat(service.buildPublicAgendaUrl("salon-maria-001"))
                .isEqualTo("https://t.me/beauty_room_bot?start=salon-maria-001");
    }

    @Test
    void buildPublicAgendaUrl_shouldReturnNullWhenUsernameBlank() {
        QrCodeServiceImplement service = serviceWithUsername("");
        assertThat(service.buildPublicAgendaUrl("salon-maria-001")).isNull();
    }

    @Test
    void buildWhatsappAgendaUrl_shouldBuildWaMeLink() {
        WhatsAppProperties whatsapp = new WhatsAppProperties();
        whatsapp.setPhoneNumber("5491101234567");
        TelegramBotProperties telegram = new TelegramBotProperties();
        QrCodeServiceImplement service = new QrCodeServiceImplement(telegram, whatsapp);
        assertThat(service.buildWhatsappAgendaUrl("salon-maria-001"))
                .isEqualTo("https://wa.me/5491101234567?text=salon-maria-001");
    }

    @Test
    void buildWhatsappAgendaUrl_shouldReturnNullWhenPhoneBlank() {
        QrCodeServiceImplement service = serviceWithUsername("beauty_room_bot");
        assertThat(service.buildWhatsappAgendaUrl("salon-maria-001")).isNull();
    }
}
