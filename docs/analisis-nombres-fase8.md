# Fase 8 — Análisis y plan de normalización de nombres

> **ESTADO: IMPLEMENTADO (7 de agosto de 2026).** Sub-fases 8.1-8.7 ejecutadas; 78 tests OK. Este documento queda como análisis/registro de las decisiones.

## Objetivo

Eliminar los problemas de nomenclatura del proyecto para que clases, métodos y directorios sigan las convenciones de Java (camelCase en clases/métodos/variables; **minúsculas en paquetes**), sin conflictos con nombres de la API de Java/Spring Boot y sin obstáculos para búsquedas/refactors con expresiones regulares.

## Criterios aplicados

1. **Clases/métodos/campos → camelCase.**
2. **Paquetes/directorios → minúsculas completas** (convención Java; un segmento de paquete con mayúscula o `_` rompe patrones `com\.mr\.sb\.beauty_room\.[a-z]+` y confunde a herramientas de rename).
3. **Sin colisión** con clases de `java.*` y de Spring (`org.springframework.*`, `jakarta.*`).
4. **Sin typos** que rompan regex (`Schedule` vs `Shedule`, `Stylist` vs `Stylyst`).
5. **Transparencia de BD:** el refactor de campos JPA no debe cambiar ninguna columna (ver §4).

---

## 1. Directorios/paquetes

Paquete base `com.mr.sb.beauty_room` con segmentos de mayúsculas/minúsculas mezclados:

| Actual | Problema | Propuesto |
|---|---|---|
| `Controllers/` | mayúscula | `controllers/` |
| `Services/` | mayúscula | `services/` |
| `Security/` | mayúscula | `security/` |
| `Config/` | mayúscula | `config/` |
| `Exceptions/` | mayúscula | `exceptions/` |
| `Auth/` | mayúscula + **vacío** | eliminar (solo contiene `Auth/Tables`, ver §5) |
| `DTOS/` | acrónimo en mayúsculas | `dto/` |
| `DTOS/Auth/` | mayúscula dentro del paquete | `dto/auth/` |
| `DTOS/blocked_slot/` | snake_case en paquete | `dto/blockedslot/` |
| `DTOS/stylist_room/` | snake_case en paquete | `dto/stylistroom/` |
| `Services/Auth/` | mayúscula | `services/auth/` |
| `Services/implement/` | ok | `services/implement/` |
| `Controllers/panel/` | ok (ya minúscula) | `controllers/panel/` |
| `entities/`, `repository/` | ok | sin cambios |

Rango de impacto: **todos los archivos Java** (132 main + 15 test) actualizan `package`/`import`. Es mecánico → usar el rename de paquete del IDE y commit separado. No hay referencias a paquetes fuera de `.java` (verificado en `pom.xml`, `application*.properties`, templates, `.env.example`).

> Decisión: unificar en minúsculas todos los paquetes (no solo `DTOS`) para no dejar media normalización. Alternativa mínima (más conservadora): solo renombrar `DTOS`→`dto`, `DTOS/blocked_slot`→`dto/blockedslot`, `DTOS/stylist_room`→`dto/stylistroom` y `DTOS/Auth`→`dto/auth`, dejando `Controllers`/`Services`/etc. como están. Recomendada: la completa.

## 2. Conflictos con la API de Java/Spring

### 2.1 `entities.Service` → `SalonService` (crítico)

Colisiona con `org.springframework.stereotype.Service`. Fuerza el FQN `@org.springframework.stereotype.Service` en `TelegramUpdateHandler.java:48` y `AvailabilityServiceImplement.java:25`. Con `SalonService` ambos pasan a `@Service` normal.

Impacto (8 archivos importan `entities.Service`): `ServiceRepository`, `AvailabilityServiceImplement`, `ServiceServiceImplement`, `TelegramUpdateHandler` (main) y `AppointmentRepositoryTests`, `ReminderServiceImplementTest`, `ServiceServiceImplementTest`, `TelegramUpdateHandlerTest` (test).

Rename completo del cluster (recomendado, elimina la ambigüedad `Service*Service`):

| Actual | Propuesto |
|---|---|
| `entities/Service` | `entities/SalonService` (mantener `@Table(name = "service")`) |
| `repository/ServiceRepository` | `repository/SalonServiceRepository` |
| `Controllers/ServiceController` | `Controllers/SalonServiceController` (endpoint `/api/service` se mantiene) |
| `Services/IServiceService` | `Services/ISalonService` |
| `Services/implement/ServiceServiceImplement` | `Services/implement/SalonServiceImplement` |
| `DTOS/service/ServiceResponseDto` | `DTOS/service/SalonServiceResponseDto` |
| `DTOS/service/ServiceSaveDto` | `DTOS/service/SalonServiceSaveDto` |

JPQL a actualizar en `SalonServiceRepository`: `FROM Service s` → `FROM SalonService s`.
Tipos de campo a actualizar: `Appointment.service`, `Stylist.services`, `AppointmentResponseDto.service`, `StylistResponseDto.service`.

### 2.2 `entities.User` (bajo riesgo)

Implementa `UserDetails` y podría hacer sombra a `org.springframework.security.core.userdetails.User`. Verificado: **ningún archivo importa la clase de Spring `User`** (solo `UserDetails`), por lo que hoy no hay conflicto. Es un nombre de dominio correcto → **se mantiene**, solo documentar en `DECISION_LOG.md`.

### 2.3 Otros

Sin conflictos adicionales: `Payment`, `Notification`, `Review`, `Appointment`, `Role`, `Tenant`, `Client`, `Stylist` no chocan con clases de `java.*` ni Spring. `Security/` (paquete) no colisiona con `org.springframework.security` (FQN distinto), pero el rename a minúsculas (§1) reduce la confusión visual.

## 3. Nombres de clases

| Actual | Problema | Propuesto |
|---|---|---|
| `Services/implement/StylistSheduleServiceImplement` | typo "Shedule" | `StylistScheduleServiceImplement` (AGENTS.md: hacerlo aquí, en Fase 8, no como parte de otro cambio) |
| `Services/implement/ServiceServiceImplement` | doble "Service" | `SalonServiceImplement` (§2.1) |
| `Controllers/Welcome` | no sigue convención `*Controller` | `Controllers/WelcomeController` |
| `*ServiceImplement` | sufijo no estándar | **Decisión:** mantener (consistente en todo el proyecto). Alternativa opcional: `*ServiceImpl`. Documentar. |

## 4. Campos/métodos snake_case → camelCase

Campos de entidad (Lombok `@Data` genera los getters/setters con el mismo nombre):

| Entidad | Actual | Propuesto |
|---|---|---|
| `Client` | `name_client` (`getName_client`) | `nameClient` (`getNameClient`) |
| `Client` | `telegram_chat_id` (`getTelegram_chat_id`) | `telegramChatId` (`getTelegramChatId`) |
| `Stylist` | `name_stylist` | `nameStylist` |
| `Stylist` | `telegram_chat_id` | `telegramChatId` |
| `Service` | `name_service` | `nameService` |
| `StylistRoom` | `name_room` | `nameRoom` |

**Impacto en BD: NULO.** No hay estrategia de naming personalizada (verificado: sin `PhysicalNamingStrategy` en properties/código) → se usa la default de Spring Boot `SpringPhysicalNamingStrategy`, que convierte `nameClient`→`name_client`, `telegramChatId`→`telegram_chat_id`, etc. Además `Client.telegram_chat_id`/`Stylist.telegram_chat_id` ya tienen `@Column(name="telegram_chat_id")` explícito. La columna resultante es idéntica a la de `V1__init_schema.sql`, así que `ddl-auto=validate` sigue OK y `import.sql`/`V2__seed_data.sql` (usan columnas, no campos) no cambian.

**JPQL a actualizar (referencia al campo, no a la columna):**
- `ClientRepository.java:21,24` — `c.telegram_chat_id` → `c.telegramChatId`
- `StylistRepository.java:22,25` — `s.telegram_chat_id` → `s.telegramChatId`
- `AppointmentRepository.java:39` — `a.client.telegram_chat_id` → `a.client.telegramChatId`

**Call-sites de getters/setters/builders a actualizar (91 coincidencias):**
- Main: `AuthenticationService` (43,45,62), `ClientServiceImplement` (33,52,67,70,101), `ServiceServiceImplement` (35,54,75,99,135), `StylistServiceImplement` (36,37,56,57,73,76,108,111), `StylistRoomServiceImplement` (43,57,75), `ReviewServiceImplement` (97), `ReportServiceImplement` (85,94), `AppointmentServiceImplement` (280,284,285,295,298,300,335,341), `ReminderServiceImplement` (63,82,88,117,131,136), `TelegramUpdateHandler` (359,383,486,487,1001,1003).
- Test: `AppointmentRepositoryTests`, `TenantIsolationTest`, `ServiceServiceImplementTest`, `ReminderServiceImplementTest`, `TelegramUpdateHandlerTest`, `AppointmentServiceImplementTest`.

Los templates de panel ya usan DTOs camelCase (`${st.telegramChatId}`, `${c.telegramChatId}`) — sin cambios.

## 5. Typos en nombres de métodos y residuos

| Actual | Propuesto | Call-sites |
|---|---|---|
| `IServiceService.findByStylystId` | `findByStylistId` | `ServiceController:89`, `ServiceServiceImplement:64`, test:111 |
| `IServiceService.serviceBelongToStylyst(long stylystId, long serviceId)` | `serviceBelongsToStylist(long stylistId, long serviceId)` | `ServiceServiceImplement:150-152` |
| `ServiceRepository.findServicesStylistId(long id, Long tenantId)` | `findByStylistIdAndTenantId` (además deja de ser un nombre inventado) | `ServiceServiceImplement:66,152`, test:108 |
| `Auth/Tables` | **eliminar** (archivo vacío de 0 bytes en paquete `Auth` vacío) | — |
| `Welcome.findAll()` | `welcome()` (devuelve texto de bienvenida, no una lista) | — |

## 6. Olores menores (documentar, no tocar)

- `Tenant.getTenantId()`: getter custom que devuelve `id` con `@JsonIgnore`. Confuso (propiedad inventada `tenantId` sin campo) pero funcional para derived queries. Dejar documentado.
- `com.mr.sb`: abreviatura del paquete raíz. No se toca (cambio de alto riesgo, sin beneficio).
- `StylistSchedule.@Table` sin nombre → tabla `stylist_schedule` (coincide con V1). OK.

---

## Plan de ejecución (sub-fases de la Fase 8)

Cada sub-fase termina con `./mvnw -DskipTests compile` y, si la DB local está arriba, `./mvnw test -Dtest=...` de los tests afectados.

| # | Sub-fase | Riesgo | Verificación |
|---|---|---|---|
| 8.1 | Renombrar `entities.Service`→`SalonService` + cluster completo (§2.1) | Medio | `./mvnw test -Dtest=ServiceServiceImplementTest,TelegramUpdateHandlerTest,ReminderServiceImplementTest` |
| 8.2 | Campos snake_case→camelCase + JPQL (§4) | Medio (mecánico) | full `./mvnw test` + arrancar app con `DDL_AUTO=validate` |
| 8.3 | Typos: `findByStylystId`, `serviceBelongToStylyst`, `findServicesStylistId`, `StylistSheduleServiceImplement` (§5) | Bajo | `./mvnw test -Dtest=ServiceServiceImplementTest,TelegramUpdateHandlerTest` |
| 8.4 | `Welcome`→`WelcomeController` + `welcome()` (§3) | Bajo | `./mvnw compile` |
| 8.5 | Eliminar `Auth/Tables` y `Auth/` vacío (§5) | Nulo | `git rm` + compile |
| 8.6 | Normalizar paquetes a minúsculas (§1) — rename con IDE, commit aparte | Alto (mecánico, toca todo) | full `./mvnw test` |
| 8.7 | Actualizar `DECISION_LOG.md`, `PROGRESS.md`, sección bot de `AGENTS.md` | — | revisión |

**Dependencias con el resto del plan de refactor del handler:** la sub-fase 8.1 desbloquea el uso de `@Service` sin FQN (beneficia a `TelegramUpdateHandler` y `AvailabilityServiceImplement`). Recomendado ejecutar 8.1-8.2 **antes** o **en paralelo** con las fases 1-7 del refactor del handler para no propagar `name_service`/`getName_service()` a los nuevos componentes. En orden inverso (Fases 1-7 primero), los nombres snake_case se replican en `TelegramViewService`/`TelegramAccountService` y luego hay que volver a tocarlos en 8.2.
