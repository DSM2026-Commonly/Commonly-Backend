# 토큰 재발급

액세스 토큰 만료 시 재로그인 없이 새 토큰을 받는 경로. 기존에는 액세스 토큰만 있었고 만료되면 재로그인 외에 방법이 없었다.

---

## 1. 스키마

`src/main/resources/schema.sql`에 넣었으므로 **수동 DDL은 필요 없다.**

```sql
CREATE TABLE IF NOT EXISTS refresh_token (
    refresh_token_id BIGSERIAL PRIMARY KEY,
    account_id       VARCHAR(50)  NOT NULL,
    token            VARCHAR(512) NOT NULL UNIQUE,
    expires_at       TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_account_id ON refresh_token(account_id);
```

`refresh_token`은 JPA 엔티티라 `ddl-auto`가 알고는 있지만, `validate` 모드에서는 **존재를 확인만 하고 만들어주지 않는다.** 수동 DDL에 맡기면 빠뜨리는 순간 기동이 실패하므로(`document_number_seq`에서 이미 밟은 문제) 같은 방식으로 `schema.sql`에 넣었다.

`spring.sql.init.mode: always` + `IF NOT EXISTS`라 매 기동마다 안전하게 돌고, `defer-datasource-initialization` 기본값이 `false`라 **Hibernate 검증보다 먼저 실행된다.**

> schema.sql 정의가 엔티티와 어긋나면 `validate`가 기동 시 잡아낸다. 드리프트 감지로 쓸 수 있다.

한 계정에 여러 행이 있을 수 있다(기기별 로그인).

## 2. 설계

### 2-1. 리프레시 토큰을 DB에 둔 이유

JWT라 서명만으로도 검증되지만, 그러면 무효화할 방법이 없다 — 탈취돼도 만료까지 그대로 살아 있고 로그아웃도 구현할 수 없다. **행이 있어야 유효한 것으로 보고, 지우면 즉시 무효**가 되게 했다.

Redis가 스택에 없어 Postgres 테이블을 썼다. 만료 행은 재발급 때 해당 계정 것만 같이 지운다(별도 스케줄러 없음).

### 2-2. 토큰 타입 claim

액세스와 리프레시는 **같은 키로 서명**되고 subject도 같다. 구분이 없으면 리프레시 토큰을 그대로 `Authorization` 헤더에 넣어 API를 호출할 수 있다. 모든 토큰에 `typ` claim(`ACCESS` / `REFRESH`)을 박고 쓰는 쪽에서 확인한다. 타입이 달라도 `INVALID_TOKEN`으로만 응답하고 어느 타입이었는지는 알려주지 않는다.

> 기존에 발급된 토큰에는 `typ`이 없으므로 **전부 무효가 된다.** 배포 후 모든 사용자가 재로그인해야 한다.

### 2-3. jti

`iat`/`exp`가 초 단위라 같은 계정이 1초 안에 두 번 발급받으면 payload가 같아지고 토큰 문자열까지 똑같이 나온다. `token`이 UNIQUE라 두 번째 로그인이 500으로 실패한다. 모든 토큰에 `jti`(UUID)를 넣어 막았다. **검증 중 실제로 밟았다.**

### 2-4. 회전

재발급하면 쓴 리프레시 토큰을 폐기하고 새로 발급한다. 같은 토큰을 두 번 쓸 수 없어 탈취된 토큰이 무한정 쓰이지 않는다.

### 2-5. JwtFilter가 재발급 경로를 건너뛴다

재발급은 액세스 토큰이 만료된 상태에서 부르는 API다. 클라이언트가 만료된 `Authorization` 헤더를 그대로 달고 오면 `JwtFilter`에서 401이 나서 **재발급 자체가 불가능해진다.** `shouldNotFilter`로 이 경로에서는 헤더를 보지 않는다.

## 3. 설정

```yaml
jwt:
  access-exp: ${JWT_ACCESS_EXP:3600}      # 1시간
  refresh-exp: ${JWT_REFRESH_EXP:1209600} # 14일
```

## 4. API

### 4.1 POST `/api/auths/reissue` — 재발급 (인증 불필요)

Request
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

| 필드 | 검증 |
|---|---|
| `refreshToken` | `@NotBlank` |

Response `200`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**응답의 `refreshToken`은 새 값이다.** 회전되므로 클라이언트는 반드시 교체 저장해야 하고, 보낸 토큰은 그 즉시 무효다.

| 상태 | 조건 | 에러 코드 |
|---|---|---|
| 400 | `refreshToken` 누락/공백 | 검증 실패 |
| 401 | 서명 위조, 형식 오류 | `INVALID_TOKEN` |
| 401 | 리프레시 토큰 만료 | `EXPIRED_TOKEN` |
| 401 | 액세스 토큰을 보냄 (`typ` 불일치) | `INVALID_TOKEN` |
| 401 | 이미 회전됐거나 무효화된 토큰 | `REFRESH_TOKEN_NOT_FOUND` |
| 404 | 삭제된 계정 | `USER_NOT_FOUND` |

### 4.2 로그인 / 회원가입 응답 변경

`POST /api/auths/login`, `POST /api/auths/signup` 응답에 `refreshToken`이 **추가**된다. 기존 `accessToken` 필드는 그대로다.

```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

## 5. 남은 것

- **로그아웃 API 없음.** 행만 지우면 되므로 붙이기 쉽다. 지금은 리프레시 토큰을 폐기할 경로가 재발급(회전)뿐이다.
- **만료 행 정리가 재발급 시점에만 일어난다.** 로그인만 하고 재발급을 안 하는 계정의 만료 행은 남는다. 양이 문제되면 배치를 붙인다.
- 비밀번호 변경 시 기존 리프레시 토큰을 전부 폐기할지 정해야 한다. 지금은 살아 있다.
