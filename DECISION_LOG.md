# 🏗️ Decisiones Arquitectónicas — Beauty Room MVP

**Versión:** 1.9  
**Fecha:** 8 de agosto de 2026  
**Estado:** ✅ Decisiones 1-4 y 8 IMPLEMENTADAS (Fase 0 completada). Fases 1-6 (API REST, bot esqueleto, FSM agendamiento, flujo del estilista, recordatorios y panel de administración) completadas. Fase 8 de normalización de nombres COMPLETADA. **Refactor de `TelegramUpdateHandler` (Fases 1-7, 9) COMPLETADO.** Fase 12 (WhatsApp, Meta Cloud API) COMPLETADA. Resto pendiente de fases futuras.

> **Nota de implementación (5 de agosto de 2026):** las decisiones 1 (Shared DB/Schema), 2 (resolución por JWT + header), 3 (queries explícitas con tenant_id) y 4 (JWT simple con claim `tenantId`) quedaron implementadas en el código. Detalle de lo hecho y gotchas en [CHECKLIST_FASE_0.md](CHECKLIST_FASE_0.md).
>
> **Fase 1 (mismo día):** API REST validada. Decisiones tomadas: (a) no-doble-booking devuelve **409 Conflict** vía `AppointmentConflictException` (reutilizando el GlobalExceptionHandler existente) en vez de un código de negocio; (b) las citas `CANCELLED`/`REJECTED` **no** bloquean slots (`existsOverlappingAppointment` los excluye); (c) el endpoint de disponibilidad se expuso como `GET /api/appointment/slots` público (alias de `/availability`) con resolución por header `X-Tenant-ID`; (d) el campo `telegram_chat_id` de `Client` mantiene snake_case en Java y se accede con `@Query` explícita (las queries derivadas `findByTelegramChatId...` no se generan bien); (e) documentación con SpringDoc 2.3.0 (UI en `/swagger-ui.html`). Detalle en [CHECKLIST_FASE_1.md](CHECKLIST_FASE_1.md).
>
> **Fase 2 (mismo día):** bot Telegram en modo **webhook** (elección del usuario por deep linking). Decisiones: (a) se descartó `org.telegram:telegrambots-spring-boot-starter` (es de Spring Boot 2.7) y se usó `telegrambots-springboot-webhook-starter:7.11.0` + `telegrambots-client:7.11.0`; (b) el onboarding de tenant se hace por **deep link `?start=<tenantKey>`** (ver decisión 5, actualizada); (c) la lógica del bot vive en `TelegramUpdateHandler` (reutilizada por el controller y el starter); (d) los beans del bot son `@ConditionalOnProperty(telegram.bot.token)` para que la app arranque sin token y los tests no requieran red; (e) `ConversationState` guarda `tenantId` como columna simple (nullable), no relación JPA. Detalle en [CHECKLIST_FASE_2.md](CHECKLIST_FASE_2.md).
>
> **Fase 3 (mismo día):** FSM conversacional de agendamiento. Decisiones: (a) **estado y datos en `ConversationState`** con `current_step` (`MENU`/`CHOOSE_SERVICE`/`CHOOSE_DATE`/`CHOOSE_TIME`/`CONFIRM`/`CANCEL_SELECT`) y `data` como JSON (`{serviceId, stylistId, date, time}`) con Jackson; (b) **selección por InlineKeyboards** con callbacks prefijados (`SERVICE:`, `DATE:`, `TIME:`, `CANCEL_APPT:`) en vez de texto libre (fecha/hora igual aceptan texto); (c) el **tenant se persiste en el estado** (los callbacks no repiten el deep link) y se usa `TenantInterceptor.setCurrentTenantId`/`clear` porque los services requieren ThreadLocal y el webhook no es HTTP; (d) el **cliente se auto-crea** desde el chat (email sintético `tg_<chatId>@bot.local`, password aleatorio BCrypt) si `findByTelegramChatIdAndTenantId` no lo encuentra; (e) la **concurrencia** se resuelve re-llamando `save()` y capturando `AppointmentConflictException` (re-prompt de horas, sin perder el hilo); (f) el servicio es el que define al estilista (cada `Service` pertenece a un `Stylist`), así elegir servicio implica estilista. Detalle en [CHECKLIST_FASE_3.md](CHECKLIST_FASE_3.md).
>
> **Fase 4 (6 de agosto):** flujo del estilista desde Telegram. Decisiones: (a) el **estilista se identifica por `telegram_chat_id`** (igual que el cliente), con el campo agregado a `Stylist` + `findByTelegramChatId`/`findByTelegramChatIdAndTenantId` — sin login, la asociación chat↔estilista es manual por ahora (onboarding self-service es Fase 10); (b) se reutiliza **el mismo `TelegramUpdateHandler` y el mismo FSM** en lugar de un handler separado: el menú es dinámico según `ensureStylist()`; (c) las **notificaciones viven en `AppointmentService`** (no en el handler): al guardar una cita se notifica al estilista y `cancelAppointmentByStylist` notifica al cliente, reutilizando `IMessagingChannel` — así cualquier canal (API, bot, futuro WhatsApp) dispara la misma notificación; (d) nuevo estado de cita **`NO_SHOW`** como valor de negocio para el futuro (reportes/insights); (e) `completeAppointment` pasa a aceptar **`PENDING` o `CONFIRMED`** (antes solo `CONFIRMED`) para que el estilista pueda cerrar citas sin que el cliente las haya confirmado; (f) el **bloqueo de horarios** usa el flujo fecha→inicio→fin con callbacks y pasos de 30 min, con **horario por defecto 08:00-20:00** si el estilista no tiene `StylistSchedule` ese día. Detalle en [CHECKLIST_FASE_4.md](CHECKLIST_FASE_4.md).
>
> **Fase 5 (mismo día):** recordatorios automáticos. Decisiones: (a) **deduplicación en la entidad `Notification`**: tipos nuevos `REMINDER_24H`/`REMINDER_2H`/`DAILY_SUMMARY` y columna `appointment_id`; antes de enviar se consulta `existsByAppointmentIdAndType` (y por día para el resumen) — sin cola de mensajes, como dice la decisión 9; (b) los recordatorios **no usan `TenantInterceptor`**: el servicio itera los tenants y pasa `tenantId` explícito a las queries de repositorio (`findRemindable`/`findStylistDay`) porque corren fuera de HTTP; (c) el **scheduler es un componente separado** (`ReminderScheduler`) del servicio (`IReminderService`), para poder testear la lógica con Mockito y configurar el cron por property (`app.reminders.interval`/`app.reminders.daily-summary`); (d) **guarda por token real** (`!isBlank`) en el scheduler, no solo `@ConditionalOnProperty` (la key `telegram.bot.token` existe vacía en el properties y esa anotación la consideraría presente) — así nunca se marcan envíos sin bot; (e) las **ventanas** de 24h exigen `startDate > now+2h` y la de 2h `<= now+2h`, para no solapar mensajes ni mandar un "mañana" incorrecto si el job estuvo caído; (f) los callbacks `REMINDER_CONFIRM:`/`REMINDER_CANCEL:` los procesa `TelegramUpdateHandler` con `confirmAppointment`/`cancelAppointment` (reutilizando la lógica tenant-filtrada existente). Detalle en [CHECKLIST_FASE_5.md](CHECKLIST_FASE_5.md).
>
> **Fase 6 (6 de agosto):** panel de administración y autonomía del estilista. Decisiones: (a) **Thymeleaf** en vez de Angular/React (decisión del usuario): renderizado server-side, sin Node ni build frontend, integrado en el mismo Spring Boot — se descarta la idea original de panel Angular para el MVP; (b) **seguridad en 2 cadenas**: `SecurityConfig` tiene `@Order(1)` para `/panel/**` (form login en `/panel/login`, logout `/panel/logout`, roles `STYLIST`/`ADMIN`, sesión `IF_REQUIRED`, **CSRF activo**) y `@Order(2)` para la API stateless JWT original sin cambios — conviven sesión y JWT sin fricción; (c) el panel **reutiliza los mismos `IService`/repositorios** de la API (cero lógica duplicada) y filtra por tenant siempre; (d) el **usuario logueado se recarga desde BD** en `PanelTenantHelper` (`UserRepository.findById`) para evitar `LazyInitializationException` del proxy `tenant` al usar el principal de sesión detached; (e) el **link público de agenda es un deep link del bot** (`https://t.me/<username>?start=<tenantKey>`) generado por `QrCodeServiceImplement` con **zxing** (nuevas deps `core`+`javase` 3.5.3) y servido como PNG en `/panel/qr.png`; (f) `telegram_chat_id` del `Stylist` pasa a editarse desde el panel (campo en `StylistSaveDto`/`StylistResponseDto`), quitando la dependencia de SQL manual para asociar chat↔estilista; (g) las citas del seed pueden tener `status` nulo, así que los templates toleran null. Detalle en [CHECKLIST_FASE_6.md](CHECKLIST_FASE_6.md).
>
> **Fase 8 (7 de agosto):** normalización de nombres. Decisiones: (a) la entidad `Service` se renombra a **`SalonService`** (con su cluster `ISalonService`/`SalonServiceImplement`/`SalonServiceRepository`/`SalonServiceController`/`SalonServiceResponseDto`/`SalonServiceSaveDto`) para eliminar la colisión con `@Service`; los archivos que usaban FQN `@org.springframework.stereotype.Service` vuelven a `@Service`; (b) los **campos de entidad pasan a camelCase** (`nameClient`, `nameStylist`, `nameService`, `nameRoom`, `telegramChatId`): es transparente para la BD porque Boot usa `SpringPhysicalNamingStrategy` (camel→snake), las columnas siguen siendo `name_client`, `telegram_chat_id`, etc. y `V1__init_schema.sql`/`ddl-auto=validate` no cambian — solo se tocaron las 3 `@Query` JPQL que referenciaban `telegram_chat_id`; (c) se corrigen typos (`findByStylystId`→`findByStylistId`, `serviceBelongToStylyst`→`serviceBelongsToStylist`, `findServicesStylistId`→`findByStylistIdAndTenantId`, `StylistSheduleServiceImplement`→`StylistScheduleServiceImplement`) y `Welcome`→`WelcomeController`; (d) los **paquetes se normalizan a minúsculas** (`Controllers`→`controllers`, `Services`→`services`, `Security`→`security`, `Config`→`config`, `Exceptions`→`exceptions`, `DTOS`→`dto`, `DTOS/blocked_slot`→`dto/blockedslot`, `DTOS/stylist_room`→`dto/stylistroom`, `DTOS/Auth`→`dto/auth`); (e) se elimina el archivo basura `Auth/Tables`; (f) se mantiene `entities.User` (correcto de dominio, no colisiona: nadie importa el `User` de Spring). Detalle en [docs/analisis-nombres-fase8.md](docs/analisis-nombres-fase8.md).
>
> **Refactor de `TelegramUpdateHandler` (8 de agosto):** de 1.086 líneas monolíticas a un dispatcher (~300 líneas efectivas) con colaboradores por dominio, siguiendo `docs/plan-refactor-telegram-handler.md` (Fases 1-7, 9). Decisiones: (a) **`TenantScope.withTenant/runWithTenant`** centraliza el patrón `TenantInterceptor.setCurrentTenantId/try/finally/clear` (Fase 1); (b) la resolución de tenant y las cuentas del chat viven en **`ITelegramAccountService`** (Fase 2); (c) la construcción de teclados/mensajes se extrae a **`ITelegramViewService`** (Fase 3); (d) los flujos por dominio **`TelegramBookingFlow`** (agendar), **`TelegramStylistFlow`** (agenda/bloquear/gestionar) y **`TelegramAccountFlow`** (menú/mis citas/cancelar/recordatorios) concentran la lógica y el handler queda como **dispatcher** con tablas de despacho (Fase 4); (e) **`ConversationStateHelper`** centraliza `updateState`/`parseData` del FSM; (f) **`util/TelegramDateUtils`** centraliza parsing/formateo de fechas y horas con locale es (Fase 5); (g) las constantes de callback quedan agrupadas en **`services/CallbackConstants`** (Fase 6); (h) logs unificados con excepción completa y capturas específicas (`DateTimeParseException`, `NumberFormatException`, `AppointmentConflictException`) (Fase 7); (i) JavaDoc de responsabilidad en cada colaborador y verificación de tamaño de clases (Fase 9). El handler conserva el mismo comportamiento (90 tests OK en `TelegramUpdateHandlerTest`).

> **Fase 12 — WhatsApp (8 de agosto):** bot multi-canal sobre la misma abstracción. Decisiones: (a) **Modelo A**: un solo WABA con un número compartido (deep link `wa.me/<num>?text=<tenantKey>`); el tenant se resuelve por el texto plano `tenantKey` o por chat conocido (`ChatAccountServiceImplement.resolveTenant`); (b) **`ChannelRouter` es el bean `@Primary` de `IMessagingChannel`**: decide el canal según si el chatId está registrado como `whatsappChatId` (Client/Stylist, columna nueva vía `V3__whatsapp_chat_id.sql`) → `WhatsAppChannel`, si no → `TelegramChannel`; la lógica de negocio no conoce el canal; (c) el **parseo de updates vive en parsers por canal** (`TelegramWebhookParser`/`WhatsAppPayloadParser`) que normalizan a `ChannelMessage`, y el dispatcher único `ChatUpdateHandler` (ex `TelegramUpdateHandler`, renombrados también `ITelegramViewService`→`IChatViewService`, `ITelegramAccountService`→`IChatAccountService`, `TelegramDateUtils`→`ChatDateUtils`, flujos `Telegram*Flow`→`BookingFlow`/`StylistFlow`/`AccountFlow`) procesa ambos; (d) **seguridad**: `/api/whatsapp/webhook` es público y verifica la firma `X-Hub-Signature-256` (HMAC-SHA256 con `whatsapp.app-secret`); (e) **recordatorios multi-canal**: `ReminderServiceImplement` envía `TemplateMessage` vía `sendTemplate`; los botones de template de WhatsApp tienen id fijo, así que el id de la cita se persiste en `ConversationState.data.reminderAppointmentId` y la confirmación/cancelación se resuelve desde el estado (`handleReminderFromState`) o por texto libre ("confirmar"/"cancelar") (`handleReminderTextReply`); (f) el panel muestra **dos links/QR** (`/panel/qr.png` Telegram, `/panel/qr-whatsapp.png` WhatsApp, `buildWhatsappAgendaUrl`). Guías en `docs/plan-integracion-whatsapp.md`, `docs/whatsapp-guia-rapida.md` y `docs/whatsapp-templates-guia.md`. 104 tests OK (+5 del E2E de WhatsApp). Fix posterior: `ChannelRouter` ya no decide solo por `whatsappChatId` (fallaba el primer mensaje de WhatsApp, que caía a `TelegramChannel` y no se respondía): ahora `ChatUpdateHandler` persiste `msg.channel()` en `ConversationState` (columna `channel`, `V4__conversation_channel.sql`) y el router consulta ese canal primero; si no hay estado, cae al `whatsappChatId` de Client/Stylist y por último a `TelegramChannel`. 114 tests OK.

> **Decisión de negocio (9 de agosto de 2026):** el producto se vende primero con **Telegram**; la conexión a WhatsApp se ofrece como **add-on** donde el cliente/estilista usa su **propio WABA** (Modelo B, un WABA por estilista en vez del Modelo A compartido). El código multi-canal queda (botones, recordatorios, `ChannelRouter`) pero el desarrollo de WPP queda en pausa hasta que se active el primer cliente con WABA propio. Nota operativa: en modo Development de Meta, la prueba de WPP exige el número de prueba + allowlist (hasta 5 números); sin verificación de negocio y publicación a Live, la app no responde por un número de negocio real.

> **Panel Super Admin (9 de agosto de 2026):** decisión de negocio: el dueño de la plataforma gestiona a sus clientes (salones) desde el mismo panel. Decisiones: (a) nuevo rol **`SUPER_ADMIN`**; (b) el super admin vive en un **tenant de plataforma reservado** (`beauty-room-platform`, plan PREMIUM) — sin migración de esquema (users.tenant_id sigue NOT NULL) y compatible con `ddl-auto=validate`; se **oculta** del listado de salones y `findTenantById` lo rechaza; (c) la cuenta se **provisiona sola al arrancar** (`config/SuperAdminInitializer`, ApplicationRunner idempotente) leyendo `app.superadmin.email`/`app.superadmin.password` (env-overridables, fallback `superadmin@beautyroom.app`/`superadmin123`); (d) `SuperAdminServiceImplement` **no usa `TenantInterceptor`**: accede a los repositorios directamente (el super admin ve toda la plataforma); (e) seguridad: `/panel/super/**` → `hasRole("SUPER_ADMIN")` ANTES de `/panel/**`; `PanelController.home` redirige a `/panel/super`; navbar role-aware vía atributo `isSuperAdmin` (modelado por `PanelModelAdvice`, sin dependencia de thymeleaf-extras-springsecurity); (f) alcance v1: dashboard con KPIs globales, CRUD de salones (crear salón + usuario ADMIN, editar, suspender/activar) y detalle con usuarios y últimas citas. 128 tests OK (+14).

---

## 1. Estrategia Multitenant

### Decisión: Shared Database, Shared Schema

```
┌─────────────────────────────────────────┐
│           Una sola Base de Datos        │
│          (MariaDB localhost:3306)       │
├─────────────────────────────────────────┤
│  Tabla: tenant                          │
│  ├─ 1: Salón María (tenant_key=sm001)  │
│  ├─ 2: Estilos Ana (tenant_key=sa001)  │
│  └─ 3: Beauty Room Pro                  │
├─────────────────────────────────────────┤
│  Tabla: stylist                         │
│  ├─ ID 1 → tenant_id=1 (Salón María)   │
│  ├─ ID 2 → tenant_id=2 (Estilos Ana)   │
│  └─ ID 3 → tenant_id=3 (Beauty Room)   │
├─────────────────────────────────────────┤
│  Tabla: appointment                     │
│  ├─ ID 100 → tenant_id=1               │
│  ├─ ID 101 → tenant_id=2               │
│  └─ ID 102 → tenant_id=1               │
└─────────────────────────────────────────┘
```

**Pros:**
- ✅ Fácil implementar con Spring Data JPA
- ✅ Sin complejidad de schema-per-tenant
- ✅ Escalable para 100s-1000s de tenants
- ✅ Una sola backup/restore

**Contras:**
- ❌ Todas las queries deben filtrar por tenant_id
- ❌ Riesgo: olvidar el WHERE tenant_id=X (security issue)

**Alternativas descartadas:**
- ❌ Schema-per-tenant: demasiado complejo para MVP
- ❌ Database-per-tenant: no escala operativamente

---

## 2. Resolución de Tenant por Request

### Decisión: JWT + TenantInterceptor

```
┌─────────────────────────────────────────────────┐
│  Cliente login                                  │
│  POST /api/auth/login                          │
│  Body: {email, password}                       │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│  AuthController busca User en DB               │
│  Obtiene Tenant ID desde User.stylist.tenant.id│
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│  JwtProvider genera JWT CON tenant_id claim    │
│  {                                              │
│    "sub": "stylist@salon.com",                 │
│    "userId": 1,                                │
│    "tenantId": 1,  ◄─── KEY                    │
│    "role": "STYLIST",                          │
│    "exp": 1723939200                           │
│  }                                             │
└──────────────────┬──────────────────────────────┘
                   │ Retorna JWT al cliente
┌──────────────────▼──────────────────────────────┐
│  Cliente almacena JWT (localStorage)            │
│  GET /api/appointments                          │
│  Header: Authorization: Bearer JWT              │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│  TenantInterceptor.preHandle()                  │
│  1. Extrae JWT                                 │
│  2. Decodifica y obtiene "tenantId" claim      │
│  3. Guarda en ThreadLocal<Long> tenantId       │
│  4. El resto del request executa con contexto  │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│  AppointmentService.findAll()                   │
│  1. Obtiene tenantId desde ThreadLocal          │
│  2. Ejecuta:                                    │
│     SELECT * FROM appointment                   │
│     WHERE tenant_id = :tenantId                 │
│  3. Solo retorna citas del tenant logueado     │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│  Response: [Appointment, ...]                   │
│  (solo las del tenant del JWT)                  │
└─────────────────────────────────────────────────┘
```

**Pros:**
- ✅ Stateless: no depende de sesiones
- ✅ Escalable horizontalmente (varias instancias)
- ✅ Tenant resuelto una vez por request
- ✅ Fácil para APIs REST

**Contras:**
- ❌ JWT debe refrescarse cada 24h
- ❌ Si JWT es robado, atacante accede como ese tenant

**Security:** Usar HTTPS en producción + refresh token rotation (Fase 11)

---

## 3. Filtros de Tenant automáticos

### Decisión: Queries explícitas con tenant_id

```java
// En lugar de esto (INSEGURO):
List<Stylist> findAll();
// Alguien olvida agregar WHERE tenant_id=X

// Hacer esto (SEGURO):
List<Stylist> findByTenantId(Long tenantId);
List<Stylist> findByTenantIdAndStatus(Long tenantId, StylistStatus status);
```

**Alternativa descartada:** Hibernate `@Filter` (demasiado complejo para MVP)

---

## 4. Autenticación

### Decisión: JWT Simple (sin OAuth)

```
┌─────────────────────────────────┐
│  Fase MVP: JWT simple           │
│  ✅ Email + Password            │
│  ✅ Token de 24h                │
│  ❌ No OAuth, no Social Login   │
└─────────────────────────────────┘
       │
       └─► Fase 8-9: Agregar OAuth (Google, etc.)
```

**Por qué JWT y no sesiones?**
- ✅ Bot de Telegram puede usar JWT fácilmente
- ✅ Escalable sin compartir sesión entre servidores

**Estructura JWT esperada:**
```json
{
  "sub": "stylist@example.com",
  "userId": 1,
  "tenantId": 1,
  "role": "STYLIST",
  "exp": 1723939200,
  "iat": 1723852800
}
```

---

## 5. Integración Bot Telegram

### Decisión: Un solo bot multitenant (no bot-por-tenant)

```
                  ┌─────────────────────┐
                  │   Bot Telegram      │
                  │   (un solo bot)     │
                  │  @beauty_room_bot   │
                  └──────────┬──────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
      ┌─────▼────┐    ┌─────▼────┐    ┌─────▼────┐
      │ Chat: 111│    │ Chat: 222│    │ Chat: 333│
      │(Salón 1) │    │(Salón 2) │    │(Salón 3) │
      └──────────┘    └──────────┘    └──────────┘
            │                │                │
            └────────────────┼────────────────┘
                             │
                    ┌────────▼────────┐
                    │ Backend Spring  │
                    │ (cloud.example) │
                    └─────────────────┘
```

**Por qué UN bot?**
- ✅ Operativamente simple (un BotFather token)
- ✅ Escalable: cada stylist se loguea en el bot
- ✅ Costo: un bot para N tenants (no N bots)
- ✅ Fácil agregar WhatsApp después

**Alternativa descartada:** Bot-por-tenant (cada stylist crea su bot)
- ❌ Operativamente complejo
- ❌ Alto costo de mantenimiento
- ❌ Confuso para usuarios

### Flujo de primer mensaje (✅ IMPLEMENTADO en Fase 2 vía deep linking):

```
Cliente abre el deep link de su salón:  https://t.me/<bot>?start=salon-maria-001
Telegram envía al webhook:              /start salon-maria-001

Bot:
  → TenantRepository.findByTenantKey("salon-maria-001") → tenant_id=1
  → Guarda conversation_state: chat_id + tenant_id
  → Responde saludo + keyboard (Agendar cita / Mis citas / Cancelar cita)

Si el /start NO trae payload:
  → ClientRepository.findByTelegramChatId(chat_id) → tenant_id
  → Si tampoco: pide al usuario abrir el deep link de su salón
```

> El deep link `?start=<tenantKey>` sustituye al "código de invitación o teléfono" que se había previsto antes: es el mismo mecanismo, pero el tenantKey viaja en el propio enlace y no hay que tipear nada.

---

## 6. Canales de mensajería

### Decisión: Interfaz `MessagingChannel` + Adapters

```java
// Interfaz genérica
public interface MessagingChannel {
    void sendMessage(String chatId, String text);
    void sendKeyboard(String chatId, String text, List<Button> buttons);
    Update receiveMessage(String updateJson);  // Webhook
    void editMessage(String chatId, String messageId, String text);
}

// Adapter 1: Telegram
public class TelegramChannel implements MessagingChannel { 
    // Implementa usando Telegram Bot API
}

// Adapter 2: WhatsApp (Fase 12)
public class WhatsAppChannel implements MessagingChannel {
    // Implementa usando Twilio o 360dialog
}

// Lógica de negocio (agnóstica de canal)
public class SchedulingService {
    private MessagingChannel channel;
    
    public void scheduleAppointment(String chatId) {
        // No sabe si es Telegram o WhatsApp
        // Solo usa el channel genérico
        channel.sendMessage(chatId, "¿Qué servicio deseas?");
    }
}
```

**Ventaja:** Agregar WhatsApp no requiere reescribir lógica de negocio.

---

## 7. Estado de Conversación (FSM)

### Decisión: Table en BD + ThreadLocal para sesión actual

```java
// Entidad para persistir estado
@Entity
public class ConversationState {
    private String chatId;
    private Long tenantId;
    private String currentStep;  // "waiting_for_service", "waiting_for_date", etc.
    private Map<String, Object> context;  // JSON con datos temporales
    private LocalDateTime lastUpdated;
}

// ThreadLocal para sesión actual
public class ConversationContext {
    private static ThreadLocal<ConversationState> current = new ThreadLocal<>();
    
    public static ConversationState get() { return current.get(); }
    public static void set(ConversationState s) { current.set(s); }
}
```

**Máquina de estados esperada:**

```
START
  │
  ├─→ ["WAITING_FOR_AUTH"] Usuario sin loguear
  │       ├─ Input: código/teléfono
  │       └─→ ["AUTHENTICATED"]
  │
  ├─→ ["AUTHENTICATED"] Usuario logueado
  │       ├─ Input: /schedule
  │       └─→ ["WAITING_FOR_SERVICE"]
  │
  ├─→ ["WAITING_FOR_SERVICE"]
  │       ├─ Input: elige servicio
  │       └─→ ["WAITING_FOR_DATE"]
  │
  ├─→ ["WAITING_FOR_DATE"]
  │       ├─ Input: elige fecha
  │       └─→ ["WAITING_FOR_TIME"]
  │
  ├─→ ["WAITING_FOR_TIME"]
  │       ├─ Input: elige hora
  │       └─→ ["CONFIRMING"]
  │
  ├─→ ["CONFIRMING"]
  │       ├─ Input: "Confirmar" → Backend crea Appointment
  │       └─→ ["COMPLETE"]
  │
  └─→ ["COMPLETE"] Cita agendada exitosamente
```

---

## 8. Base de datos

### Decisión: MariaDB (no PostgreSQL como el plan original decía)

**Razón actual:** Proyecto ya está configurado con MariaDB en pom.xml y application.properties.

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/beauty_room
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

**Migración futura (si aplica):**
- Si escalas: Considera PostgreSQL (mejor para multitenant, JSONB, partitioning)
- Proceso: escribir Liquibase migrations, no es crítico para MVP

---

## 9. Notificaciones

### Decisión: Base de datos + Job programado

```
┌─────────────────────────────────┐
│  Spring @Scheduled              │
│  Cada 30 minutos:               │
└──────────┬──────────────────────┘
           │
┌──────────▼──────────────────────┐
│  1. Query citas próximas 24h    │
│  2. Filtrar por tenant          │
│  3. Para cada cita:             │
│     - Ya se notificó? (check)   │
│     - Si no → enviar via bot    │
│     - Marcar como notificado    │
└─────────────────────────────────┘
```

**Estructura en BD:**

```java
@Entity
public class Notification {
    private Long id;
    private Long appointmentId;
    private Long clientId;
    private String message;
    private NotificationType type;  // REMINDER_24H, REMINDER_2H, CONFIRMATION
    private LocalDateTime sentAt;
    private boolean delivered;
    private Long tenantId;
}
```

**Por qué no Redis/RabbitMQ en MVP?**
- Job cada 30 min es suficiente para <1000 tenants
- Simplifica arquitectura
- Escala bien hasta Phase 11

---

## 10. Reportes (Fase 7+)

### Decisión: Aplazada hasta Phase 8

**MVP NO incluye:**
- ❌ Dashboard de métricas
- ❌ Reportes de ingresos
- ❌ Analytics avanzado

**MVP SÍ incluye:**
- ✅ Historial de citas (lista simple)
- ✅ Estado de cliente (confirmada/cancelada)
- ✅ Horarios del estilista

**Razón:** Enfoque en validar valor core (agendamiento) antes de "nice-to-haves".

---

## 11. Seguridad (Roadmap)

| Fase | Medida |
|---|---|
| MVP (0-3) | HTTPS + JWT + tenant filtering |
| 6 | Rate limiting en webhook Telegram |
| 9 | CSRF protection (cuando agreguemos forms en Angular) |
| 11 | Refresh token + token rotation |
| 11 | Encriptación de datos sensibles (teléfono, email) |
| 12 | WAF / API Gateway |

---

## Resumen de decisiones

| Aspecto | Decisión | Razón |
|---|---|---|
| Multitenant | Shared DB/Schema | Simple, escalable, no-schizophrenia |
| Tenant resolve | JWT + Interceptor | Stateless, escalable, bot-friendly |
| Filtros | Queries explícitas | Seguro (no olvidar WHERE tenant_id) |
| Auth | JWT simple | Perfecto para bot + REST |
| Bot | 1 bot multitenant | Operativamente simple |
| Mensajería | Interfaz MessagingChannel | Fácil agregar WhatsApp |
| Estado conversación | BD + ThreadLocal | Persistencia + rendimiento |
| BD | MariaDB (actual) | Ya está configurada |
| Notificaciones | Spring @Scheduled | Simple, suficiente para MVP |
| Reportes | Aplazados Phase 8+ | Focus en MVP primero |
| Seguridad | JWT + HTTPS | MVP-sufficient |

---

## Próximas decisiones (fases 7+)

- [ ] ¿Timezone handling? (Usar ZonedDateTime en Fase 8+)
- [ ] ¿Integración de calendario Google? (Fase 13)
- [ ] ¿Modelos de negocio de precios? (Fase 9)

---

**Documento versión:** 1.7  
**Próxima revisión:** Después de completar Fase 7 (validación con usuarios reales)
