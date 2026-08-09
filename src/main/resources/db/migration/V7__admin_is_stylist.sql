-- V7: el administrador del salón también es estilista (una fila en users con
-- role ADMIN + su perfil en stylist, mismo id por herencia JOINED).

-- 1) phone pasa a ser opcional (el admin-estilista puede no tener teléfono).
ALTER TABLE stylist MODIFY phone VARCHAR(255) NULL;

-- 2) Backfill: los admins existentes (User pelados con role=ADMIN y sin fila
--    stylist) se convierten en estilistas, usando el nombre del salón.
INSERT INTO stylist (id, name_stylist, phone)
SELECT u.id, t.name, NULL
FROM users u
JOIN tenant t ON t.id = u.tenant_id
WHERE u.role = 'ADMIN'
  AND u.id NOT IN (SELECT id FROM stylist);
