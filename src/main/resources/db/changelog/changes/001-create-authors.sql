

CREATE SEQUENCE authors_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE authors (
                         id              BIGINT PRIMARY KEY DEFAULT nextval('authors_seq'),
                         first_name      VARCHAR(100) NOT NULL,
                         last_name       VARCHAR(100) NOT NULL,
                         bio             VARCHAR(500),
                         created_at      TIMESTAMP NOT NULL,
                         updated_at      TIMESTAMP NOT NULL
);