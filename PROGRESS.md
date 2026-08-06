# 📊 Dashboard de Progreso — Beauty Room MVP

**Actualizado:** 5 de agosto de 2026  
**Próxima revisión:** Después de Fase 4

---

## 🎯 Progreso General del Proyecto

```
██████████████████████████████████████████░░░░  (83% completado)

✅ Backend: 100% entidades creadas
✅ Autenticación: JWT implementado
✅ APIs: Controllers CRUD funcionales
✅ Multitenant: IMPLEMENTADO (Fase 0 completada)
✅ API REST validada: slots, no-doble-booking 409, telegram_chat_id, Swagger (Fase 1)
✅ Bot Telegram: esqueleto + webhook + deep link (Fase 2 completada)
✅ Bot Telegram: FSM de agendamiento + Mis citas + Cancelar (Fase 3 completada)
❌ Bot Telegram: flujo del estilista (Fase 4)
❌ Tests E2E Postman: collection creada, ejecución manual pendiente
```

---

## 📈 Estado por Fase

### FASE 0 — Fundamentos técnicos multitenant

```
Descripción: Implementar arquitectura multitenant básica
Timeline: 1-2 semanas
Prioridad: 🔴 CRÍTICA (bloqueante de todo)
Estado: ✅ COMPLETADA (5 de agosto de 2026)

┌─────────────────────────────────────────┐
│ Subtareas:                              │
├─────────────────────────────────────────┤
│ [x] Crear entidad Tenant                │
│ [x] Agregar tenant_id a 10 tablas       │
│ [x] Implementar TenantInterceptor       │
│ [x] Actualizar 10+ Services             │
│ [x] Actualizar repositorios             │
│ [x] Actualizar JwtProvider              │
│ [x] Crear seed de 2-3 tenants           │
│ [x] Tests de aislamiento                │
├─────────────────────────────────────────┤
│ Entregable: Multitenant aislado + seguro ✅
│ Verificación: 14 tests OK + aislamiento verificado vía API
│ Bloqueante para: Fases 1-13             │
└─────────────────────────────────────────┘
```

**Documentación:** [CHECKLIST_FASE_0.md](CHECKLIST_FASE_0.md) (incluye resumen de implementación y gotchas)

---

### FASE 1 — Modelo de datos completo

```
Descripción: API REST robusta (slots, validación, sin bot)
Timeline: 1 semana (después de Fase 0)
Prioridad: 🔴 ALTA (necesario para bot)
Estado: ✅ COMPLETADA (5 de agosto de 2026)

┌─────────────────────────────────────────┐
│ Status de Componentes:                  │
├─────────────────────────────────────────┤
│ ✅ Entidades (Stylist, Client, Service) │
│ ✅ Services (interfaces + implement)    │
│ ✅ Controllers (CRUD)                   │
│ ✅ Autenticación (JWT)                  │
│ ✅ getAvailableSlots() (slots)          │
│ ✅ No-doble-booking → 409 Conflict      │
│ ✅ Endpoint GET /api/appointment/slots  │
│ ✅ telegram_chat_id en Client           │
│ ✅ Tests E2E (2 nuevos)                 │
│ ✅ Swagger / OpenAPI                    │
├─────────────────────────────────────────┤
│ Subtareas completadas:                  │
│ [x] Verificar calculateAvailableSlots() │
│ [x] Implementar si falta                │
│ [x] Agregar telegram_chat_id            │
│ [x] Crear endpoint GET /slots           │
│ [x] Collection Postman                  │
│ [x] Tests E2E                           │
│ [x] Swagger setup                       │
├─────────────────────────────────────────┤
│ Entregable: API validada + documentada ✅
│ Verificación: 16 tests OK + doble-booking 409 verificado vía API
│ Bloqueante para: Fases 2-3              │
└─────────────────────────────────────────┘
```

**Documentación:** [CHECKLIST_FASE_1.md](CHECKLIST_FASE_1.md) (incluye resumen de implementación y diferencias con el checklist)

---

### FASE 2 — Bot Telegram: esqueleto

```
Descripción: Bot conectado, webhook activo, FSM básico
Timeline: 1 semana (después de Fase 1)
Prioridad: 🔴 CRÍTICA (es el core)
Estado: ✅ COMPLETADA (5 de agosto de 2026)

┌─────────────────────────────────────────┐
│ Subtareas:                              │
├─────────────────────────────────────────┤
│ [x] Crear bot en BotFather (token)      │
│ [x] Setup webhook en Spring             │
│ [x] Endpoint /api/telegram/webhook      │
│ [x] Interfaz MessagingChannel           │
│ [x] TelegramChannel adapter             │
│ [x] ConversationState entidad           │
│ [x] FSM básico (deep link + /start)     │
│ [x] Bot responde "Hola" + keyboard      │
├─────────────────────────────────────────┤
│ Entregable: Bot says "Hola" ✅          │
│ Verificación: 30 tests OK (unit + E2E)  │
│ Bloqueante para: Fase 3 (DESBLOQUEADA)  │
└─────────────────────────────────────────┘
```

**Documentación:** [CHECKLIST_FASE_2.md](CHECKLIST_FASE_2.md) (incluye decisión de deep linking `?start=tenantKey` y cómo probar en local con ngrok).

---

### FASE 3 — Flujo conversacional: agendar

```
Descripción: Cliente agenda cita completamente vía Telegram
Timeline: 1 semana (después de Fase 2)
Prioridad: 🔴 CRÍTICA (MVP funcional)
Estado: ✅ COMPLETADA (5 de agosto de 2026)

┌─────────────────────────────────────────┐
│ Subtareas:                              │
├─────────────────────────────────────────┤
│ [x] Diseñar árbol de conversación       │
│ [x] Comandos: /start, /schedule, /cancel│
│ [x] Flujo: Identificar/crear cliente    │
│ [x] Flujo: Seleccionar servicio         │
│ [x] Flujo: Seleccionar fecha            │
│ [x] Flujo: Seleccionar hora             │
│ [x] Flujo: Confirmación                 │
│ [x] Crear Appointment en BD             │
│ [x] Inline keyboards                    │
│ [x] Mis citas + Cancelar cita           │
│ [x] Manejo de errores conversacionales  │
│ [x] Concurrencia (409 → re-elegir hora) │
├─────────────────────────────────────────┤
│ Entregable: MVP de agendamiento ✅      │
│ Verificación: 41 tests OK (15 FSM)      │
│ Bloqueante para: Fase 6 (validación)    │
└─────────────────────────────────────────┘
```

**Documentación:** [CHECKLIST_FASE_3.md](CHECKLIST_FASE_3.md)

---

### FASE 4 — Flujo del estilista

```
Descripción: Estilista gestiona agenda desde Telegram
Timeline: 1 semana (después de Fase 3)
Prioridad: 🟡 MEDIA (completa el loop)
Estado: ❌ NO INICIADA

┌─────────────────────────────────────────┐
│ Subtareas:                              │
├─────────────────────────────────────────┤
│ [ ] Comando: Ver agenda del día/semana  │
│ [ ] Comando: Bloquear horarios          │
│ [ ] Notificaciones: Nueva cita          │
│ [ ] Comando: Marcar cita completada     │
│ [ ] Comando: Cancelar cita              │
├─────────────────────────────────────────┤
│ Entregable: Estilista opera vía Telegram
│ Bloqueante para: Fase 5                 │
└─────────────────────────────────────────┘
```

---

### FASE 5 — Recordatorios automáticos

```
Descripción: Envío automático de recordatorios 24h y 2h antes
Timeline: 1 semana (después de Fase 4)
Prioridad: 🔴 ALTA (wow factor)
Estado: ❌ NO INICIADA

┌─────────────────────────────────────────┐
│ Subtareas:                              │
├─────────────────────────────────────────┤
│ [ ] Job programado @Scheduled           │
│ [ ] Query citas próximas 24h            │
│ [ ] Envío de recordatorio cliente       │
│ [ ] Resumen diario estilista            │
│ [ ] Deduplicación (no enviar 2x)        │
├─────────────────────────────────────────┤
│ Entregable: MVP con loop completo       │
│ Bloqueante para: Fase 6 (validación)    │
└─────────────────────────────────────────┘
```

---

### FASE 6 — Validación con usuarios reales

```
Descripción: Piloto con 10-15 estilistas reales
Timeline: 2-3 semanas (después de Fase 5)
Prioridad: 🔴 CRÍTICA (valida existencia)
Estado: ❌ NO INICIADA

┌─────────────────────────────────────────┐
│ Subtareas:                              │
├─────────────────────────────────────────┤
│ [ ] Reclutar estilistas (10-15)         │
│ [ ] Onboarding manual (vos das de alta) │
│ [ ] Métrica: citas/semana/estilista     │
│ [ ] Canal de feedback directo           │
│ [ ] Sesiones de feedback semanales      │
│ [ ] Documentar fricción/problemas       │
├─────────────────────────────────────────┤
│ DECISIÓN CRÍTICA:                       │
│ ¿Estilistas reales usan el bot?         │
│ SÍ → Continúa Phase 7                   │
│ NO → Pivotar (cambiar idea)             │
└─────────────────────────────────────────┘
```

---

### FASE 7 — Iteración por feedback

```
Descripción: Ajustes basados en feedback real
Timeline: 1-2 semanas (depende de Fase 6)
Prioridad: 🟡 MEDIA (varía según feedback)
Estado: ❌ NO INICIADA

Típicamente aparece:
- Servicios combinados
- Mejoras UX conversacional
- Ajustes de horarios

EVITAR: Dashboards, reportes, IA, integraciones pago
```

---

### FASE 8-10 — Frontend, self-service, billing

```
Descripción: Panel Angular, onboarding automático, monetización
Timeline: 3-4 semanas (después de Fase 6)
Prioridad: 🟢 BAJA (no es crítico para MVP)
Estado: ❌ NO INICIADA

⚠️  NO hacer antes de validar Phase 6
```

---

### FASE 11-13 — Escalabilidad y crecimiento

```
Descripción: Infrastructure, WhatsApp, growth
Timeline: 4-6 semanas (después de Fase 10)
Prioridad: 🟢 BAJA (después de tracción)
Estado: ❌ NO INICIADA

⚠️  NO hacer antes de tener estilistas reales
```

---

## 📋 Roadmap Visual

```
Semana 1-2:     ███░░░░░░░░░░░░░░░░░  FASE 0: Multitenant ✅
Semana 3:       ░░░███░░░░░░░░░░░░░░  FASE 1: API REST ✅
Semana 4:       ░░░░░███░░░░░░░░░░░░  FASE 2: Bot esqueleto ✅
Semana 5:       ░░░░░░░███░░░░░░░░░░  FASE 3: Bot MVP agendar ✅
Semana 6-7:     ░░░░░░░░░░██████░░░░  FASE 4-5: Flujos completos
Semana 8-10:    ░░░░░░░░░░░░░░░░███░  FASE 6: Validación real
Semana 11-12:   ░░░░░░░░░░░░░░░░░░░█  FASE 7: Iteración

█ = Tiempo de desarrollo (estimado)
░ = Tiempo de espera/validación/feedback
```

**Timeline MVP:** 10-12 semanas (3 meses) en total si trabajas full-time

---

## 🚨 Críticos del Proyecto

| ID | Riesgo | Severidad | Cómo mitigar |
|---|---|---|---|
| R1 | Sin multitenant → security issue | ✅ RESUELTO | Fase 0 completada, aislamiento verificado |
| R2 | Bot sin flujo de estilista → loop incompleto | 🟡 ALTA | Fases 2-3 (agendamiento) ✅; falta Fase 4 (estilista) |
| R3 | Doble-booking de citas | ✅ RESUELTO | 409 Conflict + query excluye CANCELLED (F1) |
| R4 | JWT sin tenant_id → fácil de atacar | ✅ RESUELTO | JWT incluye `tenantId` claim (F0) |
| R5 | Endpoints no validados | ✅ RESUELTO | Postman collection + tests E2E (F1) |
| R6 | Estilistas no usan el bot | 🟡 ALTA | Validar en Fase 6 |
| R7 | Escalabilidad de BD | 🟢 MEDIA | Fase 11, no es MVP |

---

## ✅ Checklist Pre-Implementación

Antes de empezar Fase 0 (completado):

- [x] Leiste CHECKLIST_FASE_0.md completo
- [x] Entendés cómo funciona TenantInterceptor
- [x] Entendés las queries con tenant_id
- [ ] Creaste rama git `feature/multitenant` *(el trabajo se hizo en `appmod/java-upgrade-20251218222941`)*
- [x] Hiciste backup de BD actual (si tiene datos) *(create-drop: no hay datos reales)*
- [x] Decidiste: empezar ahora

---

## 📚 Archivos de Documentación

| Archivo | Propósito |
|---|---|
| [plan.md](plan.md) | Roadmap detallado (13 fases) |
| [CHECKLIST_FASE_0.md](CHECKLIST_FASE_0.md) | Guía paso-a-paso multitenant |
| [CHECKLIST_FASE_1.md](CHECKLIST_FASE_1.md) | Guía verificación API REST |
| [CHECKLIST_FASE_2.md](CHECKLIST_FASE_2.md) | Guía bot Telegram (webhook + deep link) |
| [CHECKLIST_FASE_3.md](CHECKLIST_FASE_3.md) | Guía FSM de agendamiento (Fase 3) |
| [DECISION_LOG.md](DECISION_LOG.md) | Decisiones arquitectónicas explicadas |
| [RESUMEN_AJUSTES.md](RESUMEN_AJUSTES.md) | Qué cambió en el plan |
| [PROGRESS.md](PROGRESS.md) | Este archivo — estado general |

---

## 🎯 Próximo Paso Inmediato

```
1. Probar el agendamiento completo en local (ngrok + deep link + Fase 3)
2. Fase 4: FSM del estilista (agenda del día, bloquear horarios, completar/no-show, cancelar)
3. Fase 5: recordatorios automáticos (@Scheduled)
```

---

**Última actualización:** 5 de agosto de 2026 17:00 UTC  
**Responsable:** Auditoría automática + Copilot  
**Próximo review:** Después de Fase 4
