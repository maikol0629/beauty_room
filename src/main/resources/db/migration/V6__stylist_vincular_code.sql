-- V6: enlace de vinculación de estilista por deep link (código secreto por estilista).
-- El estilista abre https://t.me/<bot>?start=vincular-<código> y el bot captura su chatId.
ALTER TABLE stylist ADD COLUMN vincular_code VARCHAR(32) NULL;
UPDATE stylist SET vincular_code = SUBSTRING(REPLACE(UUID(), '-', ''), 1, 16)
WHERE vincular_code IS NULL OR vincular_code = '';
ALTER TABLE stylist ADD UNIQUE INDEX uk_stylist_vincular_code (vincular_code);
