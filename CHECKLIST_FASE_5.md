# ✅ Checklist Fase 5 — Recordatorios automáticos

**Objetivo:** reducir ausencias (dolor #1 del estilista) con recordatorios automáticos al cliente antes de la cita y un resumen diario de agenda al estilista. Cierra el loop: agendar → recordar → confirmar/asistir.

**Timeline estimado:** 1 semana
**Bloqueado por:** Fases 2-4 (bot + FSM de agendamiento + flujo del estilista) ⛔ → ✅ completadas
**Status:** 🟢 **COMPLETADA** (6 de agosto de 2026)

---

## 📌 Qué se implementó

### 1. Recordatorio 24h y 2h antes al cliente
- Job `@Scheduled` (cada 15 min por defecto, cron `app.reminders.interval`) ejecuta `ReminderService.sendUpcomingReminders()`.
- Para cada tenant, la query `AppointmentRepository.findRemindable` trae las citas `PENDING`/`CONFIRMED` en las próximas 24h cuyo cliente tenga `telegram_chat_id` (excluye citas canceladas y clientes sin chat).
- Por cita se decide la ventana:
  - **2h:** `startDate` entre ahora y ahora+2h → mensaje corto "🕑 tu cita ... es hoy a las HH:mm".
  - **24h:** `startDate` entre ahora+2h y ahora+24h → mensaje "⏰ Recordatorio: tenés una cita ... el dd/MM/yyyy a las HH:mm".
- El mensaje se envía con `sendInlineKeyboard` y botones:
  - `✅ Confirmar` → `REMINDER_CONFIRM:<appointmentId>`
  - `❌ Cancelar cita` → `REMINDER_CANCEL:<appointmentId>`
- **Deduplicación:** al enviar se persiste una `Notification` con tipo `REMINDER_24H`/`REMINDER_2H`, `appointment_id`, `userId=cliente`, status `SENT`. Antes de enviar se consulta `NotificationRepository.existsByAppointmentIdAndType` → si ya existe, no se reenvía.

### 2. Resumen diario al estilista
- Job `@Scheduled` diario (07:00 por defecto, cron `app.reminders.daily-summary`) ejecuta `ReminderService.sendDailySummary()`.
- Para cada estilista con `telegram_chat_id`, `AppointmentRepository.findStylistDay` trae las citas de hoy (excluye `CANCELLED`/`REJECTED`, ordenadas asc) y se envía el mensaje "📋 Resumen del día dd/MM/yyyy: ..." (o "No tenés citas hoy" si no hay nada).
- **Deduplicación por día:** `existsByUserIdAndTypeAndCreatedAtGreaterThanEqual` con tipo `DAILY_SUMMARY` y `createdAt >= inicio de hoy` → si ya se envió hoy, se omite.

### 3. Confirmar/cancelar desde el recordatorio
- `TelegramUpdateHandler` procesa los callbacks `REMINDER_CONFIRM:<id>` y `REMINDER_CANCEL:<id>`:
  - Confirmar → `IAppointmentService.confirmAppointment(id)` (solo funciona si está `PENDING`) → "✅ ¡Gracias por confirmar! Te esperamos."
  - Cancelar → `IAppointmentService.cancelAppointment(id)` → "❌ Tu cita fue cancelada. Si querés reagendar, usá «Agendar cita»."
  - Ambos filtran por tenant (`findAppointmentForTenant`) y usan `TenantInterceptor.setCurrentTenantId`/`clear` porque el webhook no es HTTP.
- Los prefijos viven como constantes en `ReminderServiceImplement` (`PREFIX_REMINDER_CONFIRM`/`PREFIX_REMINDER_CANCEL`) y el handler los referencia (única fuente de verdad).

### 4. Guardas
- `@EnableScheduling` en `BeautyRoomApplication`.
- `ReminderScheduler` (componente con los `@Scheduled`) se saltea la ejecución si `telegram.bot.token` está vacío → **no crea registros de dedup cuando no hay bot real** (si se configurara el token después, los recordatorios siguen funcionando).
- Los recordatorios **no dependen de `TenantInterceptor`**: el servicio itera tenants y pasa `tenantId` explícito a las queries (robusto para código fuera de HTTP).

---

## 📌 Archivos tocados

| Archivo | Cambio |
|---|---|
| `entities/NotificationType.java` | + `REMINDER_24H`, `REMINDER_2H`, `DAILY_SUMMARY` |
| `entities/Notification.java` | + columna `appointment_id` (`appointmentId`) |
| `repository/NotificationRepository.java` | + `existsByAppointmentIdAndType`, `existsByUserIdAndTypeAndCreatedAtGreaterThanEqual` |
| `repository/AppointmentRepository.java` | + `findRemindable`, `findStylistDay` |
| `Services/IReminderService.java` | **nuevo** (interfaz) |
| `Services/implement/ReminderServiceImplement.java` | **nuevo** (lógica de recordatorios + resumen diario + dedup) |
| `Services/implement/ReminderScheduler.java` | **nuevo** (`@Scheduled` + guarda por token) |
| `BeautyRoomApplication.java` | + `@EnableScheduling` |
| `Services/implement/TelegramUpdateHandler.java` | + callbacks `REMINDER_CONFIRM:`/`REMINDER_CANCEL:` |
| `application.properties` | + `app.reminders.interval`, `app.reminders.daily-summary` |

---

## 🧪 Tests

- `ReminderServiceImplementTest` (**9 tests**, Mockito): recordatorio 24h (envía + marca `REMINDER_24H`), recordatorio 2h (solo corto), dedup (no reenvía), cliente sin `telegram_chat_id` (no envía), múltiples tenants, resumen diario con/ sin citas, estilista sin chat (no envía), dedup diario.
- `TelegramUpdateHandlerTest` (**25 tests**, +3): callback `REMINDER_CONFIRM:` confirma la cita, mensaje de error si ya estaba confirmada, callback `REMINDER_CANCEL:` cancela.
- `AppointmentRepositoryTests` (**4 tests**, @DataJpaTest con MariaDB real): `findRemindable` devuelve citas activas en ventana con cliente con chat, excluye canceladas y clientes sin chat; `findStylistDay` devuelve las del día excluyendo canceladas.

**Total:** **68 tests, 0 fallos** (`./mvnw test` — requiere MariaDB corriendo).

---

## 🚀 Cómo probarlo en local (con token real)

1. `telegram.bot.token`, `telegram.bot.username` y `telegram.bot.webhook-url` (ngrok) en `application.properties`.
2. `./mvnw spring-boot:run`.
3. Para probar el recordatorio **24h**: agendá una cita (deep link + flujo de Fase 3) y, en BD, mové `start_date` a `now() + 12h` con el cliente con `telegram_chat_id` cargado. El job (cada 15 min) debería enviarle el recordatorio con los botones.
4. Para el **2h**: mové `start_date` a `now() + 1h`.
5. Tocar `✅ Confirmar` → el cliente recibe la confirmación; tocar `❌ Cancelar cita` → la cita pasa a `CANCELLED`.
6. Para el **resumen diario**: seteá el cron `app.reminders.daily-summary` a un minuto cercano (`0 */1 * * * *`) y reiniciá; o llamá `ReminderService.sendDailySummary()` con un snippet. El estilista con `telegram_chat_id` recibe su agenda del día.
7. Verificar dedup: el segundo run del job (o reiniciar) no vuelve a enviar lo ya marcado en `notification`.

> El seed de `import.sql` tiene: cliente 3 (Alice, chat `111111111`, tenant 1) y estilistas 1/2 con `telegram_chat_id` (`555000111`/`555000222`).

---

## ⚠️ Gotchas

- **`@ConditionalOnProperty` no garantiza "token no vacío":** la key `telegram.bot.token` existe (vacía) en `application.properties`. Por eso el guard de `ReminderScheduler` valida el valor (`!isBlank`) y no solo la presencia de la key.
- **No marcar envíos sin bot:** si se persiste la `Notification` antes de enviar, y el token está vacío, `TelegramChannel` loguea y no envía — pero el dedup quedaría marcado y nunca se reenviaría. El guard por token evita ese caso.
- **Ventanas de 24h vs 2h:** la ventana de 24h exige `startDate > now+2h`; si el job estuvo caído y la cita ya está a <2h, solo se envía el recordatorio corto (evita el texto "mañana" incorrecto y mensajes duplicados).
- **Matchers de Mockito:** si mezclás matchers y valores literales en un `when(...)`, Mockito lanza `InvalidUseOfMatchers` — usá `eq(...)` para los literales.
- **`findRemindable` exige `client.telegram_chat_id` no nulo:** clientes que agendaron sin resolver chat (o el chat id vacío) no reciben recordatorios, a propósito.

---

## ▶️ Próximo paso: Fase 6 (panel de administración y autonomía del estilista)

- Panel web del estilista (Thymeleaf): login, CRUD de servicios/horarios/bloqueos, citas/clientes y link público con QR. Detalle en [CHECKLIST_FASE_6.md](CHECKLIST_FASE_6.md).
