# Guía rápida — WhatsApp (Meta Cloud API)

Puesta en marcha del canal de WhatsApp para el bot de Beauty Room. Modelo A: **un
solo WABA con un solo número compartido** (todos los salones responden desde el mismo
número; el tenant se resuelve por deep link o por chat conocido).

## 1. Requisitos

1. **App en Meta for Developers** (https://developers.facebook.com).
2. **WABA** (WhatsApp Business Account) con un **número de teléfono verificado**
   (puede ser un número de prueba en sandbox mientras sea MVP).
3. **Token de acceso** de la app con permiso `whatsapp_business_messaging` y
   `whatsapp_business_management`.
4. **App secret** de la app (se usa para verificar la firma de los webhooks).
5. **Templates** aprobados por Meta para mensajes proactivos (ver
   `docs/whatsapp-templates-guia.md`).
6. **HTTPS público** apuntando a la app (el mismo túnel que ya usás para Telegram,
   ej: ngrok).

## 2. Configuración

Completá estas variables (ver `.env.example`):

```properties
whatsapp.access-token=<token de acceso>
whatsapp.phone-number-id=<id del número dentro del WABA>
whatsapp.phone-number=<número con código de país, sin +, ej: 5491101234567>
whatsapp.verify-token=<token propio, el que ponés en el dashboard de Meta>
whatsapp.app-secret=<app secret de la app de Meta>
whatsapp.webhook-url=https://TU-TUNEL/api/whatsapp/webhook
```

El canal queda desactivado si `access-token` y `phone-number-id` están vacíos.

## 3. Registrar el webhook en Meta

En el dashboard de la app → **WhatsApp → Configuration**:

- **Callback URL:** `https://TU-TUNEL/api/whatsapp/webhook`
- **Verify token:** el valor de `WHATSAPP_VERIFY_TOKEN`.

Meta hace un `GET` con `hub.mode=subscribe`, `hub.verify_token` y `hub.challenge`;
el controller devuelve el `challenge` si el token coincide.

## 4. Suscribirse a los eventos

En **Webhook → WhatsApp → Manage**: suscribirse al campo **`messages`** para recibir
los mensajes de los clientes.

## 5. Link público (deep linking)

- Panel → **Link público** (`/panel/public`): muestra el link de Telegram y el de
  WhatsApp con su QR.
- WhatsApp: `https://wa.me/<WHATSAPP_PHONE_NUMBER>?text=<tenantKey>`.
  El cliente toca el link, se abre el chat con el texto `tenantKey` prefabricado y al
  enviarlo se resuelve el tenant (ver `docs/plan-integracion-whatsapp.md`).

## 6. Probar

1. Levantar la app y el túnel.
2. Configurar el webhook en Meta (paso 3).
3. Mandar un mensaje de WhatsApp al número: debería responder con la guía (si el chat
   no es conocido) o con el menú del tenant (si el `tenantKey` se envió antes).
4. Probar el flujo completo de agendamiento (servicio → fecha → hora → confirmar),
   "Mis citas" y "Cancelar cita".

## 7. Mensajes proactivos

Los recordatorios (24h/2h) y el resumen diario del estilista se envían por el canal
preferido del usuario (WhatsApp si tiene `whatsappChatId`). Por WhatsApp requieren
**templates aprobados** (`appointment_reminder_24h`, `appointment_reminder_2h`,
`daily_summary`); si el template no existe/aprueba/falla, el envío cae a texto libre
(solo funciona dentro de la ventana de 24h de Meta tras un mensaje del usuario).

## 8. Troubleshooting

| Síntoma | Causa / solución |
|---|---|
| `No se puede verificar la firma` (403 en logs) | `whatsapp.app-secret` mal o vacío; el POST se rechaza por seguridad. |
| Webhook verificado pero no llegan mensajes | Faltó suscribir el campo `messages` en Webhook → WhatsApp. |
| `[403] User hasn't authorized` / `(#131030)` | Token sin permiso `whatsapp_business_messaging`. |
| Template falla al enviar | Template no aprobado o parámetros que no coinciden con `{{1}}, {{2}}...`. |
| Recordatorio de WhatsApp llega como texto plano | Template no aprobado; el sistema cae al fallback. |
