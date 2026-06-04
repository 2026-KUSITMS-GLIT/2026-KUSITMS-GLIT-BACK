-- (user_id, name, is_deleted) 전체를 unique key로 잡던 기존 인덱스를
-- 활성 row(is_deleted=false)만 unique를 보장하는 부분 인덱스로 전환한다.
--
-- 롤백 방법:
--   DROP INDEX uq_projects_user_name_active;
--   CREATE UNIQUE INDEX uq_projects_user_name_is_deleted ON projects (user_id, name, is_deleted);
--
-- 주의: projects 테이블이 크다면 트래픽이 적은 시간대에 배포 권장 (SHARE 락 획득).

DROP INDEX uq_projects_user_name_is_deleted;

CREATE UNIQUE INDEX uq_projects_user_name_active
    ON projects (user_id, name)
    WHERE is_deleted = FALSE;