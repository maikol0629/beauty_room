package com.mr.sb.beauty_room.dto.messaging;

import java.util.List;

/**
 * Mensaje proactivo que un canal puede enviar como template aprobado (WhatsApp)
 * o como texto libre (Telegram). El nombre del template y sus parámetros son
 * específicos de cada canal; el texto de respaldo (fallbackText) lo usa el canal
 * cuando no hay template configurado/aprobado.
 *
 * @param name         nombre del template en la plataforma del canal (ej: Meta).
 * @param languageCode idioma del template (ej: es, en).
 * @param parameters   valores para los placeholders {{1}}, {{2}}... del template.
 * @param fallbackText texto plano que se envía si el template no está disponible.
 * @param buttons      botones opcionales (callbackData del canal).
 */
public record TemplateMessage(
        String name,
        String languageCode,
        List<String> parameters,
        String fallbackText,
        List<Button> buttons) {
}
