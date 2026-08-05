## Beauty Room API

Backend Spring Boot para agendamiento de citas en salones de belleza (SaaS multitenant con bot de Telegram).

**Stack:** Java 21 • Spring Boot 3.2.3 • MariaDB • JWT • Spring Security

---

## 📊 Estado Actual del Proyecto

**Progreso general:** 70% completado

✅ **Backend creado (100%):** 10+ entidades, 10+ Controllers, autenticación JWT  
✅ **Multitenant (100%):** Fase 0 completada — entidad Tenant, aislamiento por tenant verificado  
✅ **API REST validada (100%):** Fase 1 completada — slots, no-doble-booking 409, `telegram_chat_id`, Swagger, Postman collection, tests E2E  
❌ **Bot Telegram (0%):** BLOQUEANTE — core del producto  

**Próximas 12 semanas:** Focus en Fases 2-3 (bot MVP)

---

## 🚀 Quick Start

### Requisitos

- Java 21
- Maven 3.x
- Docker y Docker Compose (para MariaDB)

---

## 📖 Documentación del Roadmap

**CRÍTICO:** Lee estos documentos EN ESTE ORDEN antes de empezar a programar:

1. **[plan.md](plan.md)** — Roadmap completo (13 fases, actualizado con estado real)
2. **[PROGRESS.md](PROGRESS.md)** — Dashboard visual de progreso
3. **[CHECKLIST_FASE_0.md](CHECKLIST_FASE_0.md)** — Guía de la implementación multitenant (COMPLETADA)
4. **[CHECKLIST_FASE_1.md](CHECKLIST_FASE_1.md)** — Guía de verificación API REST (COMPLETADA)
5. **[DECISION_LOG.md](DECISION_LOG.md)** — Decisiones arquitectónicas explicadas
6. **[RESUMEN_AJUSTES.md](RESUMEN_AJUSTES.md)** — Qué cambió en el plan original

---

## ✅ API REST validada (Fase 1 — COMPLETADA)

El backend quedó validado y documentado el 5 de agosto de 2026:

- **Disponibilidad:** `GET /api/appointment/slots?stylistId=X&serviceId=Y&date=YYYY-MM-DD` (público, header `X-Tenant-ID`) devuelve las franjas libres de 30 min.
- **No-doble-booking:** `POST /api/appointment/save` devuelve **409 Conflict** si la franja está ocupada o bloqueada; las citas `CANCELLED`/`REJECTED` no bloquean el slot.
- **`telegram_chat_id`:** nuevo campo en `Client` (entity + DTOs + registro), con `findByTelegramChatId` y `findByTelegramChatIdAndTenantId` en el repositorio.
- **Swagger/OpenAPI:** UI en `http://localhost:8080/swagger-ui.html` con esquema Bearer JWT y `@Tag`/`@Operation` en los controllers.
- **Postman:** collection `beauty_room_MVP.postman_collection.json` en la raíz.
- **Tests:** 16 tests verdes (incluye `AppointmentControllerE2ETest`).

---

## 🚨 Lo que FALTA (Bloqueantes)

### 1. FASE 2-3: Bot de Telegram (❌ 0% completada)

El core del producto — agendamiento vía Telegram bot.

**Status:** Bloqueante de MVP.  
**Timeline:** 2-3 semanas (después de Fase 1)

---

## ✅ Multitenant (Fase 0 — COMPLETADA)

El proyecto **soporta multitenant** desde el 5 de agosto de 2026:

- Entidad `Tenant` con plan (TRIAL/BASIC/PREMIUM) y status (ACTIVE/SUSPENDED/CANCELLED).
- `tenant_id` en todas las entidades de negocio.
- Resolución de tenant por request: claim `tenantId` del JWT (login/registro) o header `X-Tenant-ID` (endpoints públicos).
- Repositorios y services filtran por tenant; acceso a datos de otro tenant devuelve `404` o `[]`.
- Seed de 3 tenants en `import.sql`.
- 14 tests verdes (incluye `TenantIsolationTest` y `TenantJwtFlowTest`).

**Cómo usar el header en endpoints públicos:**
```bash
curl http://localhost:8080/api/stylist/public -H 'X-Tenant-ID: 1'
```

---

## ✅ Cómo continuar (Fase 2)

```bash
# 1. Leer la documentación (30-60 min)
# Orden: plan.md → PROGRESS.md → CHECKLIST_FASE_1.md (hecha) → plan.md Fase 2

# 2. Validar la API con Postman (opcional, collection ya creada)
# Importar beauty_room_MVP.postman_collection.json

# 3. Fase 2: Bot Telegram (BotFather, webhook, FSM)
```

---

### Cómo ejecutar

1. Levantar la base de datos (desde la carpeta `src/main/resources`):

	 ```bash
	 docker-compose up -d
	 ```

2. Iniciar la aplicación (desde la raíz del proyecto):

	 ```bash
	 ./mvnw spring-boot:run
	 ```

3. La API quedará disponible en:

- `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html` (si configuraste SpringDoc)

---

## Autenticación y roles

El sistema usa JWT y maneja tres tipos de usuarios:

- `CLIENT`
- `STYLIST`
- `ADMIN`

Flujo básico:

1. Registro de usuario (client o stylist) vía endpoints de `/api/auth`.
2. Inicio de sesión para obtener un `accessToken` JWT.
3. El token debe enviarse en el header `Authorization: Bearer <token>` para consumir el resto de endpoints protegidos.

### Endpoints de Auth

| Método | Path                         | Descripción                          |
|--------|------------------------------|--------------------------------------|
| POST   | `/api/auth/register/client`  | Registrar cliente y devolver token   |
| POST   | `/api/auth/register/stylist` | Registrar estilista y devolver token |
| POST   | `/api/auth/authenticate`     | Autenticar usuario y devolver token  |

Body para registro/autenticación (`RegisterRequest` / `AuthenticationRequest`):

```json
{
	"name": "Nombre usuario",      // solo en registro
	"email": "user@example.com",
	"password": "password-seguro"
}
```

---

## Endpoints principales

### Client (`/api/client`)

- Registro de cliente (público):

| Método | Path                  | Descripción                 |
|--------|-----------------------|-----------------------------|
| POST   | `/api/client/register`| Registrar nuevo cliente     |

- Operaciones protegidas:

| Método | Path               | Rol requerido | Descripción                    |
|--------|--------------------|---------------|--------------------------------|
| GET    | `/api/client/{id}` | `CLIENT`      | Obtener datos del cliente logueado (por id) |
| GET    | `/api/client`      | `STYLIST`     | Listar todos los clientes      |
| PUT    | `/api/client/{id}` | `CLIENT`      | Actualizar datos del cliente   |

Los controladores trabajan con DTOs:

- `ClientSaveDto` (entrada)
- `ClientResponseDto` (salida)

### Stylist (`/api/stylist`)

- Registro de estilista (solo ADMIN):

| Método | Path                    | Rol requerido | Descripción                   |
|--------|-------------------------|---------------|-------------------------------|
| POST   | `/api/stylist/register` | `ADMIN`       | Registrar nuevo estilista     |

- Operaciones protegidas:

| Método | Path                 | Rol requerido             | Descripción                  |
|--------|----------------------|---------------------------|------------------------------|
| GET    | `/api/stylist/{id}` | `STYLIST` o `ADMIN`       | Obtener estilista por id     |
| GET    | `/api/stylist`      | `STYLIST` o `ADMIN`       | Listar todos los estilistas  |
| PUT    | `/api/stylist/{id}` | `STYLIST` o `ADMIN`       | Actualizar datos del estilista |

Los controladores trabajan con DTOs:

- `StylistSaveDto` (entrada)
- `StylistResponseDto` (salida)

### Service (`/api/service`)

| Método | Path                         | Descripción                                        |
|--------|------------------------------|----------------------------------------------------|
| GET    | `/api/service/findAll`       | Listar todos los servicios                         |
| GET    | `/api/service/find/{id}`     | Obtener servicio por id                            |
| GET    | `/api/service/findByStylistId/{id}` | Listar servicios de un estilista específico |
| POST   | `/api/service/save`          | Crear servicio para un estilista                   |
| PUT    | `/api/service/update/{id}`   | Actualizar servicio existente                      |
| DELETE | `/api/service/delete/{id}`   | Eliminar servicio                                  |

Payload de creación/actualización (`ServiceSaveDto`):

```json
{
	"name": "Corte de cabello",
	"description": "Corte clásico",
	"price": 15000,
	"duration": 30,
	"id_stylist": 1
}
```

### Appointment (`/api/appointment`)

| Método | Path                              | Descripción                                      |
|--------|-----------------------------------|--------------------------------------------------|
| GET    | `/api/appointment/findById/{id}`  | Obtener cita por id                              |
| GET    | `/api/appointment/findByStylistId/{id}` | Listar citas de un estilista               |
| GET    | `/api/appointment/findByClientId/{id}`  | Listar citas de un cliente                  |
| POST   | `/api/appointment/save`           | Crear una nueva cita (aplica validaciones)       |
| PUT    | `/api/appointment/update/{id}`    | Actualizar una cita existente                     |
| DELETE | `/api/appointment/delete/{id}`    | Eliminar una cita                                 |

Payload de creación/actualización (`AppointmentSaveDto`):

```json
{
	"startDate": "2025-01-10T10:00:00",
	"id_client": 1,
	"id_stylist": 1,
	"id_service": 1
}
```


La lógica de negocio valida:

- Que la cita sea al menos 1 hora en el futuro.
- Que esté dentro del horario laboral (07:00–22:00).
- Que el estilista tenga disponibilidad en su agenda y no exista solapamiento.

---

## Errores y validaciones

La API usa Bean Validation (`@Valid`) y un `GlobalExceptionHandler` para devolver errores estructurados.

- Errores de validación devuelven **400 Bad Request** con detalle de campos.
- Errores de negocio (como estilista no encontrado al crear un servicio) pueden lanzar `IllegalArgumentException`, que también se devuelve como **400** con mensaje claro.

Para probar fácilmente la API, se recomienda usar Swagger UI (`/swagger-ui.html`) o herramientas como Postman/Insomnia.