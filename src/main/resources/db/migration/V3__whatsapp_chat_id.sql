-- V3: Soporte multi-canal. Agrega el chat id de WhatsApp a clientes y estilistas
-- para que un usuario pueda ser contactado por Telegram o WhatsApp (Fase 12).
ALTER TABLE client ADD COLUMN whatsapp_chat_id VARCHAR(255);
ALTER TABLE stylist ADD COLUMN whatsapp_chat_id VARCHAR(255);
