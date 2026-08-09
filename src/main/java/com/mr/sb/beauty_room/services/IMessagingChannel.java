package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.messaging.Button;
import com.mr.sb.beauty_room.dto.messaging.TemplateMessage;

import java.util.List;

/**
 * Canal de mensajería del bot. Los métodos de envío son agnósticos del canal
 * (Telegram, WhatsApp...); el parseo de updates vive en parsers por canal.
 */
public interface IMessagingChannel {

    void sendMessage(String chatId, String text);

    void sendKeyboard(String chatId, String text, List<String> buttons);

    void sendInlineKeyboard(String chatId, String text, List<Button> buttons);

    /**
     * Envía un mensaje proactivo (recordatorios, resúmenes, notificaciones).
     * Cada canal decide cómo emitirlo: Telegram usa el texto libre (fallback),
     * WhatsApp usa templates pre-aprobados de Meta con fallback a texto libre.
     */
    void sendTemplate(String chatId, TemplateMessage template);
}
