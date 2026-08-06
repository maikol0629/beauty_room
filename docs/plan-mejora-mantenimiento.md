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

## Fase 3 — Calidad del código y arquitectura (Semanas 3-5)

### Objetivo

Mejorar la mantenibilidad interna del backend para reducir deuda técnica.

### Tareas

1. Unificar DTOs y contratos de API
   - Definir un estándar para nombres de campos y respuestas.
   - Reducir mezclas entre snake_case y camelCase.
   - Documentar el contrato de cada endpoint.

2. Sustituir inyección por campo por constructor injection
   - Revisar controladores y servicios principales.
   - Mejorar testabilidad y claridad.

3. Centralizar manejo de excepciones
   - Crear un `@ControllerAdvice` o equivalente.
   - Mapear errores de negocio, validación, autenticación y no encontrado a respuestas consistentes.

4. Revisar nombres y responsabilidades
   - Identificar servicios sobrecargados.
   - Separar lógica compleja cuando sea necesario.

### Entregables

- Código más limpio y uniforme.
- Respuestas de API consistentes.
- Menor riesgo de regresiones al modificar el sistema.

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
- Secretos y configuración.
- Migraciones de base de datos.
- Manejo de excepciones.

### Prioridad 2: corta
- Constructor injection.
- DTOs y contratos unificados.
- Health checks y métricas.

### Prioridad 3: media
- CI/CD.
- Versionado de API.
- Política de dependencias.

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
