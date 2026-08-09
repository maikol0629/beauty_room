-- V5: agrega el rol SUPER_ADMIN al enum de users.role (Panel Super Admin).
-- Los perfiles con ddl-auto=validate exigen que el enum de BD incluya el valor;
-- el perfil test (create-drop) lo genera solo desde la entidad Role.
ALTER TABLE users
    MODIFY COLUMN role ENUM ('STYLIST','CLIENT','ADMIN','SUPER_ADMIN');
