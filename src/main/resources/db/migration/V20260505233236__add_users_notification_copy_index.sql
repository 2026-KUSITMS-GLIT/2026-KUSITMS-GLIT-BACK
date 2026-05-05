-- 알림 카피 라운드로빈 인덱스 (MYP-004 / 기획 A — A→B→C→A 순차 발송)
-- 발송 후 (current + 1) % pool_size 로 갱신. 가입 시 0부터 시작해 첫 카피(A)부터 받는다.
ALTER TABLE users
    ADD COLUMN notification_copy_index SMALLINT NOT NULL DEFAULT 0;
