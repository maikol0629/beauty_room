-- Tenants (IDs 1, 2 y 3 asignados por IDENTITY en orden)
INSERT INTO tenant (name, tenant_key, plan, status, created_at, trial_ends_at) VALUES ('Salón María', 'salon-maria-001', 'TRIAL', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY));
INSERT INTO tenant (name, tenant_key, plan, status, created_at, trial_ends_at) VALUES ('Estilos Ana', 'estilos-ana-001', 'TRIAL', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY));
INSERT INTO tenant (name, tenant_key, plan, status, created_at, trial_ends_at) VALUES ('Beauty Room Pro', 'beauty-room-pro-001', 'BASIC', 'ACTIVE', NOW(), NULL);

-- Insertar en StylistRoom (sin dependencias, ahora con tenant_id)
INSERT INTO stylist_room (id, name_room, address, tenant_id) VALUES (1, 'Room A', '123 Main Street', 1);
INSERT INTO stylist_room (id, name_room, address, tenant_id) VALUES (2, 'Room B', '456 Elm Street', 2);
INSERT INTO stylist_room (id, name_room, address, tenant_id) VALUES (3, 'Room C', '789 Oak Avenue', 3);

-- Insertar en tabla base Users (JOINED inheritance)
-- Los IDs 1, 2 y 5 serán usados por Stylist; 3, 4 y 6 por Client
-- Passwords en BCrypt (hash de "password")
-- tenant_id 1 = Salón María, tenant_id 2 = Estilos Ana, tenant_id 3 = Beauty Room Pro
INSERT INTO users (id, email, password, role, tenant_id) VALUES (1, 'john@example.com', '$2a$10$ijbBrguZGkrCu2l4FBiY2uo8FQboCLHdO3e2vm0hRe3LDc/.dQP8K', 'STYLIST', 1);
INSERT INTO users (id, email, password, role, tenant_id) VALUES (2, 'jane@example.com', '$2a$10$ijbBrguZGkrCu2l4FBiY2uo8FQboCLHdO3e2vm0hRe3LDc/.dQP8K', 'STYLIST', 2);
INSERT INTO users (id, email, password, role, tenant_id) VALUES (3, 'alice@example.com', '$2a$10$ijbBrguZGkrCu2l4FBiY2uo8FQboCLHdO3e2vm0hRe3LDc/.dQP8K', 'CLIENT', 1);
INSERT INTO users (id, email, password, role, tenant_id) VALUES (4, 'bob@example.com', '$2a$10$ijbBrguZGkrCu2l4FBiY2uo8FQboCLHdO3e2vm0hRe3LDc/.dQP8K', 'CLIENT', 2);
-- Usuarios de prueba del tenant 3 (Beauty Room Pro)
INSERT INTO users (id, email, password, role, tenant_id) VALUES (5, 'carlos@example.com', '$2a$10$ijbBrguZGkrCu2l4FBiY2uo8FQboCLHdO3e2vm0hRe3LDc/.dQP8K', 'STYLIST', 3);
INSERT INTO users (id, email, password, role, tenant_id) VALUES (6, 'diana@example.com', '$2a$10$ijbBrguZGkrCu2l4FBiY2uo8FQboCLHdO3e2vm0hRe3LDc/.dQP8K', 'CLIENT', 3);

-- Insertar en Stylist (usa el mismo id que users)
INSERT INTO stylist (id, name_stylist, phone, id_stylist_room, telegram_chat_id) VALUES (1, 'John Doe', '123456789', 1, '555000111');
INSERT INTO stylist (id, name_stylist, phone, id_stylist_room, telegram_chat_id) VALUES (2, 'Jane Smith', '987654321', 2, '555000222');
INSERT INTO stylist (id, name_stylist, phone, id_stylist_room, telegram_chat_id) VALUES (5, 'Carlos Ruiz', '555000333', 3, '555000333');

-- Insertar en Client (usa el mismo id que users)
INSERT INTO client (id, name_client, phone, telegram_chat_id) VALUES (3, 'Alice Johnson', '5551234', '111111111');
INSERT INTO client (id, name_client, phone, telegram_chat_id) VALUES (4, 'Bob Brown', '5555678', '222222222');
INSERT INTO client (id, name_client, phone, telegram_chat_id) VALUES (6, 'Diana Lopez', '555000666', '333333333');

-- Insertar en Service (relación ManyToOne con Stylist, ahora con tenant_id)
INSERT INTO service (id, name_service, description, price, duration, id_stylist, tenant_id) VALUES (1, 'Haircut', 'Basic haircut', 15.99, 30, 1, 1);
INSERT INTO service (id, name_service, description, price, duration, id_stylist, tenant_id) VALUES (2, 'Coloring', 'Hair coloring service', 40.00, 90, 2, 2);
INSERT INTO service (id, name_service, description, price, duration, id_stylist, tenant_id) VALUES (3, 'Manicure', 'Classic manicure with polish', 20.00, 45, 5, 3);

-- Insertar en Appointment (ManyToOne con Client, Stylist y Service, ahora con tenant_id)
-- Fechas relativas a NOW() para poder probar agenda y recordatorios; status variados para el panel
INSERT INTO appointment (id, start_date, end_date, id_client, id_stylist, id_service, tenant_id, status) VALUES (1, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 30 MINUTE, 3, 1, 1, 1, 'CONFIRMED');
INSERT INTO appointment (id, start_date, end_date, id_client, id_stylist, id_service, tenant_id, status) VALUES (2, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY) + INTERVAL 90 MINUTE, 4, 2, 2, 2, 'COMPLETED');
INSERT INTO appointment (id, start_date, end_date, id_client, id_stylist, id_service, tenant_id, status) VALUES (3, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 30 MINUTE, 3, 1, 1, 1, 'NO_SHOW');
INSERT INTO appointment (id, start_date, end_date, id_client, id_stylist, id_service, tenant_id, status) VALUES (4, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 45 MINUTE, 6, 5, 3, 3, 'PENDING');

-- Insertar en StylistSchedule (Horarios de los estilistas, ahora con tenant_id)
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time, tenant_id) VALUES (1, 'MONDAY', '09:00:00', '18:00:00', 1);
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time, tenant_id) VALUES (1, 'WEDNESDAY', '10:00:00', '17:00:00', 1);
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time, tenant_id) VALUES (2, 'TUESDAY', '08:30:00', '16:30:00', 2);
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time, tenant_id) VALUES (2, 'THURSDAY', '12:00:00', '20:00:00', 2);
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time, tenant_id) VALUES (5, 'MONDAY', '09:00:00', '18:00:00', 3);
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time, tenant_id) VALUES (5, 'FRIDAY', '09:00:00', '18:00:00', 3);

-- Insertar en BlockedSlot (bloqueos de estilistas, para probar el flujo /bloquear y el panel)
INSERT INTO blocked_slot (id, id_stylist, start_date, end_date, reason, tenant_id) VALUES (1, 1, DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 15 HOUR, DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 16 HOUR, 'Bloqueo por capacitacion', 1);
INSERT INTO blocked_slot (id, id_stylist, start_date, end_date, reason, tenant_id) VALUES (2, 5, DATE_ADD(NOW(), INTERVAL 3 DAY) + INTERVAL 12 HOUR, DATE_ADD(NOW(), INTERVAL 3 DAY) + INTERVAL 13 HOUR, 'Bloqueo de prueba', 3);
