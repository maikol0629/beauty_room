# ✅ Checklist Fase 6 — Panel de administración y autonomía del estilista

**Objetivo:** dar al estilista control directo sobre servicios, precios, horarios y el canal público de agenda, sin depender de soporte técnico ni del bot. El producto deja de ser un piloto con soporte manual y pasa a ser una herramienta autónoma.

**Timeline estimado:** 1 semana
**Bloqueado por:** Fases 0-5 (multitenant + API + bot + recordatorios) ⛔ → ✅ completadas
**Status:** 🟢 **COMPLETADA** (6 de agosto de 2026)

---

## 📌 Qué se implementó

### 1. Login del estilista/tenant por sesión
- `SecurityConfig` ahora tiene **2 cadenas**:
  - `@Order(1)` — Panel: `securityMatcher("/panel/**", "/css/**", "/js/**", "/images/**", "/favicon.ico")`, form login en `/panel/login` (default `/panel`), roles `STYLIST`/`ADMIN`, logout `/panel/logout`, sesión `IF_REQUIRED`, **CSRF activo**.
  - `@Order(2)` — API stateless JWT original, sin cambios (permitAll + `JwtAuthenticationFilter`).
- `PanelController` sirve el login (`GET /panel/login`), el home (`GET /panel` con counts y citas de hoy) y el logout vía Spring Security.
- El usuario en sesión queda **detached** → `PanelTenantHelper` lo recarga con `UserRepository.findById(userId)` antes de acceder a `tenant`/`stylist` (evita `LazyInitializationException`). Expone `currentTenantId()`/`currentTenant()`/`withTenant(Supplier)`.

### 2. CRUD visual de servicios y precios
- `PanelServiceController` (`/panel/services`): listar, nuevo, editar, crear, actualizar y eliminar servicios del tenant.
- Reutiliza `IServiceService` (la interfaz del dominio) — cero lógica duplicada; filtra por tenant siempre.

### 3. CRUD visual de horarios y bloqueos recurrentes
- `PanelScheduleController` (`/panel/schedules`): listar, nuevo, crear, eliminar horarios del estilista.
- `PanelBlockedController` (`/panel/blocked`): listar, nuevo, crear, eliminar bloqueos (fecha inicio/fin + motivo).

### 4. Link público de agenda + QR
- `IQrCodeService`/`QrCodeServiceImplement` (nuevas deps `com.google.zxing:core` + `javase` 3.5.3):
  - `generatePng(content, width, height)` — genera un PNG con zxing.
  - `buildPublicAgendaUrl(tenantKey)` — construye `https://t.me/<telegram.bot.username>?start=<tenantKey>` (devuelve null si el username está vacío).
- `GET /panel/public` muestra el link + QR para compartir en redes.
- `GET /panel/qr.png` sirve el QR como `image/png`.

### 5. Vista de citas y clientes
- `PanelAppointmentController` (`/panel/appointments`): listado con filtros (`stylistId`, `status`, `from`, `to`) vía `findAppointmentsByFilters`, y acciones `confirm`/`complete`/`cancel` (POST con CSRF).
- `PanelClientController` (`/panel/clients`): listado de clientes del tenant.
- Los templates toleran citas con `status` **nulo** (las citas del seed no tienen status) — se muestra `PENDING` por defecto y las acciones validan null.

### 6. Edición de estilista (telegram_chat_id)
- `PanelStylistController` (`/panel/stylists`): listar y editar. El formulario permite setear el `telegram_chat_id` del estilista (campo nuevo en `StylistSaveDto`/`StylistResponseDto`; `StylistServiceImplement` persiste el valor).
- Quita la dependencia de SQL manual para asociar chat de Telegram ↔ estilista.

### 7. Frontend
- Templates Thymeleaf en `resources/templates/panel/`: `fragments.html` (navbar + flash), `login.html`, `home.html`, `public.html`, `services/list.html` + `form.html`, `schedules/list.html` + `form.html`, `blocked/list.html` + `form.html`, `appointments/list.html`, `clients/list.html`, `stylists/list.html` + `form.html`.
- CSS: `static/css/panel.css`. Cada POST de formulario incluye `_csrf` (CSRF activo).

---

## 📌 Archivos tocados

| Archivo | Cambio |
|---|---|
| `pom.xml` | + `spring-boot-starter-thymeleaf`, + `zxing:core`/`javase` 3.5.3 |
| `Security/SecurityConfig.java` | 2 cadenas: panel (sesión) @Order(1) + API JWT @Order(2) |
| `Controllers/panel/PanelController.java` | login/home/public/qr.png |
| `Controllers/panel/PanelTenantHelper.java` | tenant del usuario logueado (recarga desde BD) |
| `Controllers/panel/PanelServiceController.java` | CRUD servicios |
| `Controllers/panel/PanelScheduleController.java` | CRUD horarios |
| `Controllers/panel/PanelBlockedController.java` | CRUD bloqueos |
| `Controllers/panel/PanelAppointmentController.java` | listado con filtros + confirm/complete/cancel |
| `Controllers/panel/PanelClientController.java` | listado clientes |
| `Controllers/panel/PanelStylistController.java` | listado + edición estilista |
| `Services/IQrCodeService.java` + `implement/QrCodeServiceImplement.java` | QR PNG + link público |
| `DTOS/stylist/StylistSaveDto.java`/`StylistResponseDto.java` + `Services/implement/StylistServiceImplement.java` | campo `telegramChatId` |
| `resources/templates/panel/**` | 14 templates Thymeleaf |
| `static/css/panel.css` | estilos del panel |
| `test/.../Controllers/PanelSecurityTest.java` | seguridad + flujos del panel (5 tests) |
| `test/.../Services/implement/QrCodeServiceTest.java` | QR (3 tests) |

---

## 🧪 Cómo probar

1. Levantar MariaDB (`docker-compose up -d`) y correr la app (`./mvnw spring-boot:run`). El seed deja `john@example.com`/`password` = STYLIST (tenant 1) y `alice@example.com`/`password` = CLIENT.
2. Ir a `http://localhost:8080/panel/login`, loguearte como `john@example.com`.
3. Crear/editar/borrar servicios en `/panel/services`, horarios en `/panel/schedules`, bloqueos en `/panel/blocked`.
4. Ver citas en `/panel/appointments` (filtros + confirmar/completar/cancelar) y clientes en `/panel/clients`.
5. `/panel/public` y `/panel/qr.png` muestran el link de agenda + QR del tenant.
6. Loguearte como `alice@example.com` → **403** (rol CLIENT no accede al panel).
7. Tests: `./mvnw test -Dtest=PanelSecurityTest,QrCodeServiceTest` → suite total **76 tests, 0 fallos**.

---

## ⚠️ Gotchas

- **CSRF activo en el panel:** cada form POST (y los botones de acciones) debe incluir el token `_csrf` (`<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>`).
- **Usuario detached en sesión:** el `principal` de la sesión NO es el `User` con la relación `tenant` cargada → usar siempre `PanelTenantHelper.currentTenantId()` (recarga desde BD). No accedas a `user.getTenant()` directamente.
- **Citas del seed sin `status`:** los templates usan `a.status != null ? ...` para mostrar la badge y habilitar acciones; no asumas `status` nunca nulo.
- **`stylist_schedule`/nombres con typo:** el servicio de horarios sigue siendo `StylistSheduleServiceImplement` (sin "c") — no lo renombres.
- **`QrCodeServiceImplement.buildPublicAgendaUrl`** devuelve null si `telegram.bot.username` está vacío (sin bot configurado) — `/panel/public` y `/panel/qr.png` deben tolerar null.
- **Cualquier endpoint del panel filtra por tenant** (los `IService` existentes lo hacen vía `TenantInterceptor` + ThreadLocal, que en el panel se setea con `PanelTenantHelper.withTenant(...)`).

---

## ▶️ Próximo paso: Fase 7 (validación con usuarios reales)

- Reclutar 10-15 estilistas, onboarding semi-manual con el panel ya disponible, medir citas/semana/estilista, canal de feedback directo. Es más de negocio que de código.
