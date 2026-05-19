-- is_completed → status(WRITING/WRITTEN/TAGGED) 로 대체
ALTER TABLE star_records
    ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'WRITING',
    ADD CONSTRAINT ck_star_records_status CHECK (status IN ('WRITING', 'WRITTEN', 'TAGGED'));

-- 기존 is_completed=true 인 레코드를 TAGGED로 전환
UPDATE star_records SET status = 'TAGGED' WHERE is_completed = TRUE AND is_deleted = FALSE;

DROP INDEX idx_star_records_user_is_completed;

ALTER TABLE star_records
    DROP COLUMN is_completed;