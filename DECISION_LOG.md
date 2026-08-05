# 🏗️ Decisiones Arquitectónicas — Beauty Room MVP

**Versión:** 1.1  
**Fecha:** 5 de agosto de 2026  
**Estado:** ✅ Decisiones 1-4 y 8 IMPLEMENTADAS (Fase 0 completada). Resto pendiente de fases futuras.

> **Nota de implementación (5 de agosto de 2026):** las decisiones 1 (Shared DB/Schema), 2 (resolución por JWT + header), 3 (queries explícitas con tenant_id) y 4 (JWT simple con claim `tenantId`) quedaron implementadas en el código. Detalle de lo hecho y gotchas en [CHECKLIST_FASE_0.md](CHECKLIST_FASE_0.md).

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

### Flujo de primer mensaje:

```
Usuario abre chat y escribe: /start

Bot responde:
  "¿Eres dueño de un salón de belleza?"
  [SI] [NO]

SI → Bot pide código de invitación o teléfono
       Valida contra DB → obtiene tenant_id
       Guarda conversación_state: tenant_id + chat_id

NO → Bot pregunta: "¿Cuál es el teléfono de tu estilista?"
      Busca en DB → obtiene tenant_id
      Muestra servicios de ese tenant
```

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

## Próximas decisiones (fases 3+)

- [ ] ¿Timezone handling? (Usar ZonedDateTime en Phase 5)
- [ ] ¿Pagina automatización de recordatorios? (Phase 5)
- [ ] ¿Integración de calendario Google? (Phase 13)
- [ ] ¿Modelos de negocio de precios? (Phase 9)

---

**Documento versión:** 1.1  
**Próxima revisión:** Después de completar Fase 1
