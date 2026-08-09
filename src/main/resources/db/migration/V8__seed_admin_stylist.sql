-- Admin-estilista del tenant 1 (Salón María): rol ADMIN en users + perfil en stylist
-- (el administrador del salón también opera como estilista; mismo id en ambas tablas).
-- Idempotente (INSERT IGNORE): no pisa si ya existe por email o id.
INSERT IGNORE INTO users (id, email, password, role, tenant_id) VALUES (7, 'admin@example.com', '$2a$10$ijbBrguZGkrCu2l4FBiY2uo8FQboCLHdO3e2vm0hRe3LDc/.dQP8K', 'ADMIN', 1);
INSERT IGNORE INTO stylist (id, name_stylist, phone, id_stylist_room, telegram_chat_id) VALUES (7, 'María López', '555000777', 1, NULL);
