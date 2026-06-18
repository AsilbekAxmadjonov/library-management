--changeset library:007-add-version-to-authors
ALTER TABLE authors ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

--changeset library:007-add-version-to-members
ALTER TABLE members ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

--changeset library:007-add-version-to-books
ALTER TABLE books ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

--changeset library:007-add-version-to-loans
ALTER TABLE loans ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

--changeset library:007-add-version-to-fines
ALTER TABLE fines ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

--changeset library:007-add-version-to-reservations
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;