-- 003-create-books.sql
--liquibase formatted sql
--changeset library:003-books

CREATE SEQUENCE books_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE books (
                       id                  BIGINT PRIMARY KEY DEFAULT nextval('books_seq'),
                       title               VARCHAR(255) NOT NULL,
                       isbn                VARCHAR(20) UNIQUE,
                       author_id           BIGINT NOT NULL REFERENCES authors(id),
                       total_copies        INT NOT NULL DEFAULT 1,
                       available_copies    INT NOT NULL DEFAULT 1,
                       genre               VARCHAR(100),
                       publication_year    INT NOT NULL,
                       price               BIGINT,
                       created_at          TIMESTAMP NOT NULL,
                       updated_at          TIMESTAMP NOT NULL,
                       CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);