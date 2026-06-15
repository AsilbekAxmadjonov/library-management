-- changeset library:010-rename-id-columns

-- authors: id → author_id
ALTER TABLE authors RENAME COLUMN id TO author_id;

-- books: id → book_id
ALTER TABLE books RENAME COLUMN id TO book_id;

-- members: id → member_id
ALTER TABLE members RENAME COLUMN id TO member_id;

-- loans: id → loan_id
ALTER TABLE loans RENAME COLUMN id TO loan_id;

-- fines: id → fine_id
ALTER TABLE fines RENAME COLUMN id TO fine_id;

-- reservations: id → reservation_id
ALTER TABLE reservations RENAME COLUMN id TO reservation_id;