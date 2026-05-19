-- [로컬 환경 주의] 기존 star_images 더미 데이터가 있으면 Flyway 실패.
-- 마이그레이션 전 아래 명령 실행 필요:
--   TRUNCATE TABLE star_images CASCADE;
ALTER TABLE star_images
    ADD COLUMN image_key VARCHAR(500) NOT NULL;