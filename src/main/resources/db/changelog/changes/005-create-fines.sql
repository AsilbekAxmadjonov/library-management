CREATE SEQUENCE fines_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE fines (
                       id                  BIGINT PRIMARY KEY DEFAULT nextval('fines_seq'),
                       loan_id             BIGINT NOT NULL UNIQUE REFERENCES loans(id),
                       amount              BIGINT NOT NULL DEFAULT 0,
                       status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                       paid_at             TIMESTAMP,
                       calculated_up_to    DATE NOT NULL,
                       created_at          TIMESTAMP NOT NULL,
                       updated_at          TIMESTAMP NOT NULL
);