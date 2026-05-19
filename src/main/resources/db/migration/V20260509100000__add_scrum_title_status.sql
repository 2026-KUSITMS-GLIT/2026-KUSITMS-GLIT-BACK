ALTER TABLE scrum_titles
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMMITTED',
    ADD CONSTRAINT ck_scrum_titles_status CHECK (status IN ('PENDING', 'COMMITTED'));