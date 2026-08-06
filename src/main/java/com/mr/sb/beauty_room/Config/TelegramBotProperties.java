package com.mr.sb.beauty_room.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {

    private String token;
    private String username;
    private String path = "/api/telegram/webhook";
    private String webhookUrl;
}
