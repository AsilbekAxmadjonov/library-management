CREATE UNIQUE INDEX idx_reservation_member_book_active
    ON reservations(member_id, book_id)
    WHERE status IN ('WAITING', 'NOTIFIED');