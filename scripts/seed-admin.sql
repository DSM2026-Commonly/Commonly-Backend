-- =============================================================================
--  담당자(ADMIN) 계정 생성
-- =============================================================================
--  기본값: 아이디 abcd1234 / 비밀번호 abcd1234!
--
--  멱등하다. 이미 있으면 비밀번호를 다시 설정하고 ADMIN 권한을 보장한다.
--  init-db.sql 없이 단독으로도 돌릴 수 있다.
--
--  ⚠️  비밀번호가 약하다. 개발·실증 환경 전용이다. 운영에 쓰려면
--      psql 변수로 다른 해시를 넘겨라 (아래 참고).
--
--  실행: psql -U <user> -d <db> -v ON_ERROR_STOP=1 -f scripts/seed-admin.sql
--  값 바꾸기:
--      psql ... -v account_id=staff01 -v pw_hash='$2a$10$...' -v name=홍길동 -v department=민원과
--
--  다른 비밀번호의 BCrypt 해시가 필요하면:
--      htpasswd -bnBC 10 "" '새비밀번호' | tr -d ':\n' | sed 's/\$2y\$/\$2a\$/'
--  (Spring의 BCryptPasswordEncoder는 strength 10, $2a$ 접두사를 쓴다)
-- =============================================================================

-- psql 변수 기본값. -v로 넘기면 그 값이 이긴다.
\if :{?account_id} \else \set account_id 'abcd1234' \endif
\if :{?pw_hash}    \else \set pw_hash '$2a$10$E19IEv08cIWspHoMUeyb8O8eMyznjZSq1ho7REsvu8sle7Y/v6zA.' \endif
\if :{?name}       \else \set name '관리자' \endif
\if :{?department} \else \set department '민원과' \endif

BEGIN;

WITH upserted_user AS (
    INSERT INTO users (account_id, password, name, password_changed)
    VALUES (:'account_id', :'pw_hash', :'name', TRUE)
    ON CONFLICT (account_id) DO UPDATE
        SET password = EXCLUDED.password,
            name     = EXCLUDED.name,
            -- 초기 비밀번호 강제 변경 상태로 두면 다른 API가 전부 403이다.
            password_changed = TRUE
    RETURNING id
)
INSERT INTO admin (user_id, department, role)
SELECT id, :'department', 'ADMIN' FROM upserted_user
ON CONFLICT (user_id) DO UPDATE
    SET department = EXCLUDED.department,
        role       = 'ADMIN';

COMMIT;

\echo ''
\echo '생성/갱신된 계정:'
SELECT u.id,
       u.account_id        AS "아이디",
       u.name              AS "이름",
       a.role              AS "권한",
       a.department        AS "부서",
       u.password_changed  AS "비밀번호_변경완료"
  FROM users u
  JOIN admin a ON a.user_id = u.id
 WHERE u.account_id = :'account_id';
