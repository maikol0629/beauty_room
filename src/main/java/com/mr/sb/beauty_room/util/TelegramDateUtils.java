package com.mr.sb.beauty_room.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class TelegramDateUtils {

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ES = new Locale("es");

    private TelegramDateUtils() {
    }

    public static LocalDate parseDate(String text) {
        String trimmed = text.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(trimmed, DATE_FMT);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static LocalTime parseTime(String text) {
        return LocalTime.parse(text.trim(), TIME_FMT);
    }

    public static String formatDateForUser(LocalDate date) {
        return date.format(DATE_FMT);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FMT);
    }

    public static String dayName(LocalDate date, TextStyle style) {
        return date.getDayOfWeek().getDisplayName(style, ES);
    }
}
