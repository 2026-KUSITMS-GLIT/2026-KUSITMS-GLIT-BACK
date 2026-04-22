-- =========================================================
-- V20260422210353__relax_users_onboarding_nullable.sql
-- 온보딩 미완료 상태 허용을 위한 users NOT NULL 완화
--
-- 배경:
--  - 소셜 로그인 시점(ONB002)에는 닉네임/직군/상태를 아직 알 수 없으나,
--    JWT 발급을 위해 users row가 먼저 존재해야 한다.
--  - 온보딩 완료 이슈(별도)에서 세 컬럼을 채워 넣으며, 완료 판정은 애플리케이션 레벨에서
--    nickname IS NOT NULL로 수행(추가 플래그 컬럼 없이).
--  - CHECK 제약(ck_users_job_role, ck_users_user_status)은 NULL을 이미 허용(NULL IN (...) = NULL)하므로 유지.
-- =========================================================

ALTER TABLE users ALTER COLUMN nickname    DROP NOT NULL;
ALTER TABLE users ALTER COLUMN job_role    DROP NOT NULL;
ALTER TABLE users ALTER COLUMN user_status DROP NOT NULL;
