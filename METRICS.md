# 📈 Métricas y KPIs — Beauty Room

**Documento:** Seguimiento de salud del proyecto  
**Actualizado:** 5 de agosto de 2026  
**Próxima revisión:** Fin de Fase 2-3

---

## 🎯 KPIs Principales (MVP)

### Fase 0-1: Validación Técnica

| Métrica | Objetivo | Actual | ✅/❌ |
|---|---|---|---|
| Multitenant implementado | 100% | 100% | ✅ |
| Security: tenant isolation verified | ✅ Test + CI | 14 tests + verificación manual cross-tenant | ✅ |
| API endpoints validados | 10/10 | 10/10 (E2E + manual) | ✅ |
| Endpoints con Postman collection | ✅ Collection | ✅ `beauty_room_MVP.postman_collection.json` | ✅ |
| calculateAvailableSlots() implementado | ✅ Funcional | ✅ `getAvailableSlots` (endpoint `/slots`) | ✅ |
| No-doble-booking validado | ✅ Tests pass | ✅ 409 Conflict + excluye CANCELLED | ✅ |
| Swagger documentation ready | ✅ Deployed | ✅ `/swagger-ui.html` | ✅ |

---

### Fase 2-3: MVP Bot

| Métrica | Objetivo | Actual | ✅/❌ |
|---|---|---|---|
| Bot responde "Hola" (F2) | ✅ 1 segundo | ✅ Respuesta + keyboard (verificado E2E y manual con deep link) | ✅ |
| Webhook recibe updates Telegram (F2) | ✅ Delivery < 1s | ✅ `/api/telegram/webhook` + deep link `?start=tenantKey` | ✅ |
| Resolución tenant por chat (F2) | ✅ chat_id → tenant_id | ✅ deep link / `Client.telegram_chat_id` → `ConversationState.tenantId` | ✅ |
| Flujo de agendamiento funcional (F3) | ✅ E2E working | ✅ servicio → fecha → hora → confirmar → save (15 tests FSM) | ✅ |
| Mis citas + Cancelar cita (F3) | ✅ Funcional | ✅ lista citas + cancelación con confirmación | ✅ |
| Concurrencia / no-doble-booking (F3) | ✅ primer gana | ✅ 409 `AppointmentConflictException` → re-elegir hora | ✅ |
| Tasa de conversación exitosa | > 80% | N/A | ⏳ |
| Bot latency (respuesta < 2s) | < 2 sec | N/A | ⏳ |

---

### Fase 6: Validación con Usuarios Reales

| Métrica | Objetivo | Actual | ✅/❌ |
|---|---|---|---|
| Estilistas reclutados | 10-15 | 0 | ❌ |
| Citas agendadas vía bot | > 5/semana/estilista | 0 | ❌ |
| Tasa de retención (1 mes) | > 60% | N/A | ⏳ |
| NPS (Net Promoter Score) | > 40 | N/A | ⏳ |
| Tasa de abandono conversacional | < 20% | N/A | ⏳ |

---

## 📊 Velocity (Velocidad de desarrollo)

### Semana 1-2: FASE 0 (Multitenant)

```
Goal: 8 subtareas completadas
       [████████████████████]  100% (COMPLETADA — 5 de agosto de 2026)

Historias completadas:
- [x] Crear entidad Tenant (4h)
- [x] Agregar tenant_id a tablas (6h)
- [x] Implementar TenantInterceptor (3h)
- [x] Actualizar Services (4h)
- [x] Actualizar Repositorios (3h)
- [x] Actualizar JWT (2h)
- [x] Seed de tenants (1h)
- [x] Tests de aislamiento (4h)

Total esperado: ~27 horas (3.5 días full-time) — COMPLETADO
```

### Semana 3: FASE 1 (Verificación API)

```
Goal: API validada y documentada
       [████████████████████]  100% (COMPLETADA — 5 de agosto de 2026)

Historias completadas:
- [x] getAvailableSlots() / endpoint /slots
- [x] No-doble-booking → 409
- [x] telegram_chat_id en Client
- [x] Postman collection
- [x] Tests E2E (AppointmentControllerE2ETest)
- [x] Swagger / OpenAPI
```

```
Goal: 7 subtareas completadas
       [░░░░░░░░░░░░░░░░░░░░]  0% (No iniciada)

Historias esperadas:
- [ ] Verificar calculateAvailableSlots (2h)
- [ ] Agregar telegram_chat_id (1h)
- [ ] Crear Collection Postman (2h)
- [ ] Tests E2E (4h)
- [ ] Swagger setup (1h)
- [ ] Bug fixes de API (3h)
- [ ] Documentation updates (1h)

Total esperado: ~14 horas (2 días full-time)
```

### Semana 4-5: FASE 2-3 (Bot MVP)

```
Goal: 11 subtareas completadas
       [░░░░░░░░░░░░░░░░░░░░]  0% (No iniciada)

Historias esperadas:
- [ ] Setup bot Telegram (2h)
- [ ] TelegramChannel adapter (3h)
- [ ] ConversationState entidad (2h)
- [ ] FSM básico (3h)
- [ ] Flujo autenticación (4h)
- [ ] Flujo seleccionar servicio (3h)
- [ ] Flujo seleccionar fecha/hora (4h)
- [ ] Flujo confirmación (3h)
- [ ] Crear Appointment desde bot (2h)
- [ ] Tests E2E bot (3h)
- [ ] Error handling (2h)

Total esperado: ~31 horas (4 días full-time)
```

---

## 🐛 Bugs Conocidos

| ID | Descripción | Severidad | Status |
|---|---|---|---|
| B1 | Sin multitenant → tenant isolation risk | 🔴 CRÍTICA | ❌ Open |
| B2 | calculateAvailableSlots() no verificado | 🔴 CRÍTICA | ⏳ Pending verification |
| B3 | Bot no existe | 🔴 CRÍTICA | ❌ Open |
| B4 | telegram_chat_id campo falta en Client | 🟡 ALTA | ❌ Open |
| B5 | Endpoints no documentados (Swagger) | 🟡 ALTA | ❌ Open |
| B6 | No hay E2E tests | 🟡 ALTA | ❌ Open |

---

## 📋 Checklist de Health Check Semanal

Usar este checklist cada viernes para evaluar salud:

```
Semana: ___________

TÉCNICA
- [ ] ¿Compilar? (./mvnw clean compile)
- [ ] ¿Tests pasan? (./mvnw test)
- [ ] ¿Errores en SonarQube? (< 3 bloqueantes)
- [ ] ¿Git branches sincronizadas?
- [ ] ¿Documentación actualizada?

PROGRESO
- [ ] ¿Completaste historias planeadas?
- [ ] ¿Encontraste blockers?
- [ ] ¿Necesitas help externo?
- [ ] ¿Timeline es realista?

SEGURIDAD
- [ ] ¿Alguien puede ver datos de otro tenant?
- [ ] ¿JWT sin tenant_id?
- [ ] ¿Queries sin WHERE tenant_id?

CALIDAD
- [ ] ¿Cobertura de tests subió?
- [ ] ¿Deuda técnica nueva?
- [ ] ¿Code reviews completadas?

SIGUIENTE SEMANA
- [ ] Historias prioritarias identificadas
- [ ] Dependencias resueltas
```

---

## 📱 Tablero de Control (Estado en tiempo real)

### Compilación & Tests

```
Status del Build:     ✅ PASSING
  - Últimas 3 builds: ✅ ✅ ✅
  - Tiempo promedio:  45 sec
  - Últimos 7 días:   100% pass rate

Tests:                ✅ 30/30 passing (5 nuevos en Fase 2)
  - Unitarios:        ✅ 30/30 passing
  - Integración:      ⏳ Pendiente Fase 3
  - E2E:              ✅ 1/1 (flujo Telegram vía TelegramUpdateHandler) + verificación manual con deep link
  - Cobertura:        52% (apunta a 70%)

Code Quality:         ⚠️  NEEDS IMPROVEMENT
  - SonarQube Grade:  C (apunta a B)
  - Technical Debt:   5 días hombre
  - Bloqueantes:      1 (FSM Fase 3 pendiente — el 2º "no-doble-booking" quedó resuelto en Fase 1)
```

### Staging Environment

```
Deployed version:     NO DEPLOYMENT YET
Base de datos:        ✅ Creada (MariaDB)
Data seed:            ⚠️  Básico (2-3 registros)
Health check:         N/A
Response time:        N/A
Uptime:               N/A
```

---

## 🚨 Alarmas críticas

```
🔴 ROJO: Acción inmediata requerida
🟡 AMARILLO: Atención en próximas 24h
🟢 VERDE: OK

ROJO:
  🔴 FSM del estilista no implementado (Fase 4, BLOQUEANTE para loop completo)

AMARILLO:
  🟡 Swagger sin documentación
  🟡 telegram.bot.token sin configurar en local
  🟡 Recordatorios automáticos pendientes (Fase 5)

VERDE:
  🟢 Multitenant implementado y aislado ✅
  🟢 Backend compilation OK
  🟢 Database connection OK
  🟢 JWT working (incluye tenantId)
  🟢 Webhook + deep link Telegram funcionales ✅
  🟢 409 Conflict no-doble-booking verificado ✅
  🟢 Agendamiento completo vía bot (servicio→fecha→hora→confirmar) ✅
```

---

## 📊 Gráficos de progreso

### Progreso por Fase (Gantt)

```
FASE 0  █████████████████████  100% (Semana 1-2) ✅
FASE 1  █████████████████████  100% (Semana 3) ✅
FASE 2  █████████████████████  100% (Semana 4) ✅ (esqueleto bot: webhook+deep link)
FASE 3  █████████████████████  100% (Semana 5) ✅ (FSM agendamiento)
FASE 4-5░░░░░░░░░░░░░░░░░░░░░  0% (Semana 6-7)
FASE 6  ░░░░░░░░░░░░░░░░░░░░░  0% (Semana 8-10)
FASE 7+ ░░░░░░░░░░░░░░░░░░░░░  0% (Semana 11+)

Total  ██████████░░░░░░░░░░░  50% de fases completadas
```

### Commits por semana

```
Semana 1-3:  Fases 0-1 (multitenant + API REST)
Semana 4:    Fase 2 (bot esqueleto: webhook + deep link)
Semana 5:    Fase 3 (FSM agendamiento) — en curso

Promedio esperado: 8-10 commits/semana
```

### Línea de burndown (ideal)

```
Tareas pendientes

50 │
   │ ╱─── Ideal
40 │╱╲
   │  ╲─── Actual (sin datos aún)
30 │   
   │    
20 │     
   │
10 │      
   │
 0 └─────────────────────────
   Sem1  Sem2  Sem3  Sem4  Sem5
```

---

## 🎓 Lecciones aprendidas

**A actualizar conforme avances:**

| Tema | Lección | Fuente |
|---|---|---|
| Multitenant | Usar `@JsonIgnore` en `tenant` (no `@JsonBackReference`) y `jackson-datatype-hibernate6` para evitar errores de proxies lazy (`ByteBuddyInterceptor`); `findBy...AndTenantId` no siempre genera query derivada válida | Fase 0 |
| Telegram (7.11.0) | Usar `telegrambots-springboot-webhook-starter:7.11.0` (el clásico `telegrambots-spring-boot-starter` es de Boot 2.7); los beans del bot deben ser `@ConditionalOnProperty(telegram.bot.token)` para arrancar sin token; resolver el tenant por **deep link `?start=tenantKey`** (un solo bot multitenant) | Fase 2 |
| FSM (Fase 3) | `@Service` choca con `entities.Service` → usar FQN en la anotación; `InlineKeyboardRow` (7.11.0) para `InlineKeyboardMarkup`; fuera de HTTP hay que `TenantInterceptor.setCurrentTenantId`/`clear`; persistir `tenantId` en `ConversationState` (los callbacks no repiten el deep link); clientes del bot con email sintético `tg_<chatId>@bot.local` | Fase 3 |
| Bot design | (Se aprenderá en Fase 3) | Bot MVP |
| Validación con usuarios | (Se aprenderá en Fase 6) | Pilotos |

---

## 📞 Decisiones pendientes

- [ ] ¿Migración de base de datos si hay datos reales en producción?
- [ ] ¿Backup strategy?
- [ ] ¿Cómo comunicar a estilistas actuales sobre Multitenant?
- [ ] ¿Timezone handling — qué zona horaria por defecto?

---

## 📅 Próximas revisiones

- **5 de agosto 2026** → ✅ Fin de Fase 3, MVP de agendamiento working (COMPLETADA)
- **2 de septiembre 2026** → Fin de Fase 4-5, loop completo (estilista + recordatorios)
- **16 de septiembre 2026** → Fin de Fase 6, validación usuarios reales

---

**Mantenedor:** Desarrollador solo  
**Última actualización:** 5 de agosto 2026 17:00 UTC  
**Próxima revisión:** Fin de Fase 4-5 (2 de septiembre)
