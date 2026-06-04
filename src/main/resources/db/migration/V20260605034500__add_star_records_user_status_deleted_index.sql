-- radar 집계 쿼리(countCompletedByCompetency) 최적화용 복합 인덱스
-- WHERE sr.user_id = ? AND sr.status = 'TAGGED' AND sr.is_deleted = false
-- CONCURRENTLY: 인덱스 빌드 중 쓰기 락 없이 운영 트래픽 영향 최소화
-- Flyway non-transactional migration 필요 (executeInTransaction=false)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_star_records_user_status_deleted
    ON star_records (user_id, status, is_deleted);
