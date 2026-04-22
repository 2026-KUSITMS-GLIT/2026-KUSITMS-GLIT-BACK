-- =========================================================
-- V20260422205249__drop_auth_tokens.sql
-- auth_tokens 테이블 제거
--
-- 배경:
--  - 리프레시 토큰 저장을 DB(auth_tokens)에서 Redis(key: refresh:{userId}, TTL)로 일원화.
--  - TTL 자동 만료와 O(1) 조회 이점이 커서 핫패스(재발급/블랙리스트)에 DB 대비 유리.
--  - 동반 제거: 엔티티 AuthToken.java.
-- =========================================================

DROP TABLE IF EXISTS auth_tokens;
