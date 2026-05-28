-- social_accounts.email을 평문에서 HMAC-SHA-256(hex) 단방향 해시로 일괄 전환한다.
-- pepper는 Flyway placeholder로 주입되며 yaml의 `social.email-hash.pepper`와 동일한 값이다.
-- 이미 64자 hex 형식이면 이전에 해시된 row로 간주하고 건너뛴다(재실행 안전성).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE social_accounts
SET email = encode(hmac(email, '${social_email_hash_pepper}', 'sha256'), 'hex')
WHERE email IS NOT NULL
  AND email <> ''
  AND email !~ '^[0-9a-f]{64}$';
