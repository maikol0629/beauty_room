# ✅ Checklist Fase 1 — Verificar API REST completa

**Objetivo:** Asegurar que el backend es robusto para soportar el bot de Telegram.  
**Timeline estimado:** 1 semana  
**Bloqueado por:** Fase 0 (Multitenant) ⛔  
**Status:** 🟢 **COMPLETADA** (5 de agosto de 2026)

---

## 📌 Resumen de implementación (qué se hizo realmente)

> Lo que ya existía se verificó y se documentó; lo que faltaba se implementó. Detalle:

- **`getAvailableSlots(stylistId, serviceId, date)`** ya existía en `IAppointmentService` / `AppointmentServiceImplement` (equivalente funcional al `calculateAvailableSlots()` del checklist). Devuelve `List<LocalTime>` filtrando por schedule del estilista, citas existentes (sin `CANCELLED`) y bloqueos (`IBlockedSlotService.isSlotBlocked`).
- **Endpoint público de slots:** se agregó `GET /api/appointment/slots` (alias de `/api/appointment/availability`) y se habilitó en `SecurityConfig`. Verificado: retorna la lista de franjas libres de 30 min.
- **No-doble-booking:** `save()` ahora lanza `AppointmentConflictException` cuando la franja no está disponible → `GlobalExceptionHandler` devuelve **409 Conflict** con mensaje claro. Se corrigió la query `existsOverlappingAppointment` para **excluir `CANCELLED` y `REJECTED`** (antes una cita cancelada seguía bloqueando el slot). Verificado: cancelar libera el slot y permite re-agendar.
- **`telegram_chat_id` en `Client`**: agregado el campo en la entidad, en `ClientResponseDto` (como `telegramChatId`), `ClientSaveDto` (`telegram_chat_id`) y `RegisterRequest` (`telegram_chat_id`, opcional). `AuthenticationService.registerClient` y `ClientServiceImplement` lo persisten/mapean. Repositorio: `findByTelegramChatId` y `findByTelegramChatIdAndTenantId` (con `@Query` explícita, porque el campo Java `telegram_chat_id` no se resuelve como `telegramChatId` en queries derivadas).
- **Seed:** `import.sql` ahora siembra `telegram_chat_id` para alice (111111111) y bob (222222222).
- **Swagger:** `springdoc-openapi-starter-webmvc-ui` (2.3.0) ya estaba en `pom.xml`. Se agregó `Config/OpenApiConfig.java` (metadata + esquema Bearer JWT) y anotaciones `@Tag`/`@Operation` en `AuthController`, `ClientController`, `StylistController`, `ServiceController` y `AppointmentController`. UI en `http://localhost:8080/swagger-ui.html` (redirige a `/swagger-ui/index.html`).
- **Postman collection:** `beauty_room_MVP.postman_collection.json` en la raíz con los **endpoints reales** de la app (incluye headers `X-Tenant-ID`, script que guarda el JWT automáticamente, y caso "conflict esperado 409").
- **Tests E2E:** `AppointmentControllerE2ETest` (`@SpringBootTest` + `@AutoConfigureMockMvc`, usa la MariaDB real): login real con alice → obtiene JWT → consulta slots → crea cita (200) → doble booking (409) → verifica que `/api/client/me` expone `telegramChatId`.

### Verificación realizada
- `./mvnw test`: **16 tests, 0 fallos** (14 de Fase 0 + 2 nuevos E2E).
- Pruebas manuales con la app corriendo:
  - `GET /api/appointment/slots?stylistId=1&serviceId=1&date=...` → lista de franjas. ✅
  - `POST /api/appointment/save` (alice, tenant 1) → 200. ✅
  - Segundo `POST` en el mismo slot → **409** `La franja horaria solicitada no está disponible para el estilista 1`. ✅
  - Tras cancelar la cita → el slot vuelve a aparecer y se puede re-agendar. ✅
  - `GET /api/client/me` → incluye `"telegramChatId": "111111111"`. ✅
  - `GET /swagger-ui.html` → 302 a `/swagger-ui/index.html`; `/v3/api-docs` → 200. ✅

---

## ⚠️ Diferencias con el checklist original

| Punto | Documentado | Implementado |
|---|---|---|
| Método de slots | `calculateAvailableSlots()` | `getAvailableSlots()` (mismo propósito) |
| Path de slots | `/api/appointments/slots` | `/api/appointment/slots` (convención singular del repo) |
| Doble booking | 409 vía excepción | ✅ igual (`AppointmentConflictException`) |
| `telegram_chat_id` | `@Column(unique = true)` | `@Column(name = "telegram_chat_id")` sin unique (evita colisión entre tenants) |
| Repo por telegram | queries derivadas | `@Query` explícita (campo snake_case) |
| `isTimeSlotAvailable()` | método público | lógica en `validateStylistAvailability()` (privado) + `existsOverlappingAppointment` |
| E2E save | `jsonPath("$.status")=PENDING` | el controller devuelve 200 con body vacío; se aserta status 200 |

---

## Checklist original (marcado)

<!-- El contenido original del checklist está más abajo, ahora marcado. -->

---

## 1. Verificar endpoints existentes con Postman

### Crear una Collection de Postman: `beauty_room_MVP.postman_collection.json`

```json
{
  "info": {
    "name": "Beauty Room MVP",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Login",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\"email\": \"stylist@example.com\", \"password\": \"password123\"}"
            },
            "url": {"raw": "{{BASE_URL}}/api/auth/login", "path": ["api", "auth", "login"]}
          }
        }
      ]
    },
    {
      "name": "Appointments",
      "item": [
        {
          "name": "Get all appointments",
          "request": {
            "method": "GET",
            "header": [{"key": "Authorization", "value": "Bearer {{JWT}}"}],
            "url": {"raw": "{{BASE_URL}}/api/appointments", "path": ["api", "appointments"]}
          }
        },
        {
          "name": "Create appointment",
          "request": {
            "method": "POST",
            "header": [
              {"key": "Authorization", "value": "Bearer {{JWT}}"},
              {"key": "Content-Type", "value": "application/json"}
            ],
            "body": {
              "mode": "raw",
              "raw": "{\"clientId\": 1, \"stylistId\": 1, \"serviceId\": 1, \"startDate\": \"2026-08-20T10:00:00\", \"endDate\": \"2026-08-20T10:30:00\"}"
            },
            "url": {"raw": "{{BASE_URL}}/api/appointments", "path": ["api", "appointments"]}
          }
        },
        {
          "name": "Get available slots",
          "request": {
            "method": "GET",
            "header": [{"key": "Authorization", "value": "Bearer {{JWT}}"}],
            "url": {"raw": "{{BASE_URL}}/api/appointments/slots?stylistId=1&serviceId=1&date=2026-08-20", "path": ["api", "appointments", "slots"]}
          }
        }
      ]
    },
    {
      "name": "Services",
      "item": [
        {
          "name": "Get all services",
          "request": {
            "method": "GET",
            "header": [{"key": "Authorization", "value": "Bearer {{JWT}}"}],
            "url": {"raw": "{{BASE_URL}}/api/services", "path": ["api", "services"]}
          }
        },
        {
          "name": "Create service",
          "request": {
            "method": "POST",
            "header": [
              {"key": "Authorization", "value": "Bearer {{JWT}}"},
              {"key": "Content-Type", "value": "application/json"}
            ],
            "body": {
              "mode": "raw",
              "raw": "{\"name_service\": \"Corte\", \"description\": \"Corte de cabello\", \"price\": 50.0, \"duration\": 30}"
            },
            "url": {"raw": "{{BASE_URL}}/api/services", "path": ["api", "services"]}
          }
        }
      ]
    },
    {
      "name": "Stylists",
      "item": [
        {
          "name": "Get all stylists",
          "request": {
            "method": "GET",
            "header": [{"key": "Authorization", "value": "Bearer {{JWT}}"}],
            "url": {"raw": "{{BASE_URL}}/api/stylists", "path": ["api", "stylists"]}
          }
        }
      ]
    },
    {
      "name": "Clients",
      "item": [
        {
          "name": "Get all clients",
          "request": {
            "method": "GET",
            "header": [{"key": "Authorization", "value": "Bearer {{JWT}}"}],
            "url": {"raw": "{{BASE_URL}}/api/clients", "path": ["api", "clients"]}
          }
        },
        {
          "name": "Create client",
          "request": {
            "method": "POST",
            "header": [
              {"key": "Authorization", "value": "Bearer {{JWT}}"},
              {"key": "Content-Type", "value": "application/json"}
            ],
            "body": {
              "mode": "raw",
              "raw": "{\"name\": \"Cliente Nuevo\", \"phone\": \"+573001234567\", \"email\": \"cliente@example.com\", \"telegram_chat_id\": \"987654321\"}"
            },
            "url": {"raw": "{{BASE_URL}}/api/clients", "path": ["api", "clients"]}
          }
        }
      ]
    },
    {
      "name": "Schedules",
      "item": [
        {
          "name": "Get stylist schedules",
          "request": {
            "method": "GET",
            "header": [{"key": "Authorization", "value": "Bearer {{JWT}}"}],
            "url": {"raw": "{{BASE_URL}}/api/schedules?stylistId=1", "path": ["api", "schedules"]}
          }
        }
      ]
    }
  ]
}
```

### Variables de Postman (Environment)
```json
{
  "id": "beauty_room_env",
  "name": "Beauty Room",
  "values": [
    {"key": "BASE_URL", "value": "http://localhost:8080", "enabled": true},
    {"key": "JWT", "value": "", "enabled": true}
  ]
}
```

**Instrucciones:**
1. Importar colección en Postman
2. Ejecutar `/login` y copiar el JWT respuesta
3. Pegar JWT en variable `{{JWT}}`
4. Ejecutar rest de requests

---

## 2. Verificar/implementar `calculateAvailableSlots()`

**Ubicación esperada:** `IAppointmentService` interface + `AppointmentServiceImplement`

```java
// src/main/java/com/mr/sb/beauty_room/Services/IAppointmentService.java

public interface IAppointmentService {
    // ... métodos existentes ...

    /**
     * Calcula franjas horarias disponibles para agendamiento.
     * 
     * @param stylistId ID del estilista
     * @param serviceId ID del servicio (para obtener duración)
     * @param date Fecha deseada (sin hora)
     * @return Lista de LocalDateTime con horas inicio disponibles
     */
    List<LocalDateTime> calculateAvailableSlots(Long stylistId, Long serviceId, LocalDate date);

    /**
     * Verifica si una franja de tiempo está disponible sin conflictos.
     * 
     * @param stylistId ID del estilista
     * @param startTime Inicio deseado
     * @param endTime Fin deseado
     * @return true si está disponible
     */
    boolean isTimeSlotAvailable(Long stylistId, LocalDateTime startTime, LocalDateTime endTime);
}
```

**Implementación esperada:**

```java
@Service
@Transactional
public class AppointmentServiceImplement implements IAppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private StylistRepository stylistRepository;
    
    @Autowired
    private ServiceRepository serviceRepository;
    
    @Autowired
    private BlockedSlotRepository blockedSlotRepository;

    @Override
    public List<LocalDateTime> calculateAvailableSlots(Long stylistId, Long serviceId, LocalDate date) {
        // 1. Obtener el stylist y verificar que existe
        Stylist stylist = stylistRepository.findById(stylistId)
            .orElseThrow(() -> new RuntimeException("Stylist not found"));

        // 2. Obtener servicio para saber duración
        Service service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("Service not found"));

        int serviceDuration = service.getDuration();  // en minutos

        // 3. Obtener horario del estilista (debe existir StylistSchedule)
        List<StylistSchedule> schedules = stylist.getStylistSchedules();
        LocalTime dayStart = null;
        LocalTime dayEnd = null;

        for (StylistSchedule schedule : schedules) {
            if (schedule.getDayOfWeek() == date.getDayOfWeek()) {
                dayStart = schedule.getStartTime();
                dayEnd = schedule.getEndTime();
                break;
            }
        }

        if (dayStart == null || dayEnd == null) {
            return List.of();  // No hay horario ese día
        }

        // 4. Generar slots de 30 minutos (o configurable)
        List<LocalDateTime> availableSlots = new ArrayList<>();
        LocalDateTime slotStart = LocalDateTime.of(date, dayStart);
        LocalDateTime dayEndTime = LocalDateTime.of(date, dayEnd);

        while (slotStart.plusMinutes(serviceDuration).isBefore(dayEndTime)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(serviceDuration);

            // 5. Verificar si hay conflictos con citas o bloqueos
            if (isTimeSlotAvailable(stylistId, slotStart, slotEnd)) {
                availableSlots.add(slotStart);
            }

            slotStart = slotStart.plusMinutes(30);  // Siguiente slot en 30 min
        }

        return availableSlots;
    }

    @Override
    public boolean isTimeSlotAvailable(Long stylistId, LocalDateTime startTime, LocalDateTime endTime) {
        // 1. Verificar conflictos con citas existentes
        boolean hasConflict = appointmentRepository.existsOverlappingAppointment(stylistId, startTime, endTime);
        if (hasConflict) {
            return false;
        }

        // 2. Verificar bloqueos de tiempo
        boolean isBlocked = blockedSlotRepository.existsBlockedSlot(stylistId, startTime, endTime);
        if (isBlocked) {
            return false;
        }

        return true;
    }
}
```

**Queries necesarias en repositorios:**

```java
// AppointmentRepository
@Query("SELECT COUNT(a) > 0 FROM Appointment a " +
       "WHERE a.stylist.id = :stylistId " +
       "AND a.status != 'CANCELLED' " +
       "AND ((a.startDate < :endTime AND a.endDate > :startTime))")
boolean existsOverlappingAppointment(@Param("stylistId") Long stylistId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

// BlockedSlotRepository
@Query("SELECT COUNT(b) > 0 FROM BlockedSlot b " +
       "WHERE b.stylist.id = :stylistId " +
       "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
boolean existsBlockedSlot(@Param("stylistId") Long stylistId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);
```

**Agregar endpoint en AppointmentController:**

```java
@GetMapping("/slots")
public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
    @RequestParam Long stylistId,
    @RequestParam Long serviceId,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    
    List<LocalDateTime> slots = appointmentService.calculateAvailableSlots(stylistId, serviceId, date);
    return ResponseEntity.ok(slots);
}
```

---

## 3. Verificar validación de no-doble-booking

**Ubicación:** `AppointmentServiceImplement.save()`

```java
@Override
public Appointment save(Appointment appointment) {
    // 1. Validar que las fechas son coherentes
    if (appointment.getStartDate().isAfter(appointment.getEndDate())) {
        throw new IllegalArgumentException("Start date cannot be after end date");
    }

    // 2. Validar que no hay solapamiento
    if (!appointmentService.isTimeSlotAvailable(
        appointment.getStylist().getId(),
        appointment.getStartDate(),
        appointment.getEndDate())) {
        throw new AppointmentConflictException("Time slot not available");
    }

    // 3. Calcular end date si viene null (basado en duración del servicio)
    if (appointment.getEndDate() == null) {
        int duration = appointment.getService().getDuration();
        appointment.setEndDate(appointment.getStartDate().plusMinutes(duration));
    }

    return appointmentRepository.save(appointment);
}
```

**Crear excepción:**

```java
// src/main/java/com/mr/sb/beauty_room/Exceptions/AppointmentConflictException.java

public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message) {
        super(message);
    }
}
```

---

## 4. Agregar campo `telegram_chat_id` a `Client`

**Cambio en entidad:**

```java
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String email;

    @Column(unique = true)
    private String telegram_chat_id;  // 🆕 AGREGAR ESTO

    private String notes;
    
    // ... otros campos existentes ...

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonBackReference
    private Tenant tenant;
}
```

**Actualizar DTO (si existe ClientDTO):**

```java
public class ClientDTO {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String telegramChatId;  // 🆕 AGREGAR
    private String notes;
}
```

**Agregar método en ClientRepository:**

```java
Optional<Client> findByTelegramChatId(String telegramChatId);
Optional<Client> findByTelegramChatIdAndTenantId(String telegramChatId, Long tenantId);
```

---

## 5. Tests end-to-end de flujo de agendamiento

**Crear test:** `src/test/java/com/mr/sb/beauty_room/Controllers/AppointmentControllerE2ETest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
public class AppointmentControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StylistRepository stylistRepository;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private ServiceRepository serviceRepository;

    private String jwtToken;
    private Long stylistId;
    private Long clientId;
    private Long serviceId;

    @Before
    public void setup() {
        // 1. Crear datos de prueba
        Stylist stylist = new Stylist().builder()
            .name_stylist("Test Stylist")
            .phone("+573001234567")
            .role(Role.STYLIST)
            .build();
        stylistId = stylistRepository.save(stylist).getId();

        Client client = new Client().builder()
            .name("Test Client")
            .phone("+573001234568")
            .email("client@test.com")
            .telegram_chat_id("123456789")
            .build();
        clientId = clientRepository.save(client).getId();

        Service service = new Service().builder()
            .name_service("Test Service")
            .price(50.0f)
            .duration(30)
            .build();
        serviceId = serviceRepository.save(service).getId();

        // 2. Login y obtener JWT
        jwtToken = "test_jwt_token";  // En real test, hacer login real
    }

    @Test
    public void testFullAppointmentFlow() throws Exception {
        // 1. Obtener slots disponibles
        mockMvc.perform(get("/api/appointments/slots")
            .param("stylistId", stylistId.toString())
            .param("serviceId", serviceId.toString())
            .param("date", "2026-08-20")
            .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk());

        // 2. Crear cita
        String appointmentJson = "{\"clientId\": " + clientId + 
                                ", \"stylistId\": " + stylistId +
                                ", \"serviceId\": " + serviceId +
                                ", \"startDate\": \"2026-08-20T10:00:00\"" +
                                ", \"endDate\": \"2026-08-20T10:30:00\"}";

        mockMvc.perform(post("/api/appointments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(appointmentJson)
            .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"));

        // 3. Verificar que no se puede crear cita en el mismo slot (conflict)
        mockMvc.perform(post("/api/appointments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(appointmentJson)
            .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isConflict());
    }
}
```

---

## 6. Documentar endpoints (Swagger)

**Agregar en pom.xml:**

```xml
<!-- Swagger/SpringDoc OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.2</version>
</dependency>
```

**Agregar anotaciones en Controllers:**

```java
@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Appointment management APIs")
public class AppointmentController {

    @GetMapping("/slots")
    @Operation(summary = "Get available time slots", description = "Returns list of available appointment slots for a stylist and service on a given date")
    @Parameter(name = "stylistId", description = "Stylist ID")
    @Parameter(name = "serviceId", description = "Service ID")
    @Parameter(name = "date", description = "Date in format YYYY-MM-DD")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(...) {
        // ...
    }

    @PostMapping
    @Operation(summary = "Create appointment", description = "Creates a new appointment")
    public ResponseEntity<Appointment> create(@RequestBody AppointmentDTO dto) {
        // ...
    }
}
```

**Swagger estará en:** `http://localhost:8080/swagger-ui.html`

---

## Orden de ejecución

1. [x] Verificar que `IAppointmentService` existe con interface completa
2. [x] Implementar `calculateAvailableSlots()` en `AppointmentServiceImplement` (ya existía como `getAvailableSlots`)
3. [x] Implementar `isTimeSlotAvailable()` en `AppointmentServiceImplement` (lógica en `validateStylistAvailability`)
4. [x] Agregar queries en `AppointmentRepository` y `BlockedSlotRepository`
5. [x] Agregar endpoint GET `/api/appointments/slots` en `AppointmentController` (como `/api/appointment/slots`)
6. [x] Verificar validación de no-doble-booking en `.save()`
7. [x] Agregar campo `telegram_chat_id` en entidad `Client`
8. [x] Agregar métodos en `ClientRepository` para buscar por telegram_chat_id
9. [x] Crear Collection de Postman
10. [x] Ejecutar tests en Postman:
    - [x] Login → obtener JWT
    - [x] GET `/api/stylists` → ver estilistas
    - [x] GET `/api/services` → ver servicios
    - [x] GET `/api/appointments/slots?stylistId=X&serviceId=Y&date=Z` → ver slots
    - [x] POST `/api/appointments` → crear cita
    - [x] POST `/api/appointments` (mismo horario) → debe fallar con 409 Conflict
11. [x] Crear tests E2E
12. [x] Ejecutar tests
13. [x] Documentar en Swagger

---

## Checklist de verificación

- [x] Todos los endpoints CRUD listados abajo responden 200 OK:
  - [x] GET `/api/stylist/public` (con X-Tenant-ID)
  - [x] GET `/api/service/public` (con X-Tenant-ID)
  - [x] GET `/api/client` (con JWT STYLIST)
  - [x] GET `/api/appointment/findById/{id}` (con JWT)
  - [x] GET `/api/appointment/slots` (con X-Tenant-ID)
- [x] Crear cita respeta disponibilidad (POST `/api/appointment/save` con overlap → 409)
- [x] Calcular slots retorna lista no vacía
- [x] Cliente se puede buscar por `telegram_chat_id`
- [x] JWT contiene informacion de tenant (Fase 0)
- [x] Swagger genera documentación en `http://localhost:8080/swagger-ui.html`
- [x] Tests E2E pasan

---

## ⚠️ Notas importantes

- Asegurar que `StylistSchedule` existe y cada estilista tiene al menos uno
- `BlockedSlot` debe tener lógica para bloqueos recurrentes (ej: almuerzo todos los días)
- Considerar timezone — usar `ZonedDateTime` en lugar de `LocalDateTime` para datos globales
- JWT token debe tener una duración razonable (ej: 24 horas para MVP)
