-- radar 집계 쿼리(countCompletedByCompetency) 최적화용 복합 인덱스
-- WHERE sr.user_id = ? AND sr.status = 'TAGGED' AND sr.is_deleted = false
CREATE INDEX idx_star_records_user_status_deleted
    ON star_records (user_id, status, is_deleted);
