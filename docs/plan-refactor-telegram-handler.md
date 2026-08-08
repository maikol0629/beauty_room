# Plan de refactor — TelegramUpdateHandler

## Objetivo

`services/implement/TelegramUpdateHandler.java` (1.086 líneas) es funcional y cubre todos los flujos del bot, pero mezcla parsing, negocio, UI y gestión de tenant. Este plan lo divide en componentes pequeños mediante **refactors incrementales**, cada uno compilable y con los tests verdes, para reducir riesgo y mejorar mantenibilidad.

## Estado actual (diagnóstico)

- Clase `@Service` (colisión con `entities.Service` resuelta en Fase 8 → `SalonService`) de 1.086 líneas.
- **11 dependencias inyectadas**: `IMessagingChannel`, `IConversationStateService`, `IAppointmentService`, `IBlockedSlotService`, `ClientRepository`, `TenantRepository`, `ServiceRepository`, `StylistRepository`, `StylistScheduleRepository`, `PasswordEncoder`, `ObjectMapper`.
- **10 bloques** duplicados del patrón `TenantInterceptor.setCurrentTenantId(...) / try / finally { clear() }` (confirmAppointment, showMyAppointments, startCancel, cancelAppointmentByCallback, showAgenda, selectBlockEnd, showStylistAppointments, manageAppointmentByCallback, handleReminderConfirm, handleReminderCancel).
- `handleCallback` (switch con ~20 ramas) y `handleText` (if/else encadenado) concentran la complejidad ciclomática.
- 68 tests cubren el handler (`TelegramUpdateHandlerTest`, 37 tests) — la red de seguridad para el refactor.

## Reglas del refactor (aplicar en TODAS las fases)

1. **Una fase = un commit compilable.** Correr `./mvnw -DskipTests compile` y `./mvnw test -Dtest=TelegramUpdateHandlerTest` después de cada fase.
2. **No cambiar comportamiento ni textos** de usuario en las fases mecánicas (1-4). Solo mover código.
3. **Usar `@Service` normal** (la colisión con `entities.Service` ya se resolvió en Fase 8 renombrando a `SalonService`). No introducir FQN.
4. DTOs nuevos en camelCase; los componentes nuevos siguen el patrón `Services/` (interfaz `I*` + `*ServiceImplement`) salvo que sean utils/helpers estáticos sin estado.
5. Fuera de HTTP, los services de negocio requieren el tenant en el ThreadLocal: **todo acceso pasa por el wrapper de la Fase 1**.

---

## Fase 1 (HECHA el 7/8/2026) — Wrapper de tenant (`withTenant`/`runWithTenant`)

**Problema crítico nº2.** Riesgo de fuga de ThreadLocal si una ruta de error omite `clear()`.

- Crear `Security/TenantScope.java` (helper estático final):
  ```java
  public final class TenantScope {
      private TenantScope() {}

      public static <T> T withTenant(Long tenantId, Supplier<T> supplier) {
          TenantInterceptor.setCurrentTenantId(tenantId);
          try {
              return supplier.get();
          } finally {
              TenantInterceptor.clear();
          }
      }

      public static void runWithTenant(Long tenantId, Runnable runnable) {
          withTenant(tenantId, () -> { runnable.run(); return null; });
      }
  }
  ```
- Reemplazar los **10 bloques** `setCurrentTenantId/try/finally/clear` en `TelegramUpdateHandler`. Ejemplo en `showMyAppointments`:
  ```java
  // antes
  TenantInterceptor.setCurrentTenantId(tenantId);
  try { appointments = appointmentService.findAppointmentsByClientID(clientOpt.get().getId()); }
  finally { TenantInterceptor.clear(); }

  // después
  appointments = TenantScope.withTenant(tenantId,
          () -> appointmentService.findAppointmentsByClientID(clientOpt.get().getId()));
  ```
- Los bloques que además capturan excepción para setear `ok = false` (cancelAppointmentByCallback, manageAppointmentByCallback, handleReminderConfirm, handleReminderCancel, selectBlockEnd) se envuelven igual, manteniendo el catch externo.
- `TenantInterceptor` queda únicamente para el flujo HTTP (interceptor de requests), sin cambios.

**Verificación:** `TelegramUpdateHandlerTest` en verde (incluye `@AfterEach clearTenantContext`).

---

## Fase 2 (HECHA el 7/8/2026) — Servicio de cuenta (`TelegramAccountService`)

**Problema crítico nº1 / mejora nº9.** Reduce dependencias inyectadas: el handler deja de tocar `ClientRepository`, `TenantRepository`, `StylistRepository` y `PasswordEncoder` para cuentas.

- Crear interfaz `services/ITelegramAccountService` + `services/implement/TelegramAccountServiceImplement`.
- Mover desde el handler:
  - `resolveTenant(TelegramMessage)` → `Long resolveTenant(TelegramMessage msg)`
  - `ensureClient(TelegramMessage, Long)` → `Client ensureClient(TelegramMessage msg, Long tenantId)` (inyecta `ClientRepository` + `PasswordEncoder`)
  - `ensureStylist(TelegramMessage, Long)` → `Optional<Stylist> findStylistByChat(TelegramMessage msg, Long tenantId)` (inyecta `StylistRepository`)
  - `displayName` / `safeName` → helpers de nombre expuestos aquí o en la vista (Fase 3).
- El handler inyecta `ITelegramAccountService` en lugar de los 3 repos + `PasswordEncoder`.

**Verificación:** tests en verde; el test de deep link (`deepLink_withValidTenantKey...`) debe pasar porque las respuestas de repos se delegan a la nueva clase (inyectar sus mocks).

---

## Fase 3 — Capa de presentación (`TelegramViewService`)

**Problema crítico nº1 / mejora nº6.** Construcción de botones y textos está dispersa; extraerla deja al handler como orquestador.

- Crear `services/ITelegramViewService` + `services/implement/TelegramViewServiceImplement`.
- Mover la construcción de **teclados y mensajes** (usa `IMessagingChannel`):
  - `showMenu(msg, tenantId)` → `void showMenu(TelegramMessage msg, Long tenantId)` (usa `ITelegramAccountService.findStylistByChat` + nombre)
  - `sendGuidance(msg)`
  - `showDateOptions`, `showTimeOptions`, `showServiceSelection` (nuevo, extraído de `startSchedule`)
  - `showAgendaSummary(msg, citas)`, `showMyAppointmentsSummary(msg, citas)` (formateo de `DATETIME_FMT` hoy en el handler)
  - `showConfirmationKeyboard(msg, service, stylist, dateStr, time)` (extraído de `selectTime`)
  - `showCancelOptions(msg, citas)`, `showBlockStartOptions`/`showBlockEndOptions`, `showStylistManagementOptions(msg, citas)` (extraídos de `showBlockStartOptions`/`showBlockEndOptions`/`showStylistAppointments`)
  - Método `sendEndWithMenu(msg, tenantId)` que unifica `channel.sendMessage + showMenu + updateState(MENU)` (mejora nº8, evita inconsistencias).
- El handler conserva la **orquestación** (qué mostrar según estado) y las constantes de pasos; las constantes de UI/callbacks que usa la vista se mueven allí o a `CallbackConstants` (Fase 6).

**Verificación:** tests en verde. Los `verify(channel).sendInlineKeyboard/sendKeyboard` deben seguir pasando porque `TelegramViewService` inyecta `IMessagingChannel`.

---

## Fase 4 (HECHA el 8/8/2026) — Orquestadores por dominio (`TelegramFlowService`)

**Problema crítico nº1 / nº3.** Dividir el handler por dominio para reducir métodos largos y el número de ramas.

- Extraer de `TelegramUpdateHandler` tres colaboradores (sin interfaz si son orquestadores internos, o con `I*` para seguir el patrón del proyecto):
  - `TelegramBookingFlow` — flujo AGENDAR: `start`, `selectService`, `selectDate`, `selectTime`, `confirm` (usando `IAvailabilityService` para slots y `IAppointmentService` para guardar).
  - `TelegramStylistFlow` — AGENDA/BLOQUEAR/GESTIONAR: `showAgenda`, `startBlock`, `selectBlockDate/Start/End`, `manage`.
  - `TelegramAccountFlow` — MENU/MIS_CITAS/CANCELAR: `showMenu`, `showMyAppointments`, `startCancel`, `cancelByCallback` (+ recordatorios `confirm`/`cancel`).
- `TelegramUpdateHandler` queda como **dispatcher**: `handle(Update)` → parse → tenant → `handleText`/`handleCallback` → delegación a los flujos. `handleCallback` y `handleText` se convierten en tablas de despacho:
  - Extraer ramas del `switch` a métodos descriptivos: `handleServiceCallback`, `handleDateCallback`, `handleTimeCallback`, `handleBlockCallbacks`, `handleAppointmentCallbacks`, `handleReminderCallbacks` (problema crítico nº3).

**Verificación:** tests en verde; los tests se mantienen intactos (verifican `channel`/`conversationStateService`, no la estructura interna).

---

## Fase 5 (HECHA el 8/8/2026) — Util de fechas/horas (`TelegramDateUtils`)

**Problema mejora nº7.** Parsing/formatting disperso y con formatos distintos.

- Crear `util/TelegramDateUtils` (estático, sin estado): `parseDate`, `parseTime`, `formatDateForUser`, `formatDateTime`, `dayName` con locale `es`.
- `TIME_FMT`/`DATETIME_FMT` movidas desde `CallbackConstants` a `TelegramDateUtils`.
- Reemplazados los usos en `TelegramViewServiceImplement`, `TelegramBookingFlow`, `TelegramStylistFlow` y `TelegramUpdateHandler` (también el formato inline `dd/MM/yyyy` en `confirmAppointment` y el método privado `parseDate` eliminado).

**Verificación:** tests en verde.

---

## Fase 6 (HECHA el 8/8/2026) — Agrupar constantes de callback

**Mejora opcional.** Convertir constantes `STEP_*`/`CB_*`/`PREFIX_*` en `CallbackConstants` (final con constantes públicas estáticas) o enums por dominio (`BookingCallback`, `StylistCallback`, `ReminderCallback`).

- Consolidado durante las Fases 3-5: todas las `STEP_*`/`CB_*`/`PREFIX_*` viven en `services/CallbackConstants` (clase final, opción A del plan) y son referenciadas por vista, flujos y handler vía `import static`. Sin literales sueltos en el código main.
- Las de recordatorios siguen en `ReminderServiceImplement` (`PREFIX_REMINDER_CONFIRM`/`CANCEL`), como dicta el plan ("no se tocan"); el handler las referencia como alias.
- Beneficio colateral logrado: los prefijos `SERVICE:`/`DATE:` se cambian en un solo lugar.

**Verificación:** tests en verde.

---

## Fase 7 (HECHA el 8/8/2026) — Logs consistentes

**Problema crítico nº4.**

- Plantillas unificadas por evento en flujos y handler (`Error agendando cita para chat_id={}:`, `Error cancelando cita {} para chat_id={}:`, `Error gestionando cita {} para chat_id={}:`, `Error bloqueando horario para chat_id={}:`); todos los `log.error` pasan la excepción completa como último arg.
- Capturas específicas preferidas: `DateTimeParseException` en `handleDateCallback`/`handleTimeCallback`/`handleBlockCallbacks` y `trySelectTimeByText` (con `Exception` como red que ahora loguea); `NumberFormatException` en ids de cita/servicio; `AppointmentConflictException` en el agendado.
- `ConversationStateHelper`: `parseData` conserva nivel `warn` (ahora con `e`) y `toData` loguea `warn` en vez de fallar en silencio.

**Verificación:** compile + revisión manual; no cambia tests.

---

## Fase 8 (HECHA el 7/8/2026) — Renombrar `entities.Service`

> **Implementado** como sub-fases 8.1-8.7 en `docs/analisis-nombres-fase8.md`: `entities.Service`→`SalonService` (con cluster), campos de entidades a camelCase (transparente a BD vía `SpringPhysicalNamingStrategy`), typos corregidos, paquetes a minúsculas. `TelegramUpdateHandler` y `AvailabilityServiceImplement` usan `@Service` normal.

**Problema crítico nº5.**

- Renombrar entidad `Service` → `SalonService` (y el DTO/repo correspondientes: `ServiceRepository` → `SalonServiceRepository`, `ServiceResponseDto` ya existe, revisar `ServiceServiceImplement`). Movería el nombre de tabla igual.
- Al ser un cambio de esquema/tabla, coordinar con Flyway: **no renombrar la columna/tabla** sin una migración `V3` (o mantener `@Table(name="services")` y `@Column` iguales para no tocar BD). Decidir en `DECISION_LOG.md`.
- Beneficio: permite usar `@Service` sin FQN en `TelegramUpdateHandler` y elimina la confusión documentada en AGENTS.md.
- **No hacer como parte de otro cambio** (regla del AGENTS.md).

---

## Fase 9 — JavaDoc y consolidación final

**Mejora opcional.**

- Agregar JavaDoc breve en `TelegramUpdateHandler` (responsabilidad de dispatcher) y en cada colaborador nuevo.
- Verificar que ninguna clase supere ~300 líneas efectivas y que el handler solo despache.
- Actualizar `docs/plan-mejora-mantenimiento.md`, `DECISION_LOG.md`, `PROGRESS.md` y la sección de AGENTS.md que describe el bot.

---

## Orden de prioridad y riesgo

| Prioridad | Fase | Riesgo | Esfuerzo |
|---|---|---|---|
| Alta | 1. TenantScope wrapper | Bajo (mecánico) | S (1 h) |
| Alta | 2. TelegramAccountService | Bajo | S (1-2 h) |
| Alta | 3. TelegramViewService | Bajo-Medio | M (3-4 h) |
| Media | 4. Orquestadores por flujo | Medio | M-L (1-2 días) |
| Media | 5. TelegramDateUtils | Bajo | ✅ HECHA (8/8/2026) |
| Baja | 6. CallbackConstants | Bajo | ✅ HECHA (8/8/2026) |
| Media | 7. Logs consistentes | Bajo | ✅ HECHA (8/8/2026) |
| Baja | 8. Renombrar entities.Service→SalonService | ✅ HECHA (7/8/2026) | ver `docs/analisis-nombres-fase8.md` |
| Baja | 9. JavaDoc y consolidación | Bajo | S |

**Regla de oro:** hacerlas en orden 1 → 9; cada una deja el código compilando y 68+ tests verdes. La Fase 8 es independiente y puede diferirse.
