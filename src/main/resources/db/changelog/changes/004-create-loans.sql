

CREATE SEQUENCE loans_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE loans (
                       id              BIGINT PRIMARY KEY DEFAULT nextval('loans_seq'),
                       member_id       BIGINT NOT NULL REFERENCES members(id),
                       book_id         BIGINT NOT NULL REFERENCES books(id),
                       loan_date       DATE NOT NULL,
                       due_date        DATE NOT NULL,
                       return_date     DATE,
                       status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       extension_count INT NOT NULL DEFAULT 0,
                       created_at      TIMESTAMP NOT NULL,
                       updated_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_loans_member_id ON loans(member_id);
CREATE INDEX idx_loans_book_id ON loans(book_id);
CREATE INDEX idx_loans_status ON loans(status);