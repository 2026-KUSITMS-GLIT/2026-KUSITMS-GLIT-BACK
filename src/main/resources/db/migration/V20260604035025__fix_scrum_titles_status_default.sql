-- V20260509100000에서 ADD COLUMN status DEFAULT 'COMMITTED'로 추가되어
-- JPA 엔티티의 기본값(PENDING)과 불일치가 발생한 문제를 수정한다.

-- 1. 컬럼 DEFAULT를 JPA 엔티티(ScrumTitle.status = PENDING)와 일치시킨다.
ALTER TABLE scrum_titles
    ALTER COLUMN status SET DEFAULT 'PENDING';

-- 2. ADD COLUMN 시 잘못 채워진 기존 row를 보정한다.
--    STAR 완료 트리거는 "해당 날짜의 모든 scrum_title을 COMMITTED"이므로,
--    산하 scrum의 날짜+유저 기준으로 완료된 star_record가 하나라도 있으면 COMMITTED 유지,
--    그렇지 않으면 DEFAULT로 잘못 설정된 것으로 보고 PENDING으로 되돌린다.
UPDATE scrum_titles st
SET status = 'PENDING'
WHERE st.status = 'COMMITTED'
  AND NOT EXISTS (
      SELECT 1
      FROM scrums s1
      JOIN scrums s2
          ON s2.user_id = s1.user_id
         AND s2.scrum_date = s1.scrum_date
      JOIN star_records sr ON sr.scrum_id = s2.id
      WHERE s1.title_id = st.id
        AND s1.is_deleted = false
        AND sr.is_completed = true
  );