-- V4: Recuerda el canal de cada conversación para que el router responda por el
-- mismo canal aunque el usuario aún no esté registrado como Client/Stylist
-- (caso del primer mensaje de WhatsApp por deep link wa.me/?text=tenantKey).
ALTER TABLE conversation_state ADD COLUMN channel ENUM('TELEGRAM','WHATSAPP');
