-- 임시저장(PENDING) 단계에서 올라간 projects.title_count를 실측값으로 보정한다.
--
-- 배경:
--   이전 구현은 POST /api/scrums/write(PENDING 저장) 시점에 title_count를 증가시켰고, 되돌리는
--   로직(cleanupPendingSession)은 "같은 날짜에 다시 작성" 경로에서만 동작했다. 따라서 심화기록을
--   완료하지 않고 이탈한 세션의 증가분이 영구히 남아, 연결된 기록이 없는 프로젝트 태그도 삭제할 수
--   없었다 (기록하기 QA 34번).
--   카운터를 STAR 확정 시점으로 옮기는 코드 수정만으로는 이미 쌓인 값이 교정되지 않아 기존 유저의
--   태그는 계속 막히므로, 일회성 보정을 수행한다.
--
-- 집계 기준:
--   살아있고 COMMITTED인 scrum_titles 수. deletable(= title_count == 0)의 의도인 "연결된 기록이
--   있는가"와 일치한다. PENDING 세션은 캘린더·홈 어디에도 노출되지 않으므로 세지 않는다.
--   soft-delete된 project도 함께 보정해 불변식을 균일하게 유지한다(해당 row의 값은 읽히지 않음).
--
-- 성능 및 락 영향:
--   집계는 idx_scrum_titles_project_is_deleted ON scrum_titles(project_id, is_deleted)로 처리되고,
--   실제 값이 달라지는 row만 UPDATE 한다. 초기 서비스 데이터 규모(수천 row 이하)에서 실행 시간 < 1초,
--   락 영향 무시 가능. 향후 대규모 테이블 대상 동일 패턴 마이그레이션 시 EXPLAIN ANALYZE 선행 필요.
--   chk_projects_title_count_non_negative(title_count >= 0)는 COUNT가 음수일 수 없어 충돌하지 않는다.
--   title_count는 SMALLINT이므로 한 프로젝트의 COMMITTED title이 32767개를 넘으면 실패하나,
--   태그당 제목 수 특성상 현실적으로 발생하지 않는다.
--
-- 주의:
--   DeleteScrumTitleService가 제목 삭제 시 title_count를 감소시키지 않는 별도 버그가 남아 있어,
--   그 버그로 부풀려진 값도 이 보정에 함께 교정된다. 코드가 고쳐지기 전까지는 일회성 효과다.
--
-- 롤백 방법:
--   값만 바꾸는 UPDATE이므로 스키마 롤백은 불필요하다. 이전 집계 기준(status 무관)으로 되돌리려면
--   아래 쿼리에서 `AND st.status = 'COMMITTED'` 조건을 제거해 재실행한다.

UPDATE projects p
SET title_count = sub.cnt
FROM (
    SELECT p2.id,
           COUNT(st.id) AS cnt
    FROM projects p2
    LEFT JOIN scrum_titles st
           ON st.project_id = p2.id
          AND st.is_deleted = false
          AND st.status = 'COMMITTED'
    GROUP BY p2.id
) sub
WHERE p.id = sub.id
  AND p.title_count <> sub.cnt;