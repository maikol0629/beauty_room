package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.config.TelegramBotProperties;
import com.mr.sb.beauty_room.config.WhatsAppProperties;
import com.mr.sb.beauty_room.services.IReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final TelegramBotProperties telegramBotProperties;
    private final WhatsAppProperties whatsappProperties;
    private final IReminderService reminderService;
    private final DataSource dataSource;

    @Scheduled(cron = "${app.reminders.interval:0 */15 * * * *}")
    public void runUpcomingReminders() {
        if (!botConfigured()) {
            log.info("Recordatorios automáticos omitidos: no hay canal configurado (telegram.bot.token o whatsapp.access-token)");
            return;
        }
        if (!isReminderSchemaReady()) {
            log.warn("Recordatorios automáticos omitidos: el esquema de BD todavía no está listo");
            return;
        }
        log.info("=== Ejecutando recordatorios automáticos (24h/2h) ===");
        try {
            reminderService.sendUpcomingReminders();
        } catch (DataAccessException e) {
            log.warn("Recordatorios automáticos omitidos por problema de acceso a BD: {}", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage());
        } catch (Exception e) {
            log.error("Error en recordatorios automáticos: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${app.reminders.daily-summary:0 0 7 * * *}")
    public void runDailySummary() {
        if (!botConfigured()) {
            log.info("Resumen diario omitido: no hay canal configurado (telegram.bot.token o whatsapp.access-token)");
            return;
        }
        if (!isReminderSchemaReady()) {
            log.warn("Resumen diario omitido: el esquema de BD todavía no está listo");
            return;
        }
        log.info("=== Ejecutando resumen diario al estilista ===");
        try {
            reminderService.sendDailySummary();
        } catch (DataAccessException e) {
            log.warn("Resumen diario omitido por problema de acceso a BD: {}", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage());
        } catch (Exception e) {
            log.error("Error en resumen diario: {}", e.getMessage(), e);
        }
    }

    private boolean isReminderSchemaReady() {
        List<String> requiredTables = List.of("appointment", "client", "service", "stylist", "tenant", "notification");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            for (String tableName : requiredTables) {
                if (!tableExists(metaData, connection.getCatalog(), tableName)) {
                    log.warn("Tabla requerida no encontrada para recordatorios: {}", tableName);
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            log.warn("No se pudo validar el esquema antes de ejecutar recordatorios: {}", e.getMessage());
            return false;
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String catalog, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = metaData.getTables(catalog, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private boolean botConfigured() {
        String telegramToken = telegramBotProperties.getToken();
        if (telegramToken != null && !telegramToken.isBlank()) {
            return true;
        }
        String whatsappToken = whatsappProperties.getAccessToken();
        return whatsappToken != null && !whatsappToken.isBlank();
    }
}
