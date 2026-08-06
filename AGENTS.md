# AGENTS.md — Beauty Room

Backend Spring Boot (Java 21, Boot 3.2.3, MariaDB) para agendamiento de citas en salones de belleza. SaaS multitenant (Fase 0 COMPLETADA) con bot de Telegram (Fases 2-3 COMPLETADAS). Toda la documentación del proyecto está en español en la raíz.

## Comandos
- Ejecutar la app: `./mvnw spring-boot:run` (desde la raíz)
- Compilar: `./mvnw -DskipTests compile`
- Tests: `./mvnw test` (para un test solo: `./mvnw test -Dtest=NombreClase`)
- Levantar MariaDB: `docker-compose up -d` desde `src/main/resources/`
- No hay lint ni formatter configurado. La verificación es compile + tests.

## Base de datos (gotchas)
- Local: db `beauty_room`, user `dev` / `dev123` en `localhost:3306` (definidos en `docker-compose.yml`).
- `spring.jpa.hibernate.ddl-auto=create-drop`: el esquema se recrea en CADA arranque y `import.sql` se re-ejecuta (`spring.sql.init.mode=always`). Cualquier dato creado a mano se pierde al reiniciar.
- Los tests `@DataJpaTest` y `@SpringBootTest` usan la MariaDB real (`@AutoConfigureTestDatabase(replace = NONE)`); necesitan la DB corriendo. Los tests de Services usan Mockito y no la requieren.
- `import.sql` siembra 3 tenants (ids 1-3: Salón María `salon-maria-001`, Estilos Ana `estilos-ana-001`, Beauty Room Pro `beauty-room-pro-001`) y users con ids 1-4 (hash BCrypt de `password`); `Stylist`/`Client` reutilizan el mismo `id` que `users`. john@example.com y alice@example.com → tenant 1; jane@example.com y bob@example.com → tenant 2.

## Arquitectura
- Paquete base `com.mr.sb.beauty_room`: `Controllers/`, `Services/` (interfaces `I*`), `Services/implement/` (`*ServiceImplement`), `repository/`, `entities/`, `DTOS/`, `Security/`, `Exceptions/`.
- Patrón: Controller → interfaz `IService` → `*ServiceImplement` → Repository. Nuevas features siguen este patrón.
- `User` es la entidad base con herencia JOINED; `Stylist` y `Client` extienden `User`. `Role` es enum (CLIENT, STYLIST, ADMIN).
- OJO con el typo existente: el servicio de horarios es `StylistSheduleServiceImplement` (sin "c") aunque la interfaz es `IStylistScheduleService`. No lo "corrijas" como parte de otro cambio.
- Los DTOs mezclan nombres: entrada en snake_case (`id_client`, `id_stylist`, `name_service`) pero respuesta en camelCase (`idService`). Respeta el estilo del DTO que estés tocando.
- Seguridad: JWT (jjwt 0.11.5) vía `JwtAuthenticationFilter`; endpoints públicos listados en `SecurityConfig`. El secret está hardcodeado en `application.properties`.

## Multitenant (Fase 0 — COMPLETADA)
- Entidad `Tenant` + enums `TenantPlan`/`TenantStatus`. Toda entidad de negocio tiene `@ManyToOne(fetch = LAZY) Tenant tenant` con `@JsonIgnore` (NO `@JsonBackReference`).
- Resolución por request: `TenantInterceptor` (en `Security/`) lee el claim `tenantId` del JWT; si no hay JWT, usa el header `X-Tenant-ID`. Guarda en ThreadLocal; `getCurrentTenantIdOrThrow()` lanza `TenantNotResolvedException` (manejada como 400). Registrado en `Config/WebConfig` para `/api/**`.
- Services y repositorios filtran por tenant SIEMPRE. Los tests de aislamiento: `TenantIsolationTest` (usa `setCurrentTenantId`/`clear`) y `TenantJwtFlowTest`.
- `jackson-datatype-hibernate6` está en `pom.xml` para serializar proxies lazy sin errores.
- OJO con queries derivadas: `NotificationRepository` usa `findByTenantIdAndUserIdOrderByCreatedAtDesc` (el nombre invertido no compila como query derivada).
- `IBlockedSlotService.isSlotBlocked(Long tenantId, Long stylistId, ...)` recibe tenantId como primer parámetro.

## Contexto de producto (importante)
- Proyecto en fase MVP. Multitenant (Fase 0) COMPLETADO. API REST validada (Fase 1) COMPLETADA. Bot Telegram esqueleto (Fase 2) COMPLETADA: webhook `/api/telegram/webhook` (público), dependencias `telegrambots-springboot-webhook-starter:7.11.0` + `telegrambots-client:7.11.0` (NO el `telegrambots-spring-boot-starter` clásico, que es de Boot 2.7), `IMessagingChannel`/`TelegramChannel`, entidad `ConversationState`, deep linking `?start=tenantKey` para resolver tenant. FSM de agendamiento (Fase 3) COMPLETADA: servicio → fecha → hora → confirmar, `InlineKeyboardMarkup`, "Mis citas", "Cancelar cita", cliente auto-creado, manejo de 409. 41 tests OK.
- El bot se registra solo si `telegram.bot.token` está configurado (beans `@ConditionalOnProperty`). Config en `application.properties`: `telegram.bot.token`, `telegram.bot.username`, `telegram.bot.path`, `telegram.bot.webhook-url`.
- PRÓXIMA TAREA = Fase 4: seguir `plan.md` (flujo del estilista desde Telegram: ver agenda del día/semana, bloquear horarios, marcar completada/no-show, cancelar con notificación al cliente).
- El FSM del bot vive en `Services/implement/TelegramUpdateHandler`. OJO: la clase usa `@org.springframework.stereotype.Service` (FQN) porque `entities.Service` choca con el import; estados en `ConversationState.currentStep` y datos JSON en `.data`; callbacks prefijados `SERVICE:`/`DATE:`/`TIME:`/`CANCEL_APPT:`; fuera de HTTP los services requieren `TenantInterceptor.setCurrentTenantId(tenantId)` + `clear()` (ver `ensureClient`/`confirmAppointment`).
- Roadmap en `plan.md`; estado en `PROGRESS.md`; decisiones de arquitectura en `DECISION_LOG.md`. Léelos antes de cambios de arquitectura.
- La skill `.opencode/skills/analista-senior/AS.md` sirve para auditorías/diagnóstico del estado del proyecto.
- Trabajo activo en la rama `appmod/java-upgrade-20251218222941` (no en `main`).
