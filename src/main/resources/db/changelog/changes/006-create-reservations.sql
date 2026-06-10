-- 006-create-reservations.sql
--liquibase formatted sql
--changeset library:006-reservations

CREATE SEQUENCE reservations_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE reservations (
                              id              BIGINT PRIMARY KEY DEFAULT nextval('reservations_seq'),
                              member_id       BIGINT NOT NULL REFERENCES members(id),
                              book_id         BIGINT NOT NULL REFERENCES books(id),
                              reserved_at     TIMESTAMP NOT NULL,
                              status          VARCHAR(30) NOT NULL DEFAULT 'WAITING',
                              expires_at      DATE,
                              created_at      TIMESTAMP NOT NULL,
                              updated_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_reservations_book_status ON reservations(book_id, status);