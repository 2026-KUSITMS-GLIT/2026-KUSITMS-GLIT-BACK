-- 회원 탈퇴 hard delete 배치(MYP-005) 대상 사용자 조회 최적화.
-- AccountHardDeleteScheduler가 매일 KST 04:00에 실행하는 쿼리:
--   SELECT u.id FROM users u WHERE u.is_deleted = true AND u.hard_delete_at <= now();
-- 풀스캔 회피 + 탈퇴 안 한 사용자는 색인 제외(partial index)로 인덱스 크기 최소화.
-- 운영 데이터셋이 커지면 CREATE INDEX CONCURRENTLY 고려(현재는 MVP 볼륨이라 불필요).
CREATE INDEX idx_users_hard_delete_pending
    ON users (hard_delete_at)
    WHERE is_deleted = true;
