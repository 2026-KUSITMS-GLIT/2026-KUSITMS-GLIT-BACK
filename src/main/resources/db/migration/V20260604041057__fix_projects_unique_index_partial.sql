-- (user_id, name, is_deleted) 전체를 unique key로 잡던 기존 인덱스를
-- 활성 row(is_deleted=false)만 unique를 보장하는 부분 인덱스로 전환한다.

DROP INDEX uq_projects_user_name_is_deleted;

CREATE UNIQUE INDEX uq_projects_user_name_active
    ON projects (user_id, name)
    WHERE is_deleted = FALSE;