package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.Config.TelegramBotProperties;
import com.mr.sb.beauty_room.Services.IReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final TelegramBotProperties telegramBotProperties;
    private final IReminderService reminderService;

    @Scheduled(cron = "${app.reminders.interval:0 */15 * * * *}")
    public void runUpcomingReminders() {
        if (!botConfigured()) {
            log.info("Recordatorios automáticos omitidos: telegram.bot.token no configurado");
            return;
        }
        log.info("=== Ejecutando recordatorios automáticos (24h/2h) ===");
        try {
            reminderService.sendUpcomingReminders();
        } catch (Exception e) {
            log.error("Error en recordatorios automáticos: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${app.reminders.daily-summary:0 0 7 * * *}")
    public void runDailySummary() {
        if (!botConfigured()) {
            log.info("Resumen diario omitido: telegram.bot.token no configurado");
            return;
        }
        log.info("=== Ejecutando resumen diario al estilista ===");
        try {
            reminderService.sendDailySummary();
        } catch (Exception e) {
            log.error("Error en resumen diario: {}", e.getMessage(), e);
        }
    }

    private boolean botConfigured() {
        String token = telegramBotProperties.getToken();
        return token != null && !token.isBlank();
    }
}
