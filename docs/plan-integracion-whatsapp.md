# Plan de integración — WhatsApp (Meta Cloud API)

Modelo **A**: un solo WABA con un solo número compartido. El bot de WhatsApp de los
salones responde desde un único número; el tenant se resuelve por **deep link**
(`https://wa.me/<numero>?text=<tenantKey>` → el usuario envía el texto tal cual) o
por chat conocido (cliente/estilista ya asociado).

## Estado

**COMPLETADA (8/8/2026):** canal WhatsApp implementado sobre la misma abstracción
`IMessagingChannel` del bot de Telegram. El bot ahora es multi-canal
(Telegram + WhatsApp) con dispatcher único `ChatUpdateHandler`.

## Arquitectura

```
Meta Cloud API (webhook) ──> WhatsAppWebhookController (/api/whatsapp/webhook)
        │  POST (firma X-Hub-Signature-256 verificada)
        ▼
WhatsAppPayloadParser ──> List<ChannelMessage>
        ▼
ChatUpdateHandler.handleMessage(ChannelMessage)   (dispatcher neutro de canal)
        ▼
TelegramChannel / WhatsAppChannel  (IMessagingChannel)
        ▲
ChannelRouter (@Primary IMessagingChannel): decide el canal según ConversationState o whatsappChatId
```

- **`IMessagingChannel`** (`services/`): contrato de envío agnóstico del canal.
  - `sendMessage(chatId, text)` → texto plano.
  - `sendKeyboard(chatId, text, buttons)` → menú de opciones.
  - `sendInlineKeyboard(chatId, text, buttons)` → botones accionables.
  - `sendTemplate(chatId, template)` → mensaje proactivo (recordatorios, resúmenes).
    Telegram lo emite como texto libre + inline keyboard; WhatsApp usa templates
    aprobados de Meta con fallback a texto libre.
- **`ChannelRouter`** (`services/implement/`, `@Primary`): único bean inyectado como
  `IMessagingChannel`. Decide por el canal guardado en `ConversationState` (columna
  `channel`, `V4`; `ChatUpdateHandler` lo persiste en cada mensaje entrante) →
  `WhatsAppChannel`/`TelegramChannel`. Si no hay estado, si el `chatId` está
  registrado como `whatsappChatId` en `Client`/`Stylist` → `WhatsAppChannel`; si no
  → `TelegramChannel`. Así el primer mensaje de WhatsApp (deep link `?text=tenantKey`)
  responde por WhatsApp aunque el cliente aún no exista en BD. La lógica de negocio no
  conoce el canal.
- **`WhatsAppChannel`** (`services/implement/`): mapea los métodos de la interfaz a
  la Cloud API:
  - `sendMessage` → mensaje de texto.
  - `sendKeyboard` → interactive **list** (máx. 10 filas; se trocea).
  - `sendInlineKeyboard` → interactive **buttons** si hay ≤3 botones; si no, list.
  - `sendTemplate` → template aprobado; si falla, cae a texto libre.
- **`WhatsAppApiClient`** (`services/implement/`): POST a
  `graph.facebook.com/{apiVersion}/{phoneNumberId}/messages` con `RestClient`.
  Devuelve `false` si no hay credenciales o falla el envío (gatilla el fallback).
- **`WhatsAppPayloadParser`** (`services/implement/`): normaliza el JSON del webhook
  a `List<ChannelMessage>` (texto, `interactive.button_reply` y `list_reply`).
- **`WhatsAppWebhookController`** (`controllers/`):
  - `GET /api/whatsapp/webhook` → verificación `hub.mode=subscribe` + verify token.
  - `POST /api/whatsapp/webhook` → verifica firma `X-Hub-Signature-256` con el
    `whatsapp.app-secret` (HMAC-SHA256) y reenvía a `ChatUpdateHandler`.
- **`WhatsAppProperties`** (`config/`): `access-token`, `phone-number-id`,
  `phone-number` (para deep links wa.me), `verify-token`, `app-secret`, `api-version`,
  `path`, `webhook-url`. Valores vacíos desactivan el canal.
- **Modelo de datos (V3):** `Client.whatsappChatId` y `Stylist.whatsappChatId`
  (columna `whatsapp_chat_id`). Un usuario puede ser contactado por cualquiera de los
  dos canales; el envío prefiere WhatsApp si está asociado.

## Deep linking (resolución de tenant)

- **Telegram:** `https://t.me/<username>?start=<tenantKey>`.
- **WhatsApp:** `https://wa.me/<phone-number>?text=<tenantKey>`. El usuario toca el
  link, WhatsApp abre un chat con el texto prefabricado y al enviarlo el texto plano
  `tenantKey` se usa como payload (ver `ChatAccountServiceImplement.resolveTenant`).
- Panel: `/panel/public` muestra ambos links y QR (QR de Telegram si está, si no QR
  de WhatsApp). `QrCodeServiceImplement.buildWhatsappAgendaUrl`.

## Recordatorios y notificaciones multi-canal

- `ReminderServiceImplement.sendReminder` arma un `TemplateMessage` y elige el chat
  destino prefiendo WhatsApp: `Client.whatsappChatId` → si no, `telegramChatId`.
- Los callbacks de template de WhatsApp tienen **id fijo** (sin payload dinámico),
  así que el id de la cita se persiste en `ConversationState.data` bajo
  `reminderAppointmentId` (`DATA_REMINDER_APPT_ID`). Al responder el usuario:
  - Botón/callback `REMINDER_CONFIRM` / `REMINDER_CANCEL` (id fijo) → se resuelve la
    cita desde el estado (`ChatUpdateHandler.handleReminderFromState`).
  - Texto libre "confirmar"/"confirmo"/"si" o "cancelar"/"no voy" → también se
    resuelve desde el estado (`handleReminderTextReply`).
  - Telegram sigue usando callbacks `REMINDER_CONFIRM:<id>` / `REMINDER_CANCEL:<id>`
    con el id embebido (sin cambio).
- `AppointmentServiceImplement`: las notificaciones al estilista (nueva cita) y al
  cliente (cancelación) eligen canal prefiendo WhatsApp (`chatTarget`).
- `ReminderScheduler`: ahora considera configurado si hay `telegram.bot.token` **o**
  `whatsapp.access-token`.

## Configuración

Variables (ver `.env.example`):

```properties
whatsapp.access-token=${WHATSAPP_ACCESS_TOKEN:}
whatsapp.phone-number-id=${WHATSAPP_PHONE_NUMBER_ID:}
whatsapp.phone-number=${WHATSAPP_PHONE_NUMBER:}
whatsapp.verify-token=${WHATSAPP_VERIFY_TOKEN:}
whatsapp.app-secret=${WHATSAPP_APP_SECRET:}
whatsapp.api-version=${WHATSAPP_API_VERSION:v21.0}
whatsapp.path=${WHATSAPP_PATH:/api/whatsapp/webhook}
whatsapp.webhook-url=${WHATSAPP_WEBHOOK_URL:}
```

Seguridad: `/api/whatsapp/webhook` está en la lista blanca de `SecurityConfig`.

## Tests

- `ChannelMessageParsersTest` (7): parseo de Telegram y WhatsApp (texto, botón, lista,
  eventos sin mensaje, payload inválido).
- `ChatAccountServiceImplementTest` (18): resolución de tenant por deep link WhatsApp
  y por chat conocido, `ensureClient`/`findStylistByChat` por canal.
- `ReminderServiceImplementTest` (10): `sendTemplate`, target prefiere WhatsApp,
  persistencia del contexto del recordatorio.
- `QrCodeServiceTest` (5): `buildWhatsappAgendaUrl`.
- Suite total: **104 tests OK**.

## Pendiente (fuera de alcance)

- Onboarding self-service de estilistas (Fase 10).
- Reagendado (Fase 11).
- Verificación de firmas en producción con secret real.
- Activación comercial del WABA y aprobación de templates (Meta).
