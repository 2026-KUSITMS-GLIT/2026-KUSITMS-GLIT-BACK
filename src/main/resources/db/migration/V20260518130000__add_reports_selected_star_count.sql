-- RPT: 리포트 생성 시 선택한 심화기록 개수 저장 (커리어 리포트 상세 서브텍스트용)
ALTER TABLE reports ADD COLUMN selected_star_count int;
-- 신규 생성분부터 애플리케이션에서 값 저장
-- 레거시 데이터 백필 정책 확정 후
-- ALTER TABLE reports ALTER COLUMN selected_star_count SET NOT NULL;
