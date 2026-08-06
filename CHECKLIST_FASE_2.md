# ✅ Checklist Fase 2 — Bot Telegram: esqueleto y conexión

**Objetivo:** el bot habla con el backend vía webhook, resuelve el tenant por deep link y loguea quién escribe (chat_id + tenant). Todavía sin lógica de negocio compleja (eso es Fase 3).

**Timeline estimado:** 1 semana
**Bloqueado por:** Fase 1 (API REST) ⛔ → ✅ completada
**Status:** 🟢 **COMPLETADA** (5 de agosto de 2026)

---

## 📌 Decisión clave: UN SOLO bot multitenant + deep linking

Tal como pedía el usuario, el onboarding del tenant se hace con **deep linking** (esto NO estaba explícito en `plan.md` y se incorporó aquí):

- Cada salón recibe un link del tipo `https://t.me/<bot_username>?start=<tenantKey>`.
- Telegram entrega ese link al bot como un mensaje de texto `/start <tenantKey>`.
- El `tenantKey` ya existía en la entidad `Tenant` y `TenantRepository.findByTenantKey` ya estaba implementado → el bot lo resuelve sin fricción.
- Si el mensaje NO trae payload (`/start` a secas), el bot intenta resolver el tenant por `Client.telegram_chat_id` (seed: alice=111111111→tenant 1, bob=222222222→tenant 2).
- Si no puede resolver tenant → responde indicando que abra el enlace de su salón (deep link).

**Alternativa descartada:** bot por tenant (n bots = n tokens = n configs). Un solo bot multitenant enruta por `chat_id` → `tenant_id` guardado en `ConversationState`.

---

## 📌 Resumen de implementación (qué se hizo realmente)

### Dependencias (`pom.xml`)

- ⚠️ La dependencia que sugería `plan.md` (`org.telegram:telegrambots-spring-boot-starter`) es de **Spring Boot 2.7** y NO es compatible con nuestro Boot 3.2.3.
- Se usó el starter moderno pensado para Spring Boot 3:
  - `org.telegram:telegrambots-springboot-webhook-starter:7.11.0` (su pom apunta a Spring 3.2.3, coincide con el proyecto)
  - `org.telegram:telegrambots-client:7.11.0` (cliente OkHttp para enviar mensajes y registrar el webhook)
- El starter auto-registra beans `SpringTelegramWebhookBot` y ejecuta el `setWebhook` al arrancar (vía `TelegramBotInitializer`).

### Configuración (`application.properties`)

```
telegram.bot.token=            # token de BotFather (vacío = el bot NO se registra, la app arranca igual)
telegram.bot.username=         # username del bot (ej: beauty_room_bot)
telegram.bot.path=/api/telegram/webhook
telegram.bot.webhook-url=      # URL HTTPS pública (ngrok/deploy). Vacía = se omite setWebhook.
```

### Nuevas clases (siguen el patrón Controller → IService → ServiceImplement → Repository)

| Clase | Rol |
|---|---|
| `entities/ConversationState` | Estado conversacional por chat (chat_id único, tenant_id nullable, current_step, data JSON, updated_at) |
| `repository/ConversationStateRepository` | `findByChatId` |
| `Services/IConversationStateService` + `Services/implement/ConversationStateServiceImplement` | `getOrCreate` / `save` / `findByChatId` |
| `DTOS/telegram/TelegramMessage` | Record normalizado (chatId, text, username, userId, callbackData) |
| `Services/IMessagingChannel` + `Services/implement/TelegramChannel` | Abstracción de canal: `sendMessage`, `sendKeyboard`, `parseUpdate`. Reutilizable para WhatsApp (Fase 12) |
| `Services/implement/TelegramUpdateHandler` | Lógica del bot: parseo → resolución de tenant → persistir estado → responder |
| `Services/implement/TelegramBotService` | `getBotPath/getBotUsername/getBotToken`, `onWebhookUpdate`, `registerWebhook` |
| `Config/TelegramBotProperties` | `@ConfigurationProperties(prefix="telegram.bot")` |
| `Config/TelegramBotConfig` | Beans `TelegramClient` (OkHttp) y `SpringTelegramWebhookBot` — `@ConditionalOnProperty(token)` |
| `Controllers/TelegramWebhookController` | `POST /api/telegram/webhook` (público) recibe el `Update` de Telegram y delega en el bot |

### Flujo de un update

```
Telegram → POST /api/telegram/webhook (Update JSON)
  → TelegramWebhookController → TelegramBotService.onWebhookUpdate
  → TelegramUpdateHandler.handle
      ├─ channel.parseUpdate(update) → TelegramMessage(chatId, text, username, ...)
      ├─ resolveTenant:
      │    ├─ /start <tenantKey> → TenantRepository.findByTenantKey
      │    └─ (sin payload) → ClientRepository.findByTelegramChatId
      ├─ conversationStateService.getOrCreate(chatId) + save(tenantId, step, data)
      ├─ si tenant resuelto → sendKeyboard(saludo, [Agendar cita, Mis citas, Cancelar cita])
      ├─ si NO → sendMessage("abrí el enlace de tu salón")
      └─ log: chat_id + username + tenantId
```

### Seguridad
- `/api/telegram/webhook` agregado a los endpoints públicos en `SecurityConfig`.
- `TenantInterceptor` aplica a `/api/**` pero el webhook no lleva JWT ni `X-Tenant-ID` → el bot resuelve el tenant internamente (por diseño).

### Tests
- `TelegramUpdateHandlerTest` (Mockito): deep link → tenant + keyboard; `/start` sin payload + chat conocido → tenant por client; chat desconocido → mensaje guía; update vacío → no hace nada.
- `TelegramChannelTest` (Mockito): parseo de message/empty; keyboard arma `SendMessage` con `ReplyKeyboardMarkup`; envío sin client no lanza error.
- `ConversationStateServiceImplementTest` (Mockito): getOrCreate crea/reusa; save delega.
- `TelegramWebhookE2ETest` (`@SpringBootTest` + MockMvc + `@MockBean TelegramClient`): POST real con Update JSON → deep link resuelve tenant 1 y envía keyboard; chat desconocido envía guía.

**Total:** 30 tests, 0 fallos (`./mvnw test`).

---

## 🚀 Cómo probarlo en local (con token real)

1. Completar `telegram.bot.token` y `telegram.bot.username` en `application.properties` (los pones vos).
2. Levantar una URL pública HTTPS para el webhook:
   ```bash
   ngrok http 8080
   ```
3. Poner esa URL en `telegram.bot.webhook-url` (ej: `https://xxxx.ngrok.io/api/telegram/webhook`). Al arrancar la app se ejecuta `setWebhook` automáticamente.
4. `./mvnw spring-boot:run`
5. Desde tu Telegram abrir: `https://t.me/<bot_username>?start=salon-maria-001`
   → el bot responde con el saludo + keyboard, y en el log aparece `Bot atendió chat_id=... tenantId=1`.
6. Probar un chat desconocido (`/start` sin payload) → el bot pide el enlace del salón.

> Sin token: la app arranca igual, el endpoint responde 200 vacío y el log muestra warning de "Telegram client no configurado".

---

## ⚠️ Gotchas

- **`User.isBot` / `firstName` son no-null en Telegram** → cualquier Update JSON de prueba DEBE incluir `is_bot` en `from`, o el parseo falla con 400/500.
- El `Update` solo se deserializa si el JSON coincide con la API de Telegram (chat, from, text, date, etc.).
- `telegram.bot.webhook-url` vacío NO rompe el arranque: solo loguea un warning y omite `setWebhook` (evita fallos en CI/tests).
- La versión de la librería importa: el starter clásico (`telegrambots-spring-boot-starter`) es de Spring Boot 2.7. Usar el nuevo (`telegrambots-springboot-webhook-starter`).

---

## ▶️ Próximo paso: Fase 3 (flujo conversacional de agendamiento)

- FSM real sobre `ConversationState` (pasos: elegir servicio → fecha → hora → confirmar).
- Login/registro del cliente en el bot (el deep link ya da el tenant; falta asociar el chat a un `Client`).
- `InlineKeyboardMarkup` para elegir opciones.
- Reutilizar `GET /api/appointment/slots` + `POST /api/appointment/save` (Fase 1).
