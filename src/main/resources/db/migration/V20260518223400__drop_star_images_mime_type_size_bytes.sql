-- [롤백 SQL]
-- 컬럼 재생성이 필요한 경우 아래 구문을 실행한다.
--   ALTER TABLE star_images
--       ADD COLUMN mime_type  VARCHAR(30),
--       ADD COLUMN size_bytes BIGINT;
-- 단, 삭제 전 데이터는 복구 불가하므로 스냅샷이 있는 경우 복원 후 실행한다.
--
-- [데이터 손실 영향 / 백필 불필요 근거]
-- mime_type, size_bytes 두 컬럼은 Presigned URL 발급 시 요청값을 저장만 했을 뿐
-- 이후 조회·비즈니스 로직에서 참조된 적이 없다. 이미지 표시에는 image_url만 사용되며,
-- mime_type은 S3 오브젝트 메타데이터로 관리되고 size_bytes는 현재 요구사항상 불필요하다.
-- 따라서 기존 데이터 손실이 서비스에 영향을 주지 않으며 별도 백필이 필요 없다.
--
-- [배포 절차 - stg → prod 순차 배포]
-- 1. mime_type·size_bytes 참조 코드를 제거한 애플리케이션을 stg에 먼저 배포한다.
-- 2. Flyway가 stg DB에 이 마이그레이션을 자동 적용한다.
-- 3. stg에서 이미지 업로드·조회·삭제 정상 동작을 확인한다.
-- 4. 동일 코드를 prod에 배포하면 Flyway가 prod DB에도 자동 적용된다.
-- 각 환경은 코드 배포와 마이그레이션이 항상 동시에 적용되므로
-- 구버전 코드가 삭제된 컬럼에 쓰는 상황은 발생하지 않는다.
ALTER TABLE star_images
    DROP COLUMN mime_type,
    DROP COLUMN size_bytes;