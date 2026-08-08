# Plan de mejora para la mantenibilidad y operación del proyecto

## Objetivo

Convertir el proyecto de un MVP funcional a una base más segura, estable y preparada para crecer, sin perder la simplicidad del backend actual.

## Alcance

Este plan aborda los 10 puntos identificados previamente:

1. Mover secretos a variables de entorno o un gestor de secretos.
2. Sustituir Hibernate `create-drop` por migraciones reales.
3. Unificar DTOs y contratos de API.
4. Reemplazar inyección por campo con constructor injection.
5. Centralizar manejo de excepciones y respuestas HTTP.
6. Añadir health checks y métricas.
7. Preparar CI/CD con build, tests y despliegue automatizado.
8. Definir política de versionado y compatibilidad de API.
9. Crear política de dependencias y actualización regular.
10. Mejorar operatividad: backups, restauración, alertas, logs estructurados y tracing.

---

## Fase 1 — Seguridad y configuración (Semanas 1-2)

### Objetivo

Eliminar configuración sensible del código y dejar el proyecto listo para entornos reales.

### Tareas

1. Externalizar secretos del archivo de propiedades
   - Mover `jwt.secret-key`, credenciales de base de datos y tokens de Telegram a variables de entorno.
   - Añadir soporte para perfiles como `dev`, `test`, `staging` y `prod`.
   - Documentar el uso de `application.yml` o `application.properties` solo para valores no sensibles.

2. Definir perfiles de entorno
   - Crear un perfil `dev` para desarrollo local con datos seed, logs verbosos y base de datos local.
   - Crear un perfil `test` para pruebas automatizadas con base de datos aislada, sin depender de producción.
   - Crear un perfil `staging` para validación previa a producción con configuraciones cercanas al entorno real.
   - Crear un perfil `prod` para despliegue real con seguridad reforzada, logging controlado y desactivación de datos sensibles en consola.
   - Establecer reglas claras de herencia entre perfiles para no duplicar configuración innecesariamente.

3. Preparar un modelo seguro de configuración
   - Definir un patrón base para leer valores con `@ConfigurationProperties`.
   - Evitar hardcodear datos en código y recursos.
   - Usar perfiles para activar/desactivar features según el entorno, por ejemplo: bot de Telegram, logs SQL, validación de webhook y seed de datos.

4. Revisar permisos y exposición de endpoints
   - Confirmar que los endpoints públicos sean realmente necesarios.
   - Reducir la superficie de ataque en producción.
   - Desactivar o limitar funcionalidades de debug en `prod`.

### Entregables

- Configuración segura y desacoplada del código.
- Perfiles `dev`, `test`, `staging` y `prod` correctamente definidos.
- Documentación de variables de entorno requeridas por perfil.
- Reglas claras de comportamiento para cada entorno.

---

## Fase 2 — Persistencia y base de datos (Semanas 2-3)

### Objetivo

Hacer que la base de datos sea tratable, reproducible y segura para evolución continua.

### Tareas

1. Introducir Flyway o Liquibase
   - Añadir migraciones versionadas para el esquema inicial.
   - Reemplazar el uso de `spring.jpa.hibernate.ddl-auto=create-drop` en entornos de persistencia.

2. Definir estrategia de datos
   - Separar datos de seed de datos de negocio.
   - Mantener `import.sql` solo para entornos de pruebas locales si sigue siendo necesario.

3. Preparar backups y restauración
   - Definir procedimiento de backup de MariaDB.
   - Documentar cómo restaurar una instancia en caso de pérdida.

### Entregables

- Migraciones versionadas y reproducibles.
- Procedimiento de backup/restauración.

---

## Fase 3 — Calidad del código y arquitectura (Semanas 3-5) ✅ COMPLETADA

> **Estado (7 de agosto de 2026):** los 4 puntos de esta fase están resueltos. Detalle de la implementación:

1. **Unificar DTOs y contratos de API** ✅
   - Estándar definido: **camelCase en todos los campos de DTOs** (request y response).
   - Renombrados: `id_client`→`clientId`, `id_stylist`→`stylistId`, `id_service`→`serviceId`, `id_stylist_room`→`stylistRoomId`, `idService`→`id`, `name_client`→`nameClient`, `name_stylist`→`nameStylist`, `telegram_chat_id`→`telegramChatId` (en DTOs: `AppointmentSaveDto`, `ClientSaveDto`, `ServiceSaveDto`, `ServiceResponseDto`, `StylistSaveDto`, `RegisterRequest`).
   - Contrato actualizado en servicios, controllers (incl. panel Thymeleaf y sus templates `form.html`/`list.html`), bot Telegram (`TelegramUpdateHandler`) y la collection Postman `beauty_room_MVP.postman_collection.json`.
   - **Breaking change:** el JSON de la API ya NO acepta snake_case en los campos renombrados. Sin usuarios reales aún (Fase 7 pendiente), es aceptable.
   - NOTA: las **entidades JPA** conservan snake_case (`name_client`, `telegram_chat_id`, `name_service`); el snake_case solo persiste a nivel de BD/entidad, no en el contrato JSON.

2. **Constructor injection** ✅
   - Eliminados los 5 `@Autowired` de campo/constructor redundantes. Ahora: `ConversationStateServiceImplement`, `StylistServiceImplement` usan `@RequiredArgsConstructor`; `TelegramBotService` y `TelegramChannel` inyectan el `TelegramClient` opcional (bean `@ConditionalOnProperty`) vía `ObjectProvider<TelegramClient>.getIfAvailable()`; `AppointmentServiceImplement` mantiene constructor explícito.
   - No queda inyección por campo en `src/main`.

3. **Centralizar manejo de excepciones** ✅
   - Ya existía: `Controllers/GlobalExceptionHandler` (`@RestControllerAdvice`) + `ApiErrorResponse` (errores de validación, tenant, acceso, conflicto 409, integridad, no encontrado, runtime).
   - Arreglado el test unitario `GlobalExceptionHandlerTest` (usaba `rejectValue` sobre un target `Object` sin la propiedad, lo que lanzaba `NotReadableProperty`; ahora usa `FieldError` explícito).

4. **Revisar nombres y responsabilidades** ✅
   - Extraído el cálculo de slots de `AppointmentServiceImplement` (era el servicio más sobrecargado, 463 líneas) a un nuevo `IAvailabilityService`/`AvailabilityServiceImplement` (horarios disponibles por estilista/servicio/fecha, con solape de citas y bloqueos). `IAppointmentService.getAvailableSlots` queda como delegado, sin cambios en controllers ni en el bot.
   - Pendiente futuro (fuera de alcance de esta fase): `TelegramUpdateHandler` (FSM, ~1.080 líneas) sigue siendo el más grande; dividirlo requiere re-diseñar el FSM y es recomendable tras Fase 7.

### Verificación
- `./mvnw test`: **78 tests OK, 0 fallos** (incluye E2E de doble-booking 409 vía `clientId`/`stylistId`/`serviceId` y los tests del panel con `stylistId`).

---

## Fase 4 — Observabilidad y operación (Semanas 4-6)

### Objetivo

Hacer que la aplicación sea operable en producción y fácil de diagnosticar.

### Tareas

1. Añadir health checks
   - Exponer endpoints de salud para base de datos, app y dependencias críticas.
   - Integrarlos con un monitor externo si es posible.

2. Añadir métricas y logs estructurados
   - Incorporar métricas de negocio y de infraestructura.
   - Usar logs estructurados para facilitar la búsqueda y el análisis.

3. Preparar tracing
   - Añadir correlación de peticiones para seguir un flujo de negocio a través de servicios y componentes.

### Entregables

- Monitoreo básico operativo.
- Mayor visibilidad de fallos y comportamiento del sistema.

---

## Fase 5 — Automatización y calidad continua (Semanas 5-6)

### Objetivo

Reducir el riesgo de introducir errores con cada cambio.

### Tareas

1. Configurar CI/CD
   - Ejecutar `mvn test` y verificar compilación en cada cambio.
   - Ejecutar análisis estático simple si se considera útil.
   - Publicar artefactos automáticamente.

2. Definir calidad mínima
   - Reglas básicas para PRs.
   - Revisión obligatoria de cambios en seguridad o base de datos.

3. Crear una política de dependencias
   - Revisar actualizaciones periódicas.
   - Mantener dependencias modernas y compatibles.

### Entregables

- Pipeline automatizado de validación.
- Menor riesgo de regresiones.

---

## Fase 6 — Evolución y madurez del producto (Semanas 6-8)

### Objetivo

Crear una base sostenible para que el sistema siga creciendo sin perder control.

### Tareas

1. Definir versionado de API
   - Adoptar una política clara para cambios compatibles e incompatibles.
   - Considerar versionado por ruta (`/api/v1`) si el proyecto crece.

2. Establecer documentación viva
   - Mantener Swagger/OpenAPI actualizado.
   - Documentar flujos y decisiones operativas.

3. Preparar soporte para crecimiento
   - Evaluar si el bot de Telegram y la API deben separarse en módulos o servicios más independientes.
   - Planificar cómo manejar más tenants, más tráfico y más integraciones.

### Entregables

- API más madura y sostenible.
- Mejor preparación para escalar el producto.

---

## Prioridades recomendadas

### Prioridad 1: inmediata
- Secretos y configuración. ✅ (Fase 1 completada)
- Migraciones de base de datos. ⏳ Flyway sigue desactivado en todos los perfiles (Fase 2, pendiente)
- Manejo de excepciones. ✅ (Fase 3)

### Prioridad 2: corta
- Constructor injection. ✅ (Fase 3)
- DTOs y contratos unificados. ✅ (Fase 3, camelCase en DTOs)
- Health checks y métricas. ❌ Pendiente (Fase 4)

### Prioridad 3: media
- CI/CD. ❌ Pendiente (Fase 5)
- Versionado de API. ❌ Pendiente (Fase 6)
- Política de dependencias. ❌ Pendiente (Fase 5)

---

## Indicadores de éxito

Se considerará que el plan ha dado resultados cuando:

- no haya secretos hardcodeados en el repositorio;
- cada entorno tenga una configuración explícita y documentada;
- `dev` y `test` puedan ejecutarse de forma local y aislada;
- `staging` y `prod` tengan políticas de seguridad y operación diferenciadas;
- la base de datos se gestione con migraciones versionadas;
- la API responda de forma uniforme ante errores;
- el sistema tenga al menos salud básica y trazabilidad operacional;
- los cambios puedan validarse automáticamente en CI.

---

## Siguiente paso recomendado

Empezar por la Fase 1 y la Fase 2, porque son las que más impactan en seguridad y estabilidad del proyecto.
