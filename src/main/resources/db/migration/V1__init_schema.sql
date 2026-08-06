-- Initial schema migration for Beauty Room
-- This file creates the base schema expected by the application.

CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_key VARCHAR(255),
    name VARCHAR(255),
    plan VARCHAR(50),
    status VARCHAR(50),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    email VARCHAR(255),
    name VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(50),
    tenant_id BIGINT,
    updated_at DATETIME(6),
    telegram_chat_id BIGINT,
    dtype VARCHAR(31) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS client (
    id BIGINT NOT NULL,
    name_client VARCHAR(255),
    phone VARCHAR(255),
    telegram_chat_id VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_client_user FOREIGN KEY (id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stylist (
    id BIGINT NOT NULL,
    name_stylist VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    telegram_chat_id VARCHAR(255),
    id_stylist_room BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_stylist_user FOREIGN KEY (id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS service (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    description VARCHAR(255),
    duration INTEGER,
    name VARCHAR(255),
    price DOUBLE,
    tenant_id BIGINT,
    updated_at DATETIME(6),
    stylist_id BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    end_time DATETIME(6),
    start_time DATETIME(6),
    status VARCHAR(50),
    tenant_id BIGINT,
    updated_at DATETIME(6),
    client_id BIGINT,
    service_id BIGINT,
    stylist_id BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS blocked_slot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    end_time DATETIME(6),
    start_time DATETIME(6),
    tenant_id BIGINT,
    updated_at DATETIME(6),
    stylist_id BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS conversation_state (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    data VARCHAR(1000),
    step VARCHAR(255),
    tenant_id BIGINT,
    updated_at DATETIME(6),
    chat_id BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT,
    created_at DATETIME(6),
    message VARCHAR(1000),
    tenant_id BIGINT,
    type VARCHAR(50),
    updated_at DATETIME(6),
    user_id BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
