ALTER TABLE star_records
    ADD COLUMN is_completed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE star_records
SET is_completed = TRUE
WHERE status = 'TAGGED';