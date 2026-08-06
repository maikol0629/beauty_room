# Roadmap — SaaS Multitenant para Estilistas (Telegram → WhatsApp)

**Stack base:** Spring Boot (backend) + Angular (frontend/panel admin) + MariaDB + Telegram Bot API
**Punto de partida:** CRUD de estilistas ya implementado en Spring Boot ✅
**Perfil:** Desarrollador solo, foco en validación rápida
**Fecha de actualización:** 5 de agosto de 2026

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

**ESTADO ACTUAL:**
- ✅ Capa de Telegram lista (Fases 2-3 completadas): webhook, `MessagingChannel`, FSM, deep link
- ❌ **PENDIENTE:** Comandos y lógica de estilista

**Acciones requeridas:**
- [ ] Comando para que el estilista vea su agenda del día/semana
- [ ] Comando para bloquear horarios manualmente (ej: "no disponible mañana 2-4pm")
- [ ] Notificación automática al estilista cuando entra una cita nueva
- [ ] Comando para marcar una cita como completada/no-show (dato valioso para el futuro)
- [ ] Comando para cancelar una cita desde su lado (con notificación al cliente)

**Entregable de la fase:** el estilista puede operar su agenda completa sin abrir ningún panel web, solo Telegram.

---

## FASE 5 — Recordatorios automáticos
*Objetivo: el "wow factor" que justifica que alguien pague por esto — reduce ausencias, que es el dolor #1 de estilistas independientes.*

**ESTADO ACTUAL:**
- ✅ Entidad `Notification` existe en BD
- ❌ **PENDIENTE:** Job programado (Spring `@Scheduled`)
- ❌ **PENDIENTE:** Lógica de envío de recordatorios
- ❌ **PENDIENTE:** Depende de Fase 2 (bot de Telegram)

**Acciones requeridas:**
- [ ] Job programado (Spring `@Scheduled` o cron) que revisa citas próximas (ej: 24h y 2h antes)
- [ ] Envío de recordatorio al cliente vía Telegram con opción de confirmar/cancelar con un botón
- [ ] Envío de resumen diario al estilista cada mañana con las citas del día
- [ ] Marcar en DB que el recordatorio ya se envió (evitar duplicados)

**Nota técnica:** con pocos tenants un cron simple cada 15-30 min alcanza. No necesitas colas (Redis/RabbitMQ) todavía — eso es optimización prematura en tu etapa.

**Entregable de la fase:** producto con loop completo: agendar → recordar → confirmar/asistir. Este es tu MVP validable.

---

## FASE 6 — Validación con usuarios reales (early adopters)
*Objetivo: dejar de suponer y empezar a saber. Esta fase es más de negocio que de código.*

**ESTADO ACTUAL:**
- ❌ No iniciada (depende de Fases 2-5 completas)
- ❌ MVP no existe aún (bot sin lógica conversacional)

**Acciones requeridas:**
- [ ] Reclutar 10-15 estilistas de tu ciudad (contacto directo, gratis a cambio de feedback)
- [ ] Onboarding manual (vos mismo das de alta cada tenant, no lo automatices todavía)
- [ ] Definir tu métrica clave: citas reales agendadas por semana por estilista activo
- [ ] Canal directo de feedback (chat de WhatsApp/Telegram personal con cada estilista piloto)
- [ ] Sesión de feedback semanal las primeras 2-3 semanas
- [ ] Documentar cada fricción/queja

**Entregable de la fase:** Sabés si estilistas reales dejan su libreta/agenda manual por tu bot, o no.

---

## FASE 7 — Iteración basada en feedback real
*Objetivo: arreglar solo lo que bloquea el uso diario, nada más.*

**ESTADO ACTUAL:**
- ❌ No iniciada (depende de Fase 6)

**Tipicamente aparece:**
- [ ] Servicios combinados (ej: corte + color en la misma cita)
- [ ] Manejo de "cliente frecuente" vs cliente nuevo
- [ ] Ajustes finos de disponibilidad (franjas de almuerzo recurrentes)
- [ ] Mejoras de redacción/UX conversacional

**EVITAR en esta fase:** dashboards, reportes, IA conversacional, integraciones de pago.

---

## FASE 8 — Panel de administración en Angular
*Objetivo: ahora sí, frontend. Antes de esto no lo necesitabas para validar.*

**ESTADO ACTUAL:**
- ❌ No iniciada (no es prioritario para MVP)
- ❌ No es bloqueante para validación

**Acciones requeridas:**
- [ ] Login del tenant (estilista/dueño de salón)
- [ ] Vista de agenda (calendario semanal/mensual) — librerías como FullCalendar
- [ ] CRUD visual de servicios y precios
- [ ] CRUD visual de horarios/disponibilidad
- [ ] Listado y ficha de clientes (historial de citas, notas)
- [ ] Métricas básicas: citas del mes, tasa de cancelación/no-show, cliente más frecuente
- [ ] Configuración del bot (mensaje de bienvenida personalizable)

**Nota:** Hasta acá pudiste operar todo con Postman + Telegram. El panel Angular es para cuando el estilista ya confía en el producto.

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
| 4 | Bot: flujo del estilista | ❌ No iniciada | Sí, completa el loop |
| 5 | Recordatorios automáticos | ❌ No iniciada | Sí, "wow factor" |
| 6 | Validación con usuarios reales | ❌ No iniciada | **Aquí decides si sigues** |
| 7 | Iteración por feedback real | ❌ No iniciada | Depende de Fase 6 |
| 8-10 | Panel Angular, billing, self-service | ❌ No iniciada | No, es después de MVP |
| 11-13 | Escalabilidad, WhatsApp, growth | ❌ No iniciada | No, es para tracción real |

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

**Timeline estimado para MVP completo:** 4-5 semanas si trabajas full-time en esto.