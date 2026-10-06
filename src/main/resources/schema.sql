-- JPA 엔티티가 아니라 ddl-auto가 못 만든다. 네이티브 SQL(DocumentNumberGenerator) 전용.
CREATE TABLE IF NOT EXISTS document_number_seq (
    "year"   INT PRIMARY KEY,
    last_no  BIGINT NOT NULL
);

-- refresh_token은 JPA 엔티티라 ddl-auto가 알고는 있지만, validate 모드에서는
-- 존재를 확인만 하고 만들어주지 않는다. 수동 DDL을 빠뜨리면 기동이 실패하므로 여기서 만든다.
-- 이 스크립트는 Hibernate 검증보다 먼저 실행된다(defer-datasource-initialization 기본값 false).
CREATE TABLE IF NOT EXISTS refresh_token (
    refresh_token_id BIGSERIAL PRIMARY KEY,
    account_id       VARCHAR(50)  NOT NULL,
    token            VARCHAR(512) NOT NULL UNIQUE,
    expires_at       TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_account_id ON refresh_token(account_id);
