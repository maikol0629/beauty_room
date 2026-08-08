-- V1: Baseline del esquema generado por Hibernate (Fase 2 del plan de mejora).
-- Este script replica EXACTAMENTE el DDL que Hibernate 6.4 genera con MySQLDialect
-- sobre MariaDB para las entidades actuales. Es la fuente de verdad del esquema
-- a partir de la cual Flyway valida (ddl-auto=validate).
-- Generado a partir del dump de hibernate (schema-generation.scripts.action=create).

CREATE TABLE tenant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    trial_ends_at DATETIME(6),
    name VARCHAR(255) NOT NULL,
    tenant_key VARCHAR(255) NOT NULL,
    plan ENUM ('TRIAL','BASIC','PREMIUM'),
    status ENUM ('ACTIVE','SUSPENDED','CANCELLED'),
    PRIMARY KEY (id),
    CONSTRAINT UK_tenant_name UNIQUE (name),
    CONSTRAINT UK_tenant_tenant_key UNIQUE (tenant_key)
) ENGINE=InnoDB;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM ('STYLIST','CLIENT','ADMIN'),
    PRIMARY KEY (id),
    CONSTRAINT UK_users_email UNIQUE (email),
    CONSTRAINT FK_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

CREATE TABLE stylist_room (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    address VARCHAR(255) NOT NULL,
    name_room VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_stylist_room_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

CREATE TABLE stylist (
    id BIGINT NOT NULL,
    id_stylist_room BIGINT,
    name_stylist VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    telegram_chat_id VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_stylist_user FOREIGN KEY (id) REFERENCES users (id),
    CONSTRAINT FK_stylist_room FOREIGN KEY (id_stylist_room) REFERENCES stylist_room (id)
) ENGINE=InnoDB;

CREATE TABLE client (
    id BIGINT NOT NULL,
    name_client VARCHAR(255),
    phone VARCHAR(255),
    telegram_chat_id VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_client_user FOREIGN KEY (id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE service (
    id BIGINT NOT NULL AUTO_INCREMENT,
    duration INTEGER NOT NULL,
    price FLOAT(23) NOT NULL,
    id_stylist BIGINT,
    tenant_id BIGINT NOT NULL,
    description VARCHAR(255),
    name_service VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_service_stylist FOREIGN KEY (id_stylist) REFERENCES stylist (id),
    CONSTRAINT FK_service_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

CREATE TABLE appointment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    end_date DATETIME(6),
    id_client BIGINT NOT NULL,
    id_service BIGINT NOT NULL,
    id_stylist BIGINT NOT NULL,
    start_date DATETIME(6),
    tenant_id BIGINT NOT NULL,
    status ENUM ('PENDING','CONFIRMED','REJECTED','CANCELLED','COMPLETED','NO_SHOW'),
    PRIMARY KEY (id),
    CONSTRAINT FK_appointment_client FOREIGN KEY (id_client) REFERENCES client (id),
    CONSTRAINT FK_appointment_service FOREIGN KEY (id_service) REFERENCES service (id),
    CONSTRAINT FK_appointment_stylist FOREIGN KEY (id_stylist) REFERENCES stylist (id),
    CONSTRAINT FK_appointment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

CREATE TABLE blocked_slot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    end_date DATETIME(6) NOT NULL,
    id_stylist BIGINT NOT NULL,
    start_date DATETIME(6) NOT NULL,
    tenant_id BIGINT NOT NULL,
    reason VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_blocked_slot_stylist FOREIGN KEY (id_stylist) REFERENCES stylist (id),
    CONSTRAINT FK_blocked_slot_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

CREATE TABLE conversation_state (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT,
    updated_at DATETIME(6) NOT NULL,
    chat_id VARCHAR(100) NOT NULL,
    current_step VARCHAR(100),
    data TEXT,
    PRIMARY KEY (id),
    CONSTRAINT UK_conversation_state_chat_id UNIQUE (chat_id)
) ENGINE=InnoDB;

CREATE TABLE notification (
    appointment_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    sent_at DATETIME(6),
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status ENUM ('PENDING','SENT','FAILED'),
    type ENUM ('EMAIL','SMS','REMINDER_24H','REMINDER_2H','DAILY_SUMMARY'),
    PRIMARY KEY (id),
    CONSTRAINT FK_notification_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

CREATE TABLE payment (
    amount FLOAT(53) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_appointment BIGINT NOT NULL,
    paid_at DATETIME(6),
    tenant_id BIGINT NOT NULL,
    currency VARCHAR(255),
    method VARCHAR(255),
    transaction_id VARCHAR(255),
    status ENUM ('PENDING','PAID','REFUNDED','FAILED'),
    PRIMARY KEY (id),
    CONSTRAINT FK_payment_appointment FOREIGN KEY (id_appointment) REFERENCES appointment (id),
    CONSTRAINT FK_payment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

CREATE TABLE review (
    rating INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_appointment BIGINT,
    id_client BIGINT NOT NULL,
    id_stylist BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    comment TEXT,
    PRIMARY KEY (id),
    CONSTRAINT FK_review_appointment FOREIGN KEY (id_appointment) REFERENCES appointment (id),
    CONSTRAINT FK_review_client FOREIGN KEY (id_client) REFERENCES client (id),
    CONSTRAINT FK_review_stylist FOREIGN KEY (id_stylist) REFERENCES stylist (id),
    CONSTRAINT FK_review_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;

CREATE TABLE stylist_schedule (
    end_time TIME(6),
    start_time TIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    stylist_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    day ENUM ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'),
    PRIMARY KEY (id),
    CONSTRAINT FK_stylist_schedule_stylist FOREIGN KEY (stylist_id) REFERENCES stylist (id),
    CONSTRAINT FK_stylist_schedule_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE=InnoDB;
