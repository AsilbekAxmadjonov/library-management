CREATE INDEX idx_fines_status ON fines(status);

CREATE INDEX idx_loans_status_due_date ON loans(status, due_date);