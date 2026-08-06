# ✅ Checklist Fase 4 — Flujo del estilista en Telegram

**Objetivo:** el estilista (dueño del negocio) gestiona su día a día desde el bot: ver su agenda, bloquear horarios, marcar citas como completada/no-show y cancelar citas con notificación al cliente. Sin abrir ningún panel web.

**Timeline estimado:** 1 semana
**Bloqueado por:** Fases 2-3 (webhook + `MessagingChannel` + `ConversationState` + deep link + FSM de agendamiento) ⛔ → ✅ completadas
**Status:** 🟢 **COMPLETADA** (6 de agosto de 2026)

---

## 📌 Árbol de conversación (estilista)

```
/start (deep link ?start=tenantKey) → resuelve tenant → MENU
   └─ MENU dinámico por rol:
        Cliente  → [Agendar cita] [Mis citas] [Cancelar cita]
        Estilista→ [Agendar cita] [Mis citas] [Cancelar cita] [Ver agenda] [Bloquear horario] [Gestionar citas]

"Ver agenda" / /agenda
   └─ inline keyboard: [📅 Hoy] [🗓 Próximos 7 días] [◀ Volver]
        └─ AGENDA_HOY / AGENDA_SEMANA → lista de citas (dd/MM/yyyy HH:mm — servicio — cliente [ESTADO])
             (excluye CANCELLED/REJECTED, ordenadas asc por fecha)

"Bloquear horario" / /bloquear
   └─ BLOCK_DATE:<fecha>  (hoy + 6 días)
        └─ BLOCK_START:<HH:mm>  (pasos de 30 min desde el horario del día; default 08:00)
             └─ BLOCK_END:<HH:mm>  (pasos de 30 min hasta el fin de jornada; default 20:00)
                  └─ IBlockedSlotService.create → "✅ Horario bloqueado: ..." → MENU

"Gestionar citas" / /gestionar
   └─ por cada cita PENDING/CONFIRMED (desde hace 1h en adelante):
        [✅ 14:30 Haircut · María] APPT_COMPLETE:<id> → completeAppointment → "✅ Listo."
        [🚫 14:30 Haircut · María] APPT_NOSHOW:<id>   → noShowAppointment  → "✅ Listo."
        [❌ 14:30 Haircut · María] APPT_CANCEL:<id>   → cancelAppointmentByStylist → "✅ Listo." + notifica al cliente
```

**Notificación automática:** cuando un cliente agenda una cita (`AppointmentService.save`), el estilista recibe:
`📅 ¡Nueva cita agendada!` (cliente, servicio, fecha, hora) si su `stylist.telegram_chat_id` está cargado.

---

## 📌 Resumen de implementación

### `entities/Stylist.java` + `repository/StylistRepository.java`
- Nuevo campo `telegram_chat_id` (`@Column(name = "telegram_chat_id")`).
- `findByTelegramChatId(String)` y `findByTelegramChatIdAndTenantId(String, Long)` (con `@Query` explícita, mismo patrón que `ClientRepository`).

### `entities/AppointmentStatus.java`
- Nuevo estado `NO_SHOW` (además de PENDING, CONFIRMED, COMPLETED, CANCELLED, REJECTED).

### `Services/IAppointmentService.java` + `Services/implement/AppointmentServiceImplement.java`
- `noShowAppointment(long id)`: PENDING/CONFIRMED → `NO_SHOW`.
- `cancelAppointmentByStylist(long id)`: → `CANCELLED` + `notifyClientOfCancellation` (mensaje "❌ Tu cita del ... fue cancelada por el salón." al `client.telegram_chat_id`; no-op si el cliente no tiene chat id).
- `completeAppointment(long id)`: ahora acepta `PENDING` **o** `CONFIRMED` (antes solo `CONFIRMED`).
- `save()`: al guardar la cita llama `notifyStylistOfNewAppointment` (mensaje "📅 ¡Nueva cita agendada!" al `stylist.telegram_chat_id`; no-op si no tiene chat id).
- Se inyectó `IMessagingChannel` en el constructor (los tests del service lo mockean).

### `Services/implement/TelegramUpdateHandler.java`
- **Identificación del estilista:** `ensureStylist()` busca `stylistRepository.findByTelegramChatIdAndTenantId(chatId, tenantId)`. Si no es estilista, los comandos responden "Este comando es solo para estilistas." y vuelven al menú.
- **`resolveTenant` ampliado:** además del deep link y `Client`, ahora consulta `stylistRepository.findByTelegramChatId` (para que el estilista que ya habló con el bot no dependa del deep link).
- **`showMenu` dinámico:** estilista → 6 opciones; cliente → 3 opciones.
- **Nuevos comandos de texto:** `/agenda`, `/bloquear`, `/gestionar` (y variantes en texto libre "ver agenda", "bloquear horario", "gestionar citas").
- **Nuevos callbacks:** `AGENDA_HOY`, `AGENDA_SEMANA`, `BLOCK_DATE:<fecha>`, `BLOCK_START:<HH:mm>`, `BLOCK_END:<HH:mm>`, `APPT_COMPLETE:<id>`, `APPT_NOSHOW:<id>`, `APPT_CANCEL:<id>`.
- **Nuevos estados:** `BLOCK_DATE`, `BLOCK_START`, `BLOCK_END`, `STYLIST_APPT` (los datos `stylistId`, `date`, `start` viven en el JSON de `ConversationState.data`).
- **Horarios de bloqueo:** `buildBlockTimes`/`buildBlockEndTimes` leen el `StylistSchedule` del día (pasos de 30 min); si el estilista no tiene horario ese día, usa 08:00-20:00 por defecto.
- **Tenancy fuera de HTTP:** `showAgenda`/`showStylistAppointments`/`manageAppointmentByCallback`/`selectBlockEnd` usan `TenantInterceptor.setCurrentTenantId(tenantId)` + `clear()` en `finally` (igual que el flujo de cliente).

### `src/main/resources/import.sql`
- Los estilistas seed 1 y 2 tienen `telegram_chat_id` (`555000111`, `555000222`) para probar el flujo de estilista sin deep link.

---

## 📌 Cómo probarlo en local (con token real)

1. `telegram.bot.token`, `telegram.bot.username` y `telegram.bot.webhook-url` (ngrok) en `application.properties`.
2. `./mvnw spring-boot:run`.
3. Para entrar como **estilista**: seteá el `telegram_chat_id` de un estilista seed en la BD con tu chat id real (el seed usa `555000111`/`555000222`; como `ddl-auto=create-drop`, editalo en `import.sql` antes de arrancar).
4. Abrir `https://t.me/<bot_username>?start=salon-maria-001` → el menú debe mostrar las 6 opciones de estilista.
5. Probar cada flujo:
   - **Ver agenda** → Hoy / Próximos 7 días.
   - **Bloquear horario** → elegir día → desde → hasta → confirmar el mensaje "✅ Horario bloqueado".
   - **Gestionar citas** → completar / no-show / cancelar una cita (verificar que el cliente recibe "❌ Tu cita ... fue cancelada por el salón.").
   - Agendar una cita como cliente → el estilista debe recibir "📅 ¡Nueva cita agendada!".

> El seed de `import.sql` tiene: tenant 1 = Salón María (John Doe, Haircut 30 min, horario LUN y MIÉ 09-18 / 10-17) y tenant 2 = Estilos Ana (Jane Smith, Coloring 90 min, MAR y JUE).

---

## ⚠️ Gotchas

- **El estilista se identifica por `telegram_chat_id` en `stylist`**, no por email/login. Para el MVP se asocia manualmente (seed o UPDATE a mano); el onboarding self-service es Fase 10.
- **`AppointmentResponseDto` no expone `telegram_chat_id`** del cliente/estilista (se accede a la entidad dentro del service para notificar, no vía el DTO).
- **Validación manual de fechas/horas pasadas** en el flujo de bloqueo: `@Future` del `BlockedSlotRequestDto` solo valida contra el momento de la llamada, por eso el handler comprueba `date.atTime(start).isBefore(LocalDateTime.now())` y `end.isAfter(start)`.
- **`UnsupportedOperationException`:** `Stream.toList()` devuelve lista inmutable; al querer `add(...)` (botón "◀ Volver") hay que envolver con `new ArrayList<>(...)` (pasó en `showBlockStartOptions`/`showBlockEndOptions`).
- **`IBlockedSlotService.create`** depende de `TenantInterceptor` (ThreadLocal); en el bot hay que `setCurrentTenantId`/`clear` explícitamente.
- **`completeAppointment`** ahora acepta `PENDING` y `CONFIRMED`; si la cita ya está `COMPLETED`/`CANCELLED`/`NO_SHOW` devuelve `false` ("No se pudo actualizar esa cita").

---

## 🧪 Tests

- `TelegramUpdateHandlerTest` creció a **22 tests** (Mockito): menú de estilista, rechazo a no-estilistas, agenda hoy/semana, flujo de bloqueo completo, botones de gestión, completar/no-show/cancelar vía callback. Se mockean `IBlockedSlotService` y `StylistScheduleRepository`; se agregaron los helpers `stubMessage`/`state`/`stubGetOrCreate` y los formatos `TIME_FMT`/`DATETIME_FMT`.
- `AppointmentServiceImplementTest` creció a **7 tests**: notificación al estilista (con y sin `telegram_chat_id`), `completeAppointment` aceptando PENDING/CONFIRMED, `noShowAppointment` → `NO_SHOW`, `cancelAppointmentByStylist` notificando al cliente.

**Total:** **53 tests, 0 fallos** (`./mvnw test` — requiere MariaDB corriendo).

---

## ▶️ Próximo paso: Fase 5 (recordatorios automáticos)

- Job `@Scheduled` que revisa citas próximas (24h y 2h antes) y envía recordatorio al cliente vía Telegram con botones confirmar/cancelar.
- Resumen diario al estilista cada mañana con las citas del día.
- Marcado en la entidad `Notification` para evitar duplicados.
