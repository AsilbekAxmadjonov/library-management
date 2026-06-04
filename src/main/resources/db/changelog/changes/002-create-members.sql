-- 002-create-members.sql
--liquibase formatted sql
--changeset library:002-members

CREATE SEQUENCE members_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE members (
                         id              BIGINT PRIMARY KEY DEFAULT nextval('members_seq'),
                         first_name      VARCHAR(100) NOT NULL,
                         last_name       VARCHAR(100) NOT NULL,
                         email           VARCHAR(150) NOT NULL UNIQUE,
                         phone           VARCHAR(20) UNIQUE,
                         status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                         type            VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
                         created_at      TIMESTAMP NOT NULL,
                         updated_at      TIMESTAMP NOT NULL
);