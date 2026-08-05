# ✅ Checklist Fase 0 — Fundamentos técnicos multitenant

**Objetivo:** Implementar arquitectura multitenant básica sin quebrar lo existente.  
**Timeline estimado:** 1-2 semanas  
**Status:** 🟢 **COMPLETADA** (5 de agosto de 2026)

---

## 📌 Resumen de implementación (qué se hizo realmente)

> Esta sección documenta **cómo quedó implementado** el código, con las desviaciones respecto al template de abajo (que sirvió de guía).

- **Entidad `Tenant`** creada en `entities/Tenant.java` + enums `TenantPlan` (TRIAL, BASIC, PREMIUM) y `TenantStatus` (ACTIVE, SUSPENDED, CANCELLED). `@PrePersist` setea `createdAt` y `trialEndsAt` (+14 días).
- **`tenant_id`** agregado como `@ManyToOne(fetch = LAZY)` en: `User` (base, por herencia JOINED cubre Stylist y Client), `StylistRoom`, `Service`, `Appointment`, `StylistSchedule`, `BlockedSlot`, `Notification`, `Payment`, `Review`. **Todas** con `@JsonIgnore` en el campo `tenant` (ver gotcha abajo).
- **`TenantInterceptor`** (`Security/TenantInterceptor.java`): resuelve tenant **desde el claim `tenantId` del JWT** (prioridad) o desde el header `X-Tenant-ID` (fallback para endpoints públicos). Guarda en `ThreadLocal`; expone `getCurrentTenantId()`, `getCurrentTenantIdOrThrow()` (lanza `TenantNotResolvedException`), `setCurrentTenantId()` (para tests) y `clear()`. Registrado en `Config/WebConfig.java` para `/api/**`.
- **`JwtService`**: `generateToken(User, tenantId)` incluye claim `tenantId`; `extractTenantId(token)` y `extractAllClaimsForTenant(token)` para resolverlo.
- **`AuthenticationService`** reescrito: el registro (client/stylist) resuelve el tenant del contexto (`TenantInterceptor.getCurrentTenantId()`) o del header, valida que exista en `TenantRepository.findByTenantKey`/`findById`, y el JWT de respuesta incluye `tenantId`.
- **Repositorios** reescritos con métodos tenant-scoped: `findByTenantId`, `findByIdAndTenantId`, etc. Ejemplos clave:
  - `AppointmentRepository.existsOverlappingAppointment(stylistId, tenantId, start, end)`
  - `NotificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(...)` (⚠️ el nombre `findByUserIdOrderByCreatedAtDescAndTenantId` NO genera query derivada válida)
  - `IBlockedSlotService.isSlotBlocked(Long tenantId, Long stylistId, ...)` (tenantId pasó a ser el primer parámetro)
- **Services**: todos los `*ServiceImplement` llaman `TenantInterceptor.getCurrentTenantIdOrThrow()` y usan métodos tenant-scoped. `save`/`update` asignan el `tenant` del contexto.
- **`import.sql`**: seed de 3 tenants (Salón María `salon-maria-001` id 1, Estilos Ana `estilos-ana-001` id 2, Beauty Room Pro `beauty-room-pro-001` id 3) y `tenant_id` en todas las inserciones.
- **`GlobalExceptionHandler`**: maneja `TenantNotResolvedException` → 400.
- **`pom.xml`**: agregado `jackson-datatype-hibernate6` para manejar proxies lazy de Hibernate.

### Gotchas descubiertos (importantes)
- **`@JsonIgnore` en `tenant`, NO `@JsonBackReference`:** `@JsonBackReference` requiere un `@JsonManagedReference` pareado y rompe la serialización. Con `@JsonIgnore` en el campo `tenant` de cada entidad se evita el error `Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]`.
- **Proxies lazy:** sin `jackson-datatype-hibernate6`, serializar una entidad con `tenant` lazy (p.ej. `StylistResponseDto.stylistRoom`) revienta con `ByteBuddyInterceptor`.

### Verificación realizada
- `./mvnw test`: **14 tests, 0 fallos** (incluye `TenantIsolationTest` y `TenantJwtFlowTest`).
- Pruebas manuales con la app corriendo:
  - `GET /api/stylist/public` con `X-Tenant-ID: 1` → solo John; con `X-Tenant-ID: 2` → solo Jane. ✅
  - `GET /api/service/public` aislado por tenant. ✅
  - `GET /api/stylist/2` con JWT de john (tenant 1) → **404**. ✅
  - `GET /api/appointment/findById/2` con JWT de alice (tenant 1) → **404**. ✅
  - `findByClientId/4` (tenant 2) con alice → `[]`. ✅
  - Registro de cliente con `X-Tenant-ID: 1` → JWT con `tenantId: 1`. ✅
  - Registro sin header → 400 "No se pudo resolver el tenant para el registro". ✅
- Endpoint sin header ni JWT → 400 `TenantNotResolvedException`.

---

## ⚠️ Diferencias respecto a este documento

El contenido original de este checklist (abajo) sirvió como guía y **esboza las decisiones tomadas**, pero el código final difiere en:

| Punto | Documentado | Implementado |
|---|---|---|
| Anotación de tenant | `@JsonBackReference` | `@JsonIgnore` (evita ByteBuddyInterceptor) |
| Interceptor | Template minimalista | Con prioridad JWT claim > header, `getCurrentTenantIdOrThrow`, soporte de tests |
| Auth | "AuthController login" | `AuthenticationService` (auth/register/authenticate) reescrito |
| Repos base | `TenantAwareRepository` con `@NoRepositoryBean` | **No se usó**; repos con query methods explícitos por tenant |
| JWT claim | `tenantId` | ✅ igual |

---

## Checklist original (marcado)

<!-- El siguiente contenido es el checklist original, ahora marcado como completado. -->

---

## 1. Crear Entidad `Tenant`

```java
// src/main/java/com/mr/sb/beauty_room/entities/Tenant.java
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tenant")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // ej: "Salón María"

    @Column(nullable = false, unique = true)
    private String tenantKey;  // ej: "salon-maria-001" (identificador único para resolver en interceptor)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantPlan plan = TenantPlan.TRIAL;  // TRIAL, BASIC, PREMIUM

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;  // ACTIVE, SUSPENDED, CANCELLED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime trialEndsAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.trialEndsAt = LocalDateTime.now().plusDays(14);
    }
}
```

**Enums a crear:**
```java
public enum TenantPlan { TRIAL, BASIC, PREMIUM }
public enum TenantStatus { ACTIVE, SUSPENDED, CANCELLED }
```

---

## 2. Agregar `tenant_id` a todas las entidades

**Tablas a modificar:**
- Stylist
- Client
- Service
- Appointment
- StylistSchedule
- BlockedSlot
- Notification
- Payment
- Review
- StylistRoom
- User (si corresponde)

**Template para cada entidad:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", nullable = false)
@JsonBackReference
private Tenant tenant;
```

**Cambios en `Stylist` (ej):**
```java
@Data
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "stylist")
@EqualsAndHashCode(callSuper = true)
public class Stylist extends User {
    // ... campos existentes ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonBackReference
    private Tenant tenant;  // 🆕 AGREGAR ESTO
}
```

**Script SQL para migrar (ONE-TIME):**
```sql
-- Esto es para cuando hayas decidido sobre la relación Stylist → Tenant
-- Por ahora no ejecutar, pero tenerlo documentado
ALTER TABLE stylist ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE stylist ADD CONSTRAINT fk_stylist_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);
-- Repetir para todas las tablas...
```

---

## 3. Crear `TenantInterceptor`

```java
// src/main/java/com/mr/sb/beauty_room/Security/TenantInterceptor.java

import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<Long> tenantId = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Opción 1: Resolver desde JWT (recomendado)
            // El JWT debería incluir un claim "tenant_id"
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                // TODO: Extraer tenant_id del JWT
                // Long tid = extractTenantFromJWT(auth);
                // tenantId.set(tid);
            }

            // Opción 2: Resolver desde header
            String tenantHeader = request.getHeader("X-Tenant-ID");
            if (tenantHeader != null) {
                tenantId.set(Long.parseLong(tenantHeader));
            }

            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        tenantId.remove();  // Limpiar ThreadLocal
    }

    public static Long getCurrentTenantId() {
        return tenantId.get();
    }
}
```

**Registrar en `WebConfig`:**
```java
// src/main/java/com/mr/sb/beauty_room/Config/WebConfig.java

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor);
    }
}
```

---

## 4. Crear Repositorio base con filtro de Tenant

```java
// src/main/java/com/mr/sb/beauty_room/repository/TenantAwareRepository.java

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {
    // Métodos que automáticamente filtran por tenant
    // Los repositorios específicos los implementarán
}
```

**Actualizar `StylistRepository` (ej):**
```java
@Repository
public interface StylistRepository extends JpaRepository<Stylist, Long> {
    // Antes: List<Stylist> findAll();
    // Después: necesita filtro tenant_id automático
    
    List<Stylist> findByTenantIdAndStatus(Long tenantId, StylistStatus status);
    Optional<Stylist> findByIdAndTenantId(Long id, Long tenantId);
}
```

**Mejor aún: usar `@Query` con Tenant resuelto desde contexto:**
```java
@Query("SELECT s FROM Stylist s WHERE s.id = :id AND s.tenant.id = :tenantId")
Optional<Stylist> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
```

---

## 5. Actualizar Services para incluir tenant

**Cambio en `StylistServiceImplement` (ej):**

```java
@Service
@Transactional
public class StylistServiceImplement implements IStylistService {

    @Autowired
    private StylistRepository stylistRepository;

    @Override
    public Optional<Stylist> findById(Long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantId();
        if (tenantId == null) {
            throw new TenantNotResolvedException("No tenant context found");
        }
        return stylistRepository.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<Stylist> findAll() {
        Long tenantId = TenantInterceptor.getCurrentTenantId();
        if (tenantId == null) {
            throw new TenantNotResolvedException("No tenant context found");
        }
        return stylistRepository.findByTenantId(tenantId);
    }

    @Override
    public Stylist save(Stylist stylist) {
        Long tenantId = TenantInterceptor.getCurrentTenantId();
        if (tenantId == null) {
            throw new TenantNotResolvedException("No tenant context found");
        }
        // Asegurar que el stylist siempre tiene el tenant del contexto
        stylist.setTenant(new Tenant().builder().id(tenantId).build());
        return stylistRepository.save(stylist);
    }
}
```

---

## 6. Crear Excepción para Tenant no resuelto

```java
// src/main/java/com/mr/sb/beauty_room/Exceptions/TenantNotResolvedException.java

public class TenantNotResolvedException extends RuntimeException {
    public TenantNotResolvedException(String message) {
        super(message);
    }
}
```

---

## 7. Actualizar `AuthController` para emitir JWT con tenant_id

**Cambio en `AuthController`:**

```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // ... validación de credenciales ...

    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found"));

    // Obtener el tenant del usuario (si es Stylist, obtenerlo de Stylist.tenant)
    Long tenantId = null;
    if (user instanceof Stylist) {
        tenantId = ((Stylist) user).getTenant().getId();
    }

    // Agregar tenant_id al JWT
    String jwtToken = jwtProvider.generateToken(user, tenantId);

    return ResponseEntity.ok(new AuthResponse(jwtToken, "Login successful"));
}
```

**Actualizar `JwtProvider` para incluir tenant_id:**

```java
public String generateToken(User user, Long tenantId) {
    return Jwts.builder()
        .setSubject(user.getEmail())
        .claim("userId", user.getId())
        .claim("role", user.getRole().name())
        .claim("tenantId", tenantId)  // 🆕 AGREGAR
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
        .signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
}
```

---

## 8. Crear seed data con 2-3 Tenants

**Actualizar `import.sql`:**

```sql
-- Tenants
INSERT INTO tenant (name, tenant_key, plan, status, created_at, trial_ends_at) 
VALUES ('Salón María', 'salon-maria-001', 'TRIAL', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY));

INSERT INTO tenant (name, tenant_key, plan, status, created_at, trial_ends_at) 
VALUES ('Estilos Ana', 'estilos-ana-001', 'TRIAL', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY));

INSERT INTO tenant (name, tenant_key, plan, status, created_at, trial_ends_at) 
VALUES ('Beauty Room Pro', 'beauty-room-pro-001', 'BASIC', 'ACTIVE', NOW(), NULL);

-- Users/Stylists para cada tenant
-- (Conectarlos con tenant_id)
```

---

## 9. Tests de aislamiento multitenant

```java
// src/test/java/com/mr/sb/beauty_room/Services/implement/TenantIsolationTest.java

@SpringBootTest
public class TenantIsolationTest {

    @Autowired
    private StylistService stylistService;

    @Autowired
    private StylistRepository stylistRepository;

    @Test
    public void testTenantAIsolationFromTenantB() {
        // 1. Crear 2 tenants
        Tenant tenantA = new Tenant().builder().id(1L).name("Tenant A").build();
        Tenant tenantB = new Tenant().builder().id(2L).name("Tenant B").build();

        // 2. Crear stylist en tenant A
        Stylist stylistA = new Stylist().builder()
            .name("Stylist A")
            .tenant(tenantA)
            .build();
        stylistRepository.save(stylistA);

        // 3. Crear stylist en tenant B
        Stylist stylistB = new Stylist().builder()
            .name("Stylist B")
            .tenant(tenantB)
            .build();
        stylistRepository.save(stylistB);

        // 4. Simular request como tenant A
        TenantInterceptor.setCurrentTenantId(1L);
        List<Stylist> styledA = stylistService.findAll();
        assertEquals(1, styledA.size());
        assertEquals("Stylist A", styledA.get(0).getName());

        // 5. Simular request como tenant B
        TenantInterceptor.setCurrentTenantId(2L);
        List<Stylist> stylistsB = stylistService.findAll();
        assertEquals(1, stylistsB.size());
        assertEquals("Stylist B", stylistsB.get(0).getName());

        // 6. Verificar que no se vieron entre sí
        assertTrue(!stylistsB.get(0).getName().equals(styledA.get(0).getName()));
    }
}
```

---

## Orden de ejecución

1. [x] Crear entidad `Tenant` + enums
2. [x] Crear repositorio de `Tenant`
3. [x] Agregar `tenant_id` a TODAS las entidades (⚠️ importante: hacer juntas para evitar inconsistencias)
4. [x] Crear `TenantInterceptor`
5. [x] Crear `WebConfig` y registrar interceptor
6. [x] Actualizar `JwtProvider` para incluir `tenantId`
7. [x] Actualizar `AuthController` para pasar `tenantId` a JWT
8. [x] Actualizar TODOS los repositorios con métodos que filtren por tenant
9. [x] Actualizar TODOS los Services para usar `TenantInterceptor.getCurrentTenantId()`
10. [x] Crear seed de 2-3 tenants en `import.sql`
11. [x] Crear tests de aislamiento
12. [x] Ejecutar tests y verificar que un tenant NO ve datos del otro

---

## ⚠️ Riesgos y cómo mitigarlos

| Riesgo | Cómo evitarlo |
|---|---|
| Olvidar `tenant_id` en una tabla | Usar find & replace: buscar `@Entity` en entities/, verificar que todas tengan campo tenant |
| Service olvidando filtrar por tenant | Crear clase base `TenantAwareService` con método `verifyTenant()` que lance excepción si no existe contexto |
| Query vieja sin tenant filter | Deprecar repositorios viejos y reemplazar con métodos nuevos que tomen `tenantId` explícito |
| JWT sin tenant_id | Tests en AuthController que verifiquen que JWT decodificado contiene `tenantId` |
