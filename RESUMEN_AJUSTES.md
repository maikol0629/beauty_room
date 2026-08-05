# 📋 Resumen de ajuste del Plan — 5 de agosto de 2026

## ¿Qué se hizo?

Se realizó una auditoría completa del proyecto Beauty Room y se ajustó el roadmap original (`plan.md`) para reflejar el estado actual de implementación.

---

## 📊 Estado del proyecto después de auditoría

### ✅ LO QUE YA ESTÁ IMPLEMENTADO (80% del backend)

**Entidades (BD):**
- ✅ `Stylist` (estilista con rol, teléfono, nombre)
- ✅ `Client` (cliente con nombre, teléfono, email)
- ✅ `Service` (servicios con duración, precio, descripción)
- ✅ `Appointment` (citas con estado: PENDING, CONFIRMED, CANCELLED, COMPLETED)
- ✅ `StylistSchedule` (horarios de trabajo del estilista)
- ✅ `BlockedSlot` (bloqueos puntuales de tiempo)
- ✅ `Notification` (notificaciones para clientes)
- ✅ `Payment` (pagos con estado)
- ✅ `Review` (reseñas de clientes)
- ✅ `StylistRoom` (salón/espacio de trabajo)
- ✅ `User` (base para autenticación)

**Autenticación & Seguridad:**
- ✅ Spring Security integrado
- ✅ JWT implementado (io.jsonwebtoken 0.11.5)
- ✅ AuthController con login
- ✅ JwtProvider para generar tokens

**APIs REST:**
- ✅ 10+ Controllers CRUD (`AppointmentController`, `ClientController`, `ServiceController`, `StylistController`, etc.)
- ✅ Global Exception Handler
- ✅ DTOs para transporte de datos

**Base de Datos:**
- ✅ MariaDB conectado (no PostgreSQL como decía el plan original)
- ✅ Spring Data JPA para ORM
- ✅ Repositorios para todas las entidades

**Testing:**
- ✅ Tests unitarios para Repository (AppointmentRepositoryTests)
- ✅ Tests unitarios para Services (AppointmentServiceImplementTest, ServiceServiceImplementTest)

---

### 🔴 LO QUE FALTA — BLOQUEANTES CRÍTICOS

1. **Multitenant (Fase 0) — ✅ COMPLETADA (5 de agosto de 2026)**
   - ✅ Entidad `Tenant` existe (con plan, status, tenantKey)
   - ✅ `tenant_id` en todas las tablas (SECURITY RISK resuelto)
   - ✅ `TenantInterceptor` para resolver tenant por request (JWT claim `tenantId` o header `X-Tenant-ID`)
   - ✅ Repositorios y services filtran por tenant (`getCurrentTenantIdOrThrow()`)
   - ✅ JWT incluye `tenantId`
   - ✅ Seed de 3 tenants + tests de aislamiento (14 tests OK, verificación manual cross-tenant vía API)

2. **Bot de Telegram (Fases 2-3) — CORE DEL PRODUCTO**
   - ❌ Cero código de integración Telegram
   - ❌ No existe endpoint `/telegram/webhook`
   - ❌ No existe interfaz `MessagingChannel`
   - ❌ No existe FSM conversacional
   - **Impacto:** Producto no es funcional sin el bot

3. **Lógica de disponibilidad (Fase 1) — NECESARIO PARA BOT**
   - ❌ Método `calculateAvailableSlots()` no verificado/implementado
   - ❌ Validación de no-doble-booking no verificada
   - ❌ Endpoint GET `/api/appointments/slots` probablemente no existe
   - **Impacto:** Bot no puede mostrar horarios disponibles

4. **Campo en Client (Fase 1)**
   - ❌ Campo `telegram_chat_id` en `Client` no existe
   - **Impacto:** No se puede identificar clientes que escriben al bot

---

### 🟡 PARCIALMENTE COMPLETADO

- Endpoints CRUD existen pero no fueron validados vía Postman
- Tests existen pero no cubren flujos E2E
- Documentación en Swagger no existe aún

---

## 📝 Cambios realizados al `plan.md`

1. **Agregado:** Estado actual del proyecto en cada Fase (✅, ❌, 🟡)
2. **Agregado:** Sección de "CRÍTICO: Próximos pasos recomendados" con timeline
3. **Actualizado:** Stack (PostgreSQL → MariaDB)
4. **Actualizado:** Descripción de lo completado vs pendiente
5. **Actualizado:** Tabla resumen con estados

---

## 📋 Archivos de guía creados

### 1. `CHECKLIST_FASE_0.md`
**Guía paso-a-paso para implementar Multitenant:**
- ✅ Crear entidad `Tenant`
- ✅ Agregar `tenant_id` a todas las tablas
- ✅ Implementar `TenantInterceptor`
- ✅ Actualizar repositorios con filtros
- ✅ Actualizar Services
- ✅ Actualizar JWT para incluir `tenantId`
- ✅ Crear seed de 2-3 tenants
- ✅ Tests de aislamiento

**Incluye:** Código de ejemplo, queries SQL, tests, riesgos y cómo mitigarlos.

**Timeline:** 1-2 semanas de implementación.

---

### 2. `CHECKLIST_FASE_1.md`
**Guía para verificar que API REST es robusta:**
- ✅ Collection de Postman para probar endpoints
- ✅ Implementar `calculateAvailableSlots()`
- ✅ Implementar validación de no-doble-booking
- ✅ Agregar campo `telegram_chat_id` a Client
- ✅ Tests end-to-end
- ✅ Swagger documentation

**Incluye:** Postman collection JSON, código de ejemplo, queries, tests E2E, documentación.

**Timeline:** 1 semana de implementación (después de Fase 0).

---

## 🚀 Recomendación de orden de trabajo

### Semana 1-2: **FASE 0 — Multitenant (CRÍTICO)**
```
1. Crear entidad Tenant
2. Agregar tenant_id a 10 tablas
3. Implementar TenantInterceptor
4. Actualizar 10+ Services
5. Actualizar repositorios
6. Tests de aislamiento
```

**¿Por qué PRIMERO?** Sin esto, cada feature que agregues genera deuda técnica. Es el cimiento.

---

### Semana 3: **FASE 1 — Verificar API REST**
```
1. Implementar calculateAvailableSlots()
2. Verificar no-doble-booking
3. Agregar telegram_chat_id a Client
4. Probar endpoints con Postman
5. Crear tests E2E
6. Documentar en Swagger
```

**¿Por qué?** Necesitas backend robusto antes de construir bot.

---

### Semana 4-5: **FASE 2-3 — Bot Telegram (MVP)**
```
1. Setup bot Telegram + webhook
2. Crear interfaz MessagingChannel
3. Implementar FSM conversacional
4. Flujo: agendar cita desde Telegram
5. Recordatorios básicos
```

**¿Por qué?** Este es tu MVP validable con usuarios reales.

---

## ⚠️ Riesgos actuales

| Riesgo | Severidad | Cómo se mitiga |
|---|---|---|
| ~~Sin multitenant → un usuario ve datos de otro~~ | ✅ RESUELTO | Fase 0 completada, aislamiento verificado |
| Bot no existe → no hay producto | 🔴 CRÍTICA | Timeline de 4-5 semanas realista |
| Endpoints no validados → discovery de bugs tarde | 🟡 Alta | Usar CHECKLIST_FASE_1 + Postman |
| ~~JWT sin tenant_id → fácil atacar otros tenants~~ | ✅ RESUELTO | JWT incluye `tenantId` (FASE 0) |
| Sin calculateAvailableSlots() → bot puede double-book | 🔴 CRÍTICA | Verificar/implementar (FASE 1) |

---

## 💡 Próximos pasos inmediatos

1. **Leer** `CHECKLIST_FASE_1.md` completo
2. **Validar** los endpoints multitenant con Postman (Fase 1)
3. **Verificar** `calculateAvailableSlots()` y no-doble-booking vía API
4. **Agregar** `telegram_chat_id` a `Client`

---

## 📞 Preguntas para aclarar antes de empezar

- [ ] ¿Necesitas migración de datos actuales? (Si usaste DB localmente, ¿hay clientes reales?)
- [ ] ¿El AuthController usa email+password? ¿O es OAuth/Social?
- [ ] ¿Está desplegado en algún servidor o es solo local?
- [ ] ¿Tienes estilistas/clientes reales para testear después de MVP?

---

## 📚 Archivos clave del proyecto

- [plan.md](plan.md) — Roadmap actualizado
- [CHECKLIST_FASE_0.md](CHECKLIST_FASE_0.md) — Guía implementación multitenant (COMPLETADA, con resumen de implementación)
- [CHECKLIST_FASE_1.md](CHECKLIST_FASE_1.md) — Guía verificación API REST
- [pom.xml](pom.xml) — Dependencias (Java 21, Spring Boot 3.2.3, MariaDB)
- [src/main/resources/application.properties](src/main/resources/application.properties) — Config (MariaDB localhost)

---

**Última actualización:** 5 de agosto de 2026  
**Próxima auditoría recomendada:** Después de completar FASE 0
