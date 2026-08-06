# ✅ Checklist Fase 3 — Flujo conversacional de agendamiento (el core del producto)

**Objetivo:** un cliente agenda una cita de punta a punta hablando con el bot: deep link → servicio → fecha → hora → confirmar. Incluye "Mis citas" y "Cancelar cita".

**Timeline estimado:** 1 semana
**Bloqueado por:** Fase 2 (webhook + `MessagingChannel` + `ConversationState` + deep link) ⛔ → ✅ completada
**Status:** 🟢 **COMPLETADA** (5 de agosto de 2026)

---

## 📌 Árbol de conversación (FSM)

```
/start (deep link ?start=tenantKey)  → resuelve tenant → MENU
   └─ MENU (ReplyKeyboard): [Agendar cita] [Mis citas] [Cancelar cita]

"Agendar cita" / /schedule / /agendar
   └─ CHOOSE_SERVICE → inline keyboard con los servicios del tenant (SERVICE:<id>)
   └─ CHOOSE_DATE    → fechas con disponibilidad (próximos 7 días) + texto libre YYYY-MM-DD
   └─ CHOOSE_TIME    → horas de getAvailableSlots (TIME:HH:mm) + texto libre HH:mm
   └─ CONFIRM        → [✅ Confirmar] [❌ Cancelar]
        ├─ OK  → IAppointmentService.save → "¡Listo! Tu cita quedó agendada" → MENU
        ├─ 409 → AppointmentConflictException → "ya no está disponible, elegí otra hora" → CHOOSE_TIME
        └─ ABORT → MENU

"Mis citas" / /miscitas
   └─ clientRepository.findByTelegramChatIdAndTenantId → lista citas (desc) → MENU

"Cancelar cita" / /cancelar
   └─ lista citas futuras PENDING/CONFIRMED (CANCEL_APPT:<id>) → cancelAppointment → MENU
```

**Estados persistidos en `ConversationState`:** `MENU`, `CHOOSE_SERVICE`, `CHOOSE_DATE`, `CHOOSE_TIME`, `CONFIRM`, `CANCEL_SELECT`, `INITIAL`. El `tenantId` resuelto se persiste en el estado para sobrevivir entre mensajes (los callbacks no traen el deep link de nuevo).

---

## 📌 Resumen de implementación

### `Services/IMessagingChannel` + `Services/implement/TelegramChannel`
- Nuevo método `sendInlineKeyboard(chatId, text, List<Button>)` que arma `InlineKeyboardMarkup` (filas de hasta 3 botones vía `InlineKeyboardRow`).
- Nuevo DTO `DTOS/telegram/Button(text, callbackData)`.
- `TelegramMessage` ganó `firstName` (para saludo y nombre del cliente).

### `Services/implement/TelegramUpdateHandler` (la lógica del FSM)
- Inyecta ahora: `IAppointmentService`, `ServiceRepository`, `StylistRepository`, `PasswordEncoder`, `ObjectMapper`.
- `handle()` despacha a `handleText` o `handleCallback` según `callbackData`.
- **Data JSON** en `ConversationState.data` con `{serviceId, stylistId, date, time}` (Jackson `ObjectMapper`).
- **Callbacks con prefijo:** `SERVICE:`, `DATE:`, `TIME:`, `CANCEL_APPT:` + constantes `MENU`, `AGENDAR`, `MIS_CITAS`, `CANCELAR_CITA`, `CONFIRM`, `ABORT`, `BACK_DATES`.
- **Asociación chat → Client:** `ensureClient()` busca `findByTelegramChatIdAndTenantId`; si no existe crea un `Client` con email sintético `tg_<chatId>@bot.local`, password aleatorio BCrypt, `name_client` desde Telegram, `telegram_chat_id` y tenant.
- **Tenancy en el bot:** `IAppointmentService.save/cancelAppointment/findAppointmentsByClientID` usan `TenantInterceptor.getCurrentTenantIdOrThrow()` (ThreadLocal HTTP). El handler hace `TenantInterceptor.setCurrentTenantId(tenantId)` antes y `clear()` en `finally`.
- **Concurrencia:** `save()` lanza `AppointmentConflictException` si el slot fue tomado → se captura, se re-listán horas y el estado vuelve a `CHOOSE_TIME` (no se pierde el hilo).
- **Errores conversacionales:** fecha/hora inválida re-pregunta; estado/data perdida → vuelve a menú con mensaje claro.

### Fechas disponibles
- `findDatesWithSlots()` consulta `getAvailableSlots` para los próximos 7 días y solo ofrece fechas con horarios (evita mostrar días vacíos).
- El cliente igual puede tipear una fecha manual (`YYYY-MM-DD` o `dd/MM/yyyy`) y una hora (`HH:mm`).

### Seguridad
- El webhook sigue público. El handler solo llama servicios internos con el tenant **resuelto del estado/chat**, nunca de headers HTTP.

### Tests
- `TelegramUpdateHandlerTest` creció de 4 a **15 tests** (Mockito): deep link, menú, servicios, fechas, horas, confirmación (existente y cliente nuevo), conflicto 409, mis citas, cancelar cita.
- `TelegramChannelTest` valida el parseo con `firstName` y sigue 5 tests.
- `TelegramWebhookE2ETest` sigue validando deep link + guía.

**Total:** **41 tests, 0 fallos** (`./mvnw test`).

---

## 🚀 Cómo probarlo en local (con token real)

1. `telegram.bot.token`, `telegram.bot.username` y `telegram.bot.webhook-url` (ngrok) en `application.properties`.
2. `./mvnw spring-boot:run`
3. Abrir `https://t.me/<bot_username>?start=salon-maria-001`
4. Tap "Agendar cita" → elegir servicio (Haircut) → elegir fecha (solo se muestran días con horarios) → elegir hora → Confirmar.
5. Verificar en el log `Bot atendió chat_id=... tenantId=1` y que la cita quedó en la BD (revisar "Mis citas").
6. Probar el 409: agendar dos veces la misma franja, o crear una cita en esa franja por la API y volver a agendar.

> El seed de `import.sql` tiene: tenant 1 = Salón María (John Doe, Haircut 30 min, horario LUN y MIÉ 09-18 / 10-17) y tenant 2 = Estilos Ana (Jane Smith, Coloring 90 min, MAR y JUE).

---

## ⚠️ Gotchas

- **`org.springframework.stereotype.Service` vs `entities.Service`:** en `TelegramUpdateHandler` chocan los imports → la clase usa `@org.springframework.stereotype.Service` (FQN) para desambiguar.
- **`InlineKeyboardRow`** (7.11.0) es el tipo que espera `InlineKeyboardMarkup.keyboard` (no `List<List<InlineKeyboardButton>>`).
- Los builders de telegrambots dentro de streams pierden inferencia genérica → construir las filas con un loop explícito y tipado.
- `TenantInterceptor` es ThreadLocal: en el bot (fuera de HTTP) hay que `setCurrentTenantId`/`clear` explícitamente.
- El email sintético `tg_<chatId>@bot.local` cumple `users.email NOT NULL UNIQUE` sin necesidad de registro web.

---

## ▶️ Próximo paso: Fase 4 (flujo del estilista)

- Comando agenda del día/semana, bloquear horarios, marcar completada/no-show, cancelar con notificación al cliente.
- La capa de Telegram ya está lista (Fases 2-3): se reutiliza `MessagingChannel` + `ConversationState` + deep link.
