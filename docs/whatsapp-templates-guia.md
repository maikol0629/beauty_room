# WhatsApp — Guía de templates (Meta)

Los mensajes proactivos (recordatorios y resumen diario) salen por WhatsApp con
templates aprobados. Cada template debe crearse en el **Manager de WhatsApp Business**
(https://business.facebook.com/wa/manage/) con el idioma `es`.

## Templates requeridos

| Nombre | Uso | Parámetros | Botones |
|---|---|---|---|
| `appointment_reminder_24h` | Recordatorio de cita (≥2h de antelación) | `{{1}}` servicio, `{{2}}` fecha dd/MM/yyyy, `{{3}}` hora HH:mm | 2 quick-reply: `✅ Confirmar` / `❌ Cancelar cita` |
| `appointment_reminder_2h` | Recordatorio de cita (hoy, <2h) | `{{1}}` servicio, `{{2}}` fecha, `{{3}}` hora | 2 quick-reply: `✅ Confirmar` / `❌ Cancelar cita` |
| `daily_summary` | Resumen diario al estilista | `{{1}}` texto del resumen | sin botones |

Los nombres deben coincidir exactamente con los que usa el código
(`ReminderServiceImplement`): `appointment_reminder_24h`, `appointment_reminder_2h`,
`daily_summary`.

## Texto sugerido

### appointment_reminder_24h
```
⏰ Recordatorio: tenés una cita de {{1}} el {{2}} a las {{3}}. ¿Confirmás que vas?
```
Botones quick-reply: `✅ Confirmar`, `❌ Cancelar cita`.

### appointment_reminder_2h
```
🕑 Recordatorio: tu cita de {{1}} es hoy a las {{3}}. Te esperamos.
```
Botones quick-reply: `✅ Confirmar`, `❌ Cancelar cita`.

### daily_summary
```
📋 {{1}}
```
(El backend arma el resumen completo del día.)

## Cómo se envían

`WhatsAppChannel.sendTemplate` construye el payload:

```json
{
  "messaging_product": "whatsapp",
  "to": "<whatsappChatId>",
  "type": "template",
  "template": {
    "name": "appointment_reminder_24h",
    "language": { "code": "es" },
    "components": [
      { "type": "body", "parameters": [ {"type":"text","text":"Corte"}, {"type":"text","text":"10/08/2026"}, {"type":"text","text":"10:00"} ] }
    ]
  }
}
```

## Botones de los recordatorios (importante)

Los templates de WhatsApp tienen **botones de id fijo** (sin payload dinámico). Por
eso el id de la cita del recordatorio se guarda en `ConversationState.data` bajo
`reminderAppointmentId` y, al responder, se resuelve desde ahí:

- Botón **Confirmar** → callback `REMINDER_CONFIRM` (id fijo) → confirma la cita.
- Botón **Cancelar cita** → callback `REMINDER_CANCEL` → cancela la cita.
- Alternativa por **texto libre**: responder "confirmar", "confirmo" o "si" /
  "cancelar" o "no voy" también confirma/cancela la cita pendiente
  (`ChatUpdateHandler.handleReminderTextReply`).

En Telegram los botones siguen llevando el id embebido
(`REMINDER_CONFIRM:<id>`/`REMINDER_CANCEL:<id>`), así que ambos canales funcionan
sin cambios en el flujo.

## Notas

- Un template solo puede reusarse si el usuario le escribió al número dentro de la
  ventana de **24 horas**; fuera de ella, `sendTemplate` falla y cae al **fallback de
  texto libre** (`TemplateMessage.fallbackText`).
- El estado de un template recién creado es `PENDING`/`IN_APPEAL` hasta que Meta lo
  aprueba. Sin aprobación, el envío falla (y cae al fallback).
- No usar emojis en exceso en los templates (Meta restringe el contenido publicitario
  engañoso).
