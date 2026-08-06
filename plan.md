# Roadmap — SaaS Multitenant para Estilistas (Telegram → WhatsApp)

**Stack base:** Spring Boot (backend) + Angular (frontend/panel admin) + MariaDB + Telegram Bot API
**Punto de partida:** CRUD de estilistas ya implementado en Spring Boot ✅
**Perfil:** Desarrollador solo, foco en validación rápida
**Fecha de actualización:** 6 de agosto de 2026

---

## 📊 Estado actual del proyecto

### ✅ Completado
- **Backend:** Todas las entidades principales (Stylist, Client, Service, Appointment, StylistSchedule, BlockedSlot, Notification, Payment, Review, StylistRoom)
- **Multitenant (Fase 0):** entidad `Tenant`, `tenant_id` en todas las tablas, `TenantInterceptor` (JWT claim / header), filtros por tenant en repos/servicios, seed 3 tenants, tests de aislamiento
- **Autenticación:** JWT implementado (io.jsonwebtoken 0.11.5)
- **Seguridad:** Spring Security integrado
- **Base de datos:** MariaDB funcionando, todas las tablas creadas
- **APIs:** Controllers CRUD para 10+ recursos
- **Lógica:** Services e interfaces definidas
- **Testing:** Tests unitarios para Repository y Services existen
- **Global error handling:** GlobalExceptionHandler implementado

### 🔴 CRÍTICA — Bloqueantes para continuar
- **Bot de Telegram:** NO INICIADO. Cero líneas de código de integración.

### 🟡 Parcialmente completado
- **Verificación de API:** Endpoints existen pero no validados con Postman
- **Cálculo de slots:** Lógica no verificada si existe en `IAppointmentService`
- **Telegram integration:** Solo existe el campo enum, nada más

---

## FASE 0 — Fundamentos técnicos multitenant
*Objetivo: que tu CRUD actual soporte múltiples salones/estilistas de forma aislada, antes de construir nada más encima.*

**ESTADO ACTUAL: ✅ COMPLETADA (5 de agosto de 2026)**
- ✅ Entidad `Tenant` + enums `TenantPlan` (TRIAL/BASIC/PREMIUM) y `TenantStatus` (ACTIVE/SUSPENDED/CANCELLED)
- ✅ `tenant_id` en todas las entidades (`User`, `StylistRoom`, `Service`, `Appointment`, `StylistSchedule`, `BlockedSlot`, `Notification`, `Payment`, `Review`)
- ✅ `TenantInterceptor` resuelve tenant desde claim JWT `tenantId` o header `X-Tenant-ID` (fallback)
- ✅ Filtros por tenant en repositorios y services (`getCurrentTenantIdOrThrow()`)
- ✅ JWT con claim `tenantId` (registro y login)
- ✅ Seed de 3 tenants en `import.sql` (Salón María, Estilos Ana, Beauty Room Pro)
- ✅ Tests de aislamiento (`TenantIsolationTest`, `TenantJwtFlowTest`) — 14 tests, 0 fallos
- ✅ Verificación manual de aislamiento cross-tenant vía API (404/[] en acceso a datos de otro tenant)

**Entregable de la fase:** Dos tenants distintos pueden coexistir en la BD sin ver datos uno del otro. Verificable vía API con JWTs diferentes. ✅

---

## FASE 1 — Modelo de datos completo (citas, servicios, clientes)
*Objetivo: tener el dominio de negocio completo en base de datos, sin bot todavía. Esto es "solo backend".*

**ESTADO ACTUAL: ✅ COMPLETADA (5 de agosto de 2026)**
- ✅ `Stylist` (nombre, teléfono, id_stylist_room, role)
- ✅ `Client` (nombre, teléfono, email, `telegram_chat_id`)
- ✅ `Service` (nombre, duración en minutos, precio, descripción)
- ✅ `StylistSchedule` (días laborales, horas inicio/fin, asociado a estilista)
- ✅ `BlockedSlot` (bloqueos puntuales/recurrentes)
- ✅ `Appointment` (estilista, cliente, servicio, startDate, endDate, status)
- ✅ Controllers CRUD: `AppointmentController`, `ServiceController`, `ClientController`, `StylistScheduleController`, `BlockedSlotController`
- ✅ Services: interfaces + implementaciones para toda la lógica
- ✅ Validación de status en Appointment (PENDING, CONFIRMED, CANCELLED, COMPLETED)
- ✅ **Cálculo de slots:** `getAvailableSlots(stylistId, serviceId, date)` implementado y expuesto en `GET /api/appointment/slots` (público con X-Tenant-ID)
- ✅ **No-doble-booking:** validado con `existsOverlappingAppointment` (excluye CANCELLED/REJECTED); `POST /api/appointment/save` devuelve **409 Conflict**
- ✅ **`telegram_chat_id` en `Client`**: entity + DTOs (`telegramChatId`) + registro + `findByTelegramChatIdAndTenantId`
- ✅ **Swagger/OpenAPI:** `springdoc-openapi-starter-webmvc-ui` + `OpenApiConfig` + `@Tag`/`@Operation`
- ✅ **Postman collection:** `beauty_room_MVP.postman_collection.json` (endpoints reales)
- ✅ **Tests E2E:** `AppointmentControllerE2ETest` (login real, slots, cita 200, doble booking 409, telegramChatId) — 16 tests, 0 fallos

**Entregable de la fase:** API REST funcional donde, via Postman, podrías crear una cita respetando disponibilidad y sin choques de horario. No necesitas el bot ni el panel Angular todavía. ✅

---

## FASE 2 — Bot de Telegram: esqueleto y conexión
*Objetivo: que el bot hable con tu backend. Todavía sin lógica de negocio compleja.*

**ESTADO ACTUAL: ✅ COMPLETADA (5 de agosto de 2026)**
- ✅ Bot creado en Telegram (BotFather) — token/username los pone el usuario en `application.properties`
- ✅ Configuración de webhook (`telegram.bot.*`; setWebhook automático al arrancar con URL pública HTTPS)
- ✅ Endpoint `/api/telegram/webhook` (público) que recibe updates
- ✅ Interfaz `MessagingChannel` (`sendMessage`, `sendKeyboard`, `parseUpdate`) + adapter `TelegramChannel`
- ✅ Mapeo `telegram_chat_id` ↔ `tenant_id` (por `Client.telegram_chat_id` o por deep link `start=tenantKey`)
- ✅ FSM / `ConversationState` (entidad + repositorio + service)
- ✅ **Dependencia Telegram:** se usó `org.telegram:telegrambots-springboot-webhook-starter:7.11.0` + `telegrambots-client:7.11.0` (el `telegrambots-spring-boot-starter` sugerido antes es de Spring Boot 2.7, incompatible con Boot 3.2.3)
- ✅ Bot responde "Hola" con keyboard (Agendar cita / Mis citas / Cancelar cita) cuando resuelve tenant

**DECISIÓN CRÍTICA (confirmada y documentada):** UN SOLO BOT multitenant. El onboarding del tenant se hace por **deep linking**: `https://t.me/<bot>?start=<tenantKey>` → Telegram envía `/start <tenantKey>` → el bot resuelve el `Tenant` por `tenantKey` y guarda `chat_id → tenant_id` en `ConversationState`.

**Entregable de la fase:** Bot responde "hola" cuando le escribís, y tu backend logea correctamente quién escribió (chat_id + tenant resuelto). ✅

**Verificación:** 30 tests OK (`TelegramUpdateHandlerTest`, `TelegramChannelTest`, `ConversationStateServiceImplementTest`, `TelegramWebhookE2ETest`). Detalle en [CHECKLIST_FASE_2.md](CHECKLIST_FASE_2.md).

---

## FASE 3 — Flujo conversacional: agendar cita (el core del producto)
*Objetivo: un cliente final puede agendar una cita hablando con el bot, de punta a punta.*

**ESTADO ACTUAL: ✅ COMPLETADA (5 de agosto de 2026)**
- ✅ FSM conversacional completo en `TelegramUpdateHandler` (estados `MENU`, `CHOOSE_SERVICE`, `CHOOSE_DATE`, `CHOOSE_TIME`, `CONFIRM`, `CANCEL_SELECT`)
- ✅ Comandos: `/start`, `/schedule`, `/agendar`, `/miscitas`, `/cancel`, `/cancelar`, `/reschedule` (stub informativo)
- ✅ **Flujo de agendamiento:** servicios del tenant → fechas disponibles (próximos 7 días, calculadas con `getAvailableSlots`) → horas → confirmación → `AppointmentSaveDto` → `IAppointmentService.save`
- ✅ `InlineKeyboardMarkup` (`sendInlineKeyboard` en `IMessagingChannel`/`TelegramChannel`, DTO `Button`, filas de hasta 3)
- ✅ **Asociación chat → Client:** `findByTelegramChatIdAndTenantId`; si no existe, se crea el `Client` (email sintético `tg_<chatId>@bot.local`, nombre desde Telegram, `telegram_chat_id`)
- ✅ **Concurrencia / no-doble-booking:** `AppointmentConflictException` (409) capturada → se re-ofrecen horas sin perder el hilo (`CHOOSE_TIME`)
- ✅ **Mis citas:** lista de citas del cliente (fechas desc, máximo 5) con estado
- ✅ **Cancelar cita:** lista citas futuras PENDING/CONFIRMED con botones `CANCEL_APPT:<id>` → `IAppointmentService.cancelAppointment`
- ✅ **Manejo de errores conversacionales:** fechas/horas inválidas re-preguntan, estado perdido → vuelve a menú, tenancy se persiste en `ConversationState.tenantId` para sobrevivir entre mensajes
- ✅ Tests: 15 unitarios de FSM + E2E webhook (deep link) OK — suite total **41 tests, 0 fallos**

**Nota técnica:** `IAppointmentService.save`/`cancelAppointment`/`findAppointmentsByClientID` dependen de `TenantInterceptor` (ThreadLocal HTTP). En el bot no hay request HTTP, así que el handler hace `TenantInterceptor.setCurrentTenantId(tenantId)` antes de llamar y `clear()` en `finally`.

**Entregable de la fase:** Agendás una cita ficticia desde tu Telegram personal de punta a punta (deep link → servicio → fecha → hora → confirmar), sin Postman. ✅

---

## FASE 4 — Flujo del estilista (lado dueño del negocio)
*Objetivo: el estilista también gestiona su día a día desde Telegram, no solo el cliente agenda.*

**ESTADO ACTUAL: ✅ COMPLETADA (6 de agosto de 2026)**
- ✅ **Agenda del día/semana:** `/agenda` o "Ver agenda" → menú `AGENDA_HOY` / `AGENDA_SEMANA` (próximos 7 días); lista citas ordenadas excluyendo `CANCELLED`/`REJECTED` (filtra por estilista vía `findAppointmentsByFilters`)
- ✅ **Bloquear horarios:** `/bloquear` o "Bloquear horario" → flujo fecha → hora inicio → hora fin (callbacks `BLOCK_DATE:`/`BLOCK_START:`/`BLOCK_END:`, pasos de 30 min); horario por defecto 08:00-20:00 si el estilista no tiene `StylistSchedule` ese día; crea `BlockedSlot` con `reason "Bloqueado desde el bot"` vía `IBlockedSlotService.create`
- ✅ **Notificación al estilista en cada cita nueva:** `AppointmentService.save` → `notifyStylistOfNewAppointment` (mensaje "📅 ¡Nueva cita agendada!" al `telegram_chat_id` del estilista; no-op si no tiene chat id)
- ✅ **Completada / no-show:** `/gestionar` o "Gestionar citas" → botones por cita `APPT_COMPLETE:<id>` (→ `completeAppointment`, acepta PENDING/CONFIRMED), `APPT_NOSHOW:<id>` (→ `noShowAppointment`, nuevo estado `NO_SHOW`)
- ✅ **Cancelar desde el estilista con notificación al cliente:** `APPT_CANCEL:<id>` → `cancelAppointmentByStylist` (→ estado `CANCELLED` + `notifyClientOfCancellation` al chat del cliente: "❌ Tu cita del ... fue cancelada por el salón.")
- ✅ **Menú dinámico por rol:** el estilista (resuelto por `stylistRepository.findByTelegramChatId`) ve 6 opciones (incluye Ver agenda / Bloquear horario / Gestionar citas); el cliente ve solo las 3 de siempre
- ✅ Campo `telegram_chat_id` en `Stylist` (entity + `findByTelegramChatId`/`findByTelegramChatIdAndTenantId`) + seed en `import.sql`
- ✅ Tests: 22 en `TelegramUpdateHandlerTest` + 7 en `AppointmentServiceImplementTest` — suite total **53 tests, 0 fallos**

**Nota técnica:** el estilista se identifica por `telegram_chat_id` en la tabla `stylist` (como el cliente en `client`); el bot resuelve el tenant desde el estado o desde el chat del estilista en `resolveTenant`.

**Entregable de la fase:** el estilista puede operar su agenda completa sin abrir ningún panel web, solo Telegram. ✅

---

## FASE 5 — Recordatorios automáticos
*Objetivo: el "wow factor" que justifica que alguien pague por esto — reduce ausencias, que es el dolor #1 de estilistas independientes.*

**ESTADO ACTUAL: ✅ COMPLETADA (6 de agosto de 2026)**
- ✅ **Job programado (`@Scheduled` + `@EnableScheduling`):** `ReminderScheduler` con cron cada 15 min (`app.reminders.interval`) para recordatorios y cron diario 07:00 (`app.reminders.daily-summary`) para el resumen. El job se omite si `telegram.bot.token` está vacío (evita marcar envíos sin bot real).
- ✅ **Recordatorios 24h y 2h antes:** `IReminderService.sendUpcomingReminders()` consulta citas `PENDING`/`CONFIRMED` en ventanas de 24h y 2h (query `findRemindable` que ya excluye citas canceladas y clientes sin `telegram_chat_id`) y envía al cliente un mensaje con botones `✅ Confirmar` (`REMINDER_CONFIRM:<id>`) y `❌ Cancelar cita` (`REMINDER_CANCEL:<id>`).
- ✅ **Deduplicación:** cada envío persiste una `Notification` (nuevos tipos `REMINDER_24H`/`REMINDER_2H` y columna `appointment_id`); `existsByAppointmentIdAndType` evita reenviar. La ventana de 2h solo aplica al recordatorio corto (la de 24h exige que la cita esté a más de 2h) para no solapar mensajes.
- ✅ **Resumen diario al estilista:** `sendDailySummary()` envía a cada estilista con `telegram_chat_id` las citas del día (excluye `CANCELLED`/`REJECTED`, query `findStylistDay`), con mensaje "No tenés citas hoy" si está vacío. Dedup por día (`existsByUserIdAndTypeAndCreatedAtGreaterThanEqual` con tipo `DAILY_SUMMARY`).
- ✅ **Confirmar/cancelar desde el recordatorio:** `TelegramUpdateHandler` procesa los callbacks `REMINDER_CONFIRM:`/`REMINDER_CANCEL:` → `confirmAppointment`/`cancelAppointment` (ambos filtran por tenant) con mensajes de confirmación al cliente.
- ✅ Tests: 9 en `ReminderServiceImplementTest` + 3 de callbacks en `TelegramUpdateHandlerTest` + 3 de queries en `AppointmentRepositoryTests` — suite total **68 tests, 0 fallos**

**Nota técnica:** los recordatorios no dependen de `TenantInterceptor`: el servicio itera tenants y pasa `tenantId` explícito a las queries de repositorio (más robusto para código que corre fuera de HTTP).

**Entregable de la fase:** producto con loop completo: agendar → recordar → confirmar/asistir. Este es tu MVP validable. ✅

---

## FASE 6 — Panel de administración y autonomía del estilista
*Objetivo: dar al estilista control directo sobre servicios, precios, horarios y el canal público de agenda.*

**ESTADO ACTUAL:**
- ✅ Completada (6 de agosto de 2026)
- ✅ Login del tenant/estilista por sesión (form login Spring Security, roles `STYLIST`/`ADMIN`)
- ✅ CRUD visual de servicios y precios
- ✅ CRUD visual de horarios y bloqueos recurrentes
- ✅ Link público de agenda + código QR (zxing) para compartir en redes → deep link del bot `https://t.me/<username>?start=<tenantKey>`
- ✅ Vista de citas (filtros, confirmar/completar/cancelar) y de clientes
- ✅ Edición de estilista (incluye `telegram_chat_id`)
- ✅ Integración con la lógica existente (mismos `IService`/repositorios; aislamiento por tenant)
- ✅ Seguridad en 2 cadenas: panel por sesión + API stateless JWT intacta
- ✅ 8 tests nuevos (QR + seguridad del panel) — suite total **76 tests, 0 fallos**

**Acciones requeridas:**
- [x] Login del tenant / estilista (credenciales JWT o sesión Angular) — sesión Spring Security
- [x] CRUD visual de servicios y precios para el tenant
- [x] CRUD visual de horarios, disponibilidad y bloqueos recurrentes
- [x] Generación de link público de agenda y código QR para compartir en redes
- [x] Vista de citas y clientes con estado de reservas
- [x] Integración con la lógica existente: usar APIs y servicios ya implementados
- [x] Documentar el flujo para que el estilista pueda publicar el nuevo canal de agendamiento

**Entregable de la fase:** el estilista puede administrar su oferta y publicar su agenda sin intervención técnica. El producto deja de depender de soporte manual para cambiar servicios, precios o disponibilidad. ✅ (detalle en [CHECKLIST_FASE_6.md](CHECKLIST_FASE_6.md))

---

## FASE 7 — Validación con usuarios reales (early adopters)
*Objetivo: dejar de suponer y empezar a saber. Esta fase es más de negocio que de código.*

**ESTADO ACTUAL:**
- ❌ No iniciada (depende de Fase 6)
- ✅ MVP de código completo: bot con flujo completo de agendamiento, gestión de citas del estilista y recordatorios automáticos.
- ✅ Este paso confirma si el panel admin y el canal público son realmente útiles.

**Acciones requeridas:**
- [ ] Reclutar 10-15 estilistas de tu ciudad (contacto directo, gratis a cambio de feedback)
- [ ] Onboarding semi-manual con el panel ya disponible
- [ ] Definir tu métrica clave: citas reales agendadas por semana por estilista activo
- [ ] Canal directo de feedback (chat de WhatsApp/Telegram personal con cada piloto)
- [ ] Sesión de feedback semanal las primeras 2-3 semanas
- [ ] Documentar cada fricción/queja y cada cambio de configuración que haga el estilista

**Entregable de la fase:** sabes si estilistas reales pueden autogestionar sus servicios y si el nuevo canal público funciona en la práctica.

---

## FASE 8 — Iteración basada en feedback real
*Objetivo: corregir solo lo que bloquea el uso diario, nada más.*

**ESTADO ACTUAL:**
- ❌ No iniciada (depende de Fase 7)

**Típicamente aparece:**
- [ ] Servicios combinados (ej: corte + color en la misma cita)
- [ ] Manejo de "cliente frecuente" vs cliente nuevo
- [ ] Ajustes finos de disponibilidad (franjas de almuerzo recurrentes)
- [ ] Mejoras en el flujo de link/QR y en la experiencia de publicación
- [ ] Simplificación de la UI del panel admin

**EVITAR en esta fase:** agregar nuevas integraciones grandes antes de validar el panel y la adopción real.

---

## FASE 9 — Monetización y billing
*Objetivo: convertir early adopters gratuitos en clientes de pago.*

**ESTADO ACTUAL:**
- ✅ Entidad `Payment` y `PaymentStatus` existen
- ❌ **PENDIENTE:** Entidades `Plan` y `Subscripcion`
- ❌ **PENDIENTE:** Integración de pasarela de pago
- ❌ **PENDIENTE:** Lógica de bloqueo/degradación

**Acciones requeridas:**
- [ ] Entidad `Plan` (nombre, precio mensual, características)
- [ ] Entidad `Subscripcion` (tenant, plan, fecha inicio, fecha fin, estado)
- [ ] Integración de pasarela local (Colombia: Wompi, PayU, Mercado Pago)
- [ ] Lógica de bloqueo: cuando vence plan, bot deja de agendar nuevas citas
- [ ] Trial gratuito con límite de tiempo (ej: 14-30 días)

---

## FASE 10 — Onboarding self-service (dejar de dar de alta manual)
*Objetivo: escalar sin que cada tenant nuevo dependa de vos.*

**ESTADO ACTUAL:**
- ❌ No iniciada (depende de Fase 6-7)

**Acciones requeridas:**
- [ ] Flujo de registro desde Telegram o landing page
- [ ] Wizard de configuración inicial (servicios, horarios)
- [ ] Verificación de email/teléfono
- [ ] Documentación/tutorial corto

---

## FASE 11 — Robustez y escalabilidad técnica
*Objetivo: ahora sí invertir en infraestructura, cuando ya hay tenants de pago dependiendo del sistema.*

**ESTADO ACTUAL:**
- ✅ Spring Security + JWT implementado
- ✅ Spring Data JPA con MariaDB
- ✅ Global exception handler presente
- ❌ **PENDIENTE:** Tests automatizados completos
- ❌ **PENDIENTE:** Logging estructurado
- ❌ **PENDIENTE:** Rate limiting
- ❌ **PENDIENTE:** Monitoreo/Sentry

**Acciones requeridas:**
- [ ] Manejo de colas si volumen requiere (Redis + Spring Queue, o `@Async`)
- [ ] Logging estructurado y monitoreo (Sentry para errores)
- [ ] Backups automáticos de MariaDB
- [ ] Rate limiting en webhook de Telegram
- [ ] Tests automatizados de flujos críticos (agendar, cancelar, recordatorio)
- [ ] Ambientes separados (dev/staging/prod)

---

## FASE 12 — Integración de WhatsApp
*Objetivo: segundo canal, usando la abstracción `MessagingChannel` que preparaste en la Fase 2.*

**ESTADO ACTUAL:**
- ❌ No iniciada (depende de Fase 2: interfaz `MessagingChannel`)

**Acciones requeridas:**
- [ ] Elegir proveedor: Twilio o 360dialog
- [ ] Diseñar mensajes considerando restricciones de WhatsApp (templates aprobados)
- [ ] Implementar adapter `WhatsAppChannel` sobre interfaz existente
- [ ] Costos: trasladarlos a pricing o absorber como ventaja competitiva
- [ ] Piloto con 2-3 tenants existentes

---

## FASE 13 — Crecimiento y diferenciación (largo plazo)
*Ideas para cuando el core esté sólido y validado — no antes.*

**ESTADO ACTUAL:**
- ❌ No iniciada (muy temprano)

**Posibles mejoras:**
- [ ] Recordatorios inteligentes (ajustar horario según respuesta histórica)
- [ ] Lista de espera automática
- [ ] Programa de referidos entre estilistas
- [ ] Reportes/analítica profunda (ingresos, servicios más rentables)
- [ ] Integración con Google Calendar
- [ ] Multi-idioma

---

## Resumen visual de prioridades

| Fase | Foco | Estado | ¿Bloqueante? |
|---|---|---|---|
| 0 | Backend multitenant + Tenant entity | ✅ **COMPLETADA** | Sí, es la base |
| 1 | API REST completa (slots, validación) | ✅ **COMPLETADA** | Sí, valida backend |
| 2 | Bot: esqueleto + webhook | ✅ **COMPLETADA** | Sí, es el core |
| 3 | Bot: flujo de agendamiento | ✅ **COMPLETADA** | Sí, es el core |
| 4 | Bot: flujo del estilista | ✅ **COMPLETADA** | Sí, completa el loop |
| 5 | Recordatorios automáticos | ✅ **COMPLETADA** | Sí, "wow factor" |
| 6 | Panel admin + autonomía del estilista | ✅ **COMPLETADA** | Sí, necesario antes de validar |
| 7 | Validación con usuarios reales | ❌ No iniciada | Sí, depende de Fase 6 |
| 8 | Iteración por feedback real | ❌ No iniciada | No, depende de adopción |
| 9-13 | Billing, self-service y crecimiento | ❌ No iniciada | No, es después de adopción inicial |

---

## 🚨 CRÍTICO: Próximos pasos recomendados

1. **PRIMERO — Fase 0 (1-2 semanas): ✅ COMPLETADA**
   - [x] Crear entidad `Tenant`
   - [x] Agregar `tenant_id` a todas las tablas
   - [x] Implementar `TenantInterceptor`
   - [x] Tests de aislamiento multitenant
   - **Estado:** 14 tests OK, aislamiento verificado vía API.

2. **LUEGO — Verificar Fase 1 (1 semana): ✅ COMPLETADA**
   - [x] `getAvailableSlots()` funciona y expuesto en `GET /api/appointment/slots`
   - [x] Validación de no-doble-booking implementada y verificada (409 Conflict)
   - [x] Campo `telegram_chat_id` agregado a `Client`
   - [x] Tests end-to-end (2 E2E nuevos) + Postman collection
   - [x] Swagger documentado
   - **Estado:** 16 tests OK; doble-booking → 409 verificado vía API.

3. **FINALMENTE — Fase 2-3 (2-3 semanas): ✅ COMPLETADA**
   - [x] Setup bot Telegram + webhook (`telegrambots-springboot-webhook-starter` 7.11.0, endpoint `/api/telegram/webhook`)
   - [x] Interfaz `MessagingChannel` + adapter `TelegramChannel`
   - [x] `ConversationState` (FSM básico) + deep linking `?start=tenantKey`
   - [x] FSM conversacional completo (Fase 3): elegir servicio → fecha → hora → confirmar
   - [x] MVP completo: agendar cita desde Telegram (+ Mis citas + Cancelar cita)
   - **Razón:** Este es tu primer producto validable.

4. **Fase 4 — Flujo del estilista (1 semana): ✅ COMPLETADA**
   - [x] `/agenda` (día / próximos 7 días) + `/bloquear` (fecha → inicio → fin) + `/gestionar` (completar / no-show / cancelar)
   - [x] Notificación al estilista en cada cita nueva y al cliente cuando el estilista cancela
   - [x] Campo `telegram_chat_id` en `Stylist` + menú dinámico por rol
   - [x] 53 tests OK
   - **Razón:** Completa el loop operativo sin panel web. Detalle en [CHECKLIST_FASE_4.md](CHECKLIST_FASE_4.md).

5. **AHORA — Fase 5: Recordatorios automáticos (1 semana): ✅ COMPLETADA**
   - [x] Job `@Scheduled` cada 15 min: recordatorios 24h y 2h antes al cliente con botones confirmar/cancelar
   - [x] Resumen diario al estilista cada mañana con las citas del día
   - [x] Deduplicación en `Notification` (tipos `REMINDER_24H`/`REMINDER_2H`/`DAILY_SUMMARY`, columna `appointment_id`)
   - [x] 68 tests OK
   - **Razón:** Es el "wow factor" que reduce ausencias (dolor #1). Detalle en [CHECKLIST_FASE_5.md](CHECKLIST_FASE_5.md).

6. **AHORA — Fase 6: Panel de administración y autonomía del estilista (1 semana): ✅ COMPLETADA**
   - [x] Login del tenant/estilista por sesión (roles `STYLIST`/`ADMIN`) + logout
   - [x] CRUD visual de servicios/precios, horarios y bloqueos recurrentes (Thymeleaf)
   - [x] Link público de agenda + QR (zxing) → deep link del bot
   - [x] Vista de citas (filtros + confirmar/completar/cancelar) y clientes
   - [x] 76 tests OK (8 nuevos: QR + seguridad del panel)
   - **Razón:** El estilista administra su oferta y publica su canal sin soporte técnico. Detalle en [CHECKLIST_FASE_6.md](CHECKLIST_FASE_6.md).

7. **SIGUIENTE — Fase 7: Validación con usuarios reales (early adopters)**: reclutar 10-15 estilistas, onboarding semi-manual con el panel, medir citas/semana/estilista. Es más de negocio que de código.

**Timeline estimado para MVP completo:** 4-5 semanas si trabajas full-time en esto.