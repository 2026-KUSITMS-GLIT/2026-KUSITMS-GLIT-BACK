-- 연속 기록 일수 / 마지막 기록일 (REC-001 — 배지 [N일 연속 기록 중] + 캐릭터 분기)
-- current_streak   : 끊기지 않은 KST 일자 기준 연속 기록 일수. 가입 시 0, 첫 기록 시 1, gap 1 → +1, gap ≥ 2 → 1로 리셋.
-- last_record_date : KST 기준 마지막 기록 일자. 가입 직후 NULL. NULL이면 streak/glaring 모두 false 분기(가입 직후=기본 캐릭터).
-- 백필은 본 마이그레이션에선 수행하지 않는다. 기존 유저는 default(0/NULL)로 시작하고, 다음 기록 시점부터 정확히 누적된다.
ALTER TABLE users
    ADD COLUMN current_streak   SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN last_record_date DATE;
