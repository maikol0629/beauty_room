package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.Config.TelegramBotProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceTest {

    private QrCodeServiceImplement serviceWithUsername(String username) {
        TelegramBotProperties props = new TelegramBotProperties();
        props.setUsername(username);
        return new QrCodeServiceImplement(props);
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
}
