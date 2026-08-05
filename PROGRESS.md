# 📊 Dashboard de Progreso — Beauty Room MVP

**Actualizado:** 5 de agosto de 2026  
**Próxima revisión:** Después de Fase 1

---

## 🎯 Progreso General del Proyecto

```
███████████████████████████████░░░░░░░░░░░░░  (68% completado)

✅ Backend: 100% entidades creadas
✅ Autenticación: JWT implementado
✅ APIs: Controllers CRUD funcionales
✅ Multitenant: IMPLEMENTADO (Fase 0 completada)
❌ Bot Telegram: NO INICIADO (BLOQUEANTE)
❌ Tests E2E: NO EXISTE
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
Estado: 🟡 PARCIALMENTE COMPLETADA

┌─────────────────────────────────────────┐
│ Status de Componentes:                  │
├─────────────────────────────────────────┤
│ ✅ Entidades (Stylist, Client, Service) │
│ ✅ Services (interfaces + implement)    │
│ ✅ Controllers (CRUD)                   │
│ ✅ Autenticación (JWT)                  │
│ ❌ calculateAvailableSlots() - ?        │
│ ❌ No-doble-booking validación - ?      │
│ ❌ Endpoint GET /slots - NO             │
│ ❌ telegram_chat_id en Client - NO      │
│ ❌ Tests E2E con Postman - NO           │
│ ❌ Swagger documentation - NO           │
├─────────────────────────────────────────┤
│ Subtareas faltantes:                    │
│ [ ] Verificar calculateAvailableSlots() │
│ [ ] Implementar si falta                │
│ [ ] Agregar telegram_chat_id            │
│ [ ] Crear endpoint GET /slots           │
│ [ ] Collection Postman                  │
│ [ ] Tests E2E                           │
│ [ ] Swagger setup                       │
├─────────────────────────────────────────┤
│ Entregable: API validada + documentada  │
│ Bloqueante para: Fases 2-3              │
└─────────────────────────────────────────┘
```

**Documentación:** [CHECKLIST_FASE_1.md](CHECKLIST_FASE_1.md)

---

### FASE 2 — Bot Telegram: esqueleto

```
Descripción: Bot conectado, webhook activo, FSM básico
Timeline: 1 semana (después de Fase 1)
Prioridad: 🔴 CRÍTICA (es el core)
Estado: ❌ NO INICIADA

┌─────────────────────────────────────────┐
│ Subtareas:                              │
├─────────────────────────────────────────┤
│ [ ] Crear bot en BotFather              │
│ [ ] Setup webhook en Spring             │
│ [ ] Endpoint /telegram/webhook          │
│ [ ] Interfaz MessagingChannel           │
│ [ ] TelegramChannel adapter             │
│ [ ] ConversationState entidad           │
│ [ ] FSM básico implementado             │
│ [ ] Bot responde "Hola" + keyboard      │
├─────────────────────────────────────────┤
│ Entregable: Bot says "Hola"             │
│ Bloqueante para: Fase 3                 │
└─────────────────────────────────────────┘
```

---

### FASE 3 — Flujo conversacional: agendar

```
Descripción: Cliente agenda cita completamente vía Telegram
Timeline: 1 semana (después de Fase 2)
Prioridad: 🔴 CRÍTICA (MVP funcional)
Estado: ❌ NO INICIADA

┌─────────────────────────────────────────┐
│ Subtareas:                              │
├─────────────────────────────────────────┤
│ [ ] Diseñar árbol de conversación       │
│ [ ] Comandos: /start, /schedule, etc.   │
│ [ ] Flujo: Autenticación en bot         │
│ [ ] Flujo: Seleccionar servicio         │
│ [ ] Flujo: Seleccionar fecha            │
│ [ ] Flujo: Seleccionar hora             │
│ [ ] Flujo: Confirmación                 │
│ [ ] Crear Appointment en BD             │
│ [ ] Inline keyboards                    │
│ [ ] Manejo de errores conversacionales  │
├─────────────────────────────────────────┤
│ Entregable: MVP completo funcionando    │
│ Bloqueante para: Fase 6 (validación)    │
└─────────────────────────────────────────┘
```

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
Semana 3:       ░░░███░░░░░░░░░░░░░░  FASE 1: API REST
Semana 4-5:     ░░░░░██████░░░░░░░░░  FASE 2-3: Bot MVP
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
| R2 | Bot no existe → no hay producto | 🔴 CRÍTICA | Timeline realista (4-5 sem) |
| R3 | Doble-booking de citas | 🔴 CRÍTICA | Tests en Fase 1 |
| R4 | JWT sin tenant_id → fácil de atacar | ✅ RESUELTO | JWT incluye `tenantId` claim (F0) |
| R5 | Endpoints no validados | 🟡 ALTA | Postman collection (F1) |
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
| [DECISION_LOG.md](DECISION_LOG.md) | Decisiones arquitectónicas explicadas |
| [RESUMEN_AJUSTES.md](RESUMEN_AJUSTES.md) | Qué cambió en el plan |
| [PROGRESS.md](PROGRESS.md) | Este archivo — estado general |

---

## 🎯 Próximo Paso Inmediato

```
1. Leer CHECKLIST_FASE_1.md (30 min)
2. Validar API REST con Postman (endpoints multitenant)
3. Agregar telegram_chat_id a Client
4. Crear endpoint GET /slots
```

---

**Última actualización:** 5 de agosto de 2026 17:00 UTC  
**Responsable:** Auditoría automática + Copilot  
**Próximo review:** 19 de agosto de 2026 (después de Fase 1)
