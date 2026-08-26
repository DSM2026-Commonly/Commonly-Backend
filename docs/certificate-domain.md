# certificate 도메인 개발 문서

경력증명서 발급/조회/수정/출력 도메인. 데이터는 이미 적재된 `certificate` 테이블을 그대로 쓰고, 출력물은 유성구청 「경력증명서 서식」(`경력증명서 서식.hwpx`)을 따른다.

- 명세: https://app.notion.com/p/3c82cdd741ac805da67fe1391a1c7feb?v=4882cdd741ac82469c5588f953fb51b3 (`구분 = 경력 증명서`)
- 서식: `경력증명서 서식.hwpx` (대전광역시 유성구청장 발급)
- 선행: [human-domain.md](docs/human-domain.md) — 인적사항(성명/생년월일/주소)은 `humans`에서 온다

---

## 1. 서식 → 데이터 매핑

**이 문서에서 제일 중요한 표.** 나머지는 전부 이 표에서 파생된다.

서식은 표 3개로 되어 있다. 머리말(문서번호/담당자), 본문(인적사항 + 재직사항 10행 + 총계), 발급일/직인.

| 서식 칸 | 값 | 소스 | 상태 |
|---|---|---|---|
| `제 (연도) - ` | 문서번호 `유성구-2026-000001` | 신규 채번 (§3) | 없음 → 만들어야 함 |
| `담 당 자 :` / `연 락 처 :` | 발급 담당자 이름/연락처 | auth 도메인 | **미구현** → §7-1 |
| 성 명 `(한글)` | `humans.name` | human | 있음 |
| 성 명 `(영문)` | — | — | **어디에도 없음.** 공란 출력 |
| 생년월일 | `humans.birth_date` | human | 있음 |
| 주 소 | `humans.address` | human | 있음 (nullable) |
| 재직사항 · 근무기간 `부터` | `certificate.hire_date` | certificate | 있음 (`LocalDate`) |
| 재직사항 · 근무기간 `까지` | `retirement_date` ?? `expiration_date` | certificate | 있음 (`LocalDate`) |
| 재직사항 · 근무부서 | `certificate.department` | certificate | 컬럼만 있음, **데이터 없음** → §1-2 |
| 재직사항 · 담당업무 | `certificate.key_responsibilities` | certificate | 있음 |
| 총 근무기간 `총  개월  일` | 계산값 | 위 근무기간 합산 | 계산 필요 (§3) |
| 퇴직사유 | `certificate.reason` | certificate | 있음 |
| 그 밖의 사항 | `otherMatters` | 발급 요청 body | 저장 안 됨 → §1-3 |
| 용도 | `purpose` | 발급 요청 body | 저장 안 됨 → §1-3 |
| `202 .  .  .` | 발급일 | 발급 시각 | — |
| `대전광역시 유성구청장 (인)` | 직인 이미지 | 파일 | **없음** → §7-1 |

`certificate`에는 있지만 서식에는 **안 들어가는** 컬럼: `gender`, `job_title`, `division`, `employment_type`, `note`. 서식에 성별 칸이 아예 없다. 상세 조회(§5.3)에는 그대로 내려준다.

### 1-1. 재직사항은 10행 고정

서식의 재직사항 표는 데이터 행이 **10행**이다 (`인적사항` 3행 + `재직사항` 헤더 2행 + 데이터 10행 + `총 근무기간`/`그 밖의 사항`/`용도` 3행 = 18행).

`workExperienceIds`가 10개를 넘으면 어떻게 되는지 명세에 없다. 여기서는 **11개 이상이면 400**으로 막는다. 표를 넘겨서 2페이지로 흘리는 건 서식 원형이 깨지고, 조용히 앞 10개만 찍는 건 증명서에서 하면 안 되는 짓이다.

### 1-2. 근무부서는 컬럼만 있고 값이 없다

`certificate.department`(nullable)를 추가해 뒀다. **자리만 잡아둔 것이고 채우는 경로는 아직 없다.**

- `certificate.division`은 부서가 아니다. 엑셀 원본의 `구분(채용,전보,해지,퇴직)` 열이고 `RowValidator`가 그 4개 값으로 검증한다.
- `humans.department`는 **사람당 한 개**라 "2020년엔 총무과, 2023년엔 민원과" 같은 기간별 부서가 표현이 안 된다. 전보(`division = 전보`) 이력이 데이터에 실제로 들어 있는데 부서명이 없다.
- 원본 엑셀(`기간제 근로자 관리서식.xlsx`)에 부서 열이 없다. 그래서 엑셀 업로드 경로(`ColumnMappingTable`)에는 `department`를 **넣지 않았다.** 매핑 대상 필드로 열어봐야 매핑할 열이 없다.

값을 채우려면 원천 데이터 확보가 먼저다 (§7-1). 그때까지 서식의 근무부서 칸은 **공란으로 출력한다.** `humans.department`를 전 행에 반복해 찍는 건 안 된다 — 틀린 값을 증명서에 인쇄하게 된다.

### 1-3. `purpose` / `otherMatters`는 저장한다

명세상 발급 요청 body에만 있고 저장 위치가 없다. 그런데 이 둘은 서식에 인쇄되는 값이다. 저장하지 않으면 **같은 증명서를 다시 뽑을 수 없고**, 발급 이력에서 "무슨 용도로 발급했는지"를 못 본다. `certificates_issued`에 컬럼으로 넣는다 (§2).

---

## 2. 스키마

마이그레이션 도구가 없고 `ddl-auto`가 `validate`이므로 DDL은 수동 적용한다.

### 2-1. 기존 `certificate` 테이블 변경

```sql
-- (1) 사람과 연결. human-domain.md §6-2에서 막혀 있던 그 FK.
ALTER TABLE certificate ADD COLUMN human_id BIGINT REFERENCES humans(human_id);

-- (2) 기존 행 백필: (성명, 생년월일)로 매칭. humans의 uk_humans_name_birth_date가 1:1을 보장한다.
UPDATE certificate c SET human_id = h.human_id
  FROM humans h
 WHERE h.name = c.name AND h.birth_date = c.birth_date;

CREATE INDEX idx_certificate_human_id ON certificate(human_id);

-- (3) 날짜 3종을 String에서 DATE로. §2-2 참고. **적용 완료된 엔티티에 맞춘 DDL이다.**
ALTER TABLE certificate ALTER COLUMN hire_date       TYPE DATE USING NULLIF(hire_date, '')::DATE;
ALTER TABLE certificate ALTER COLUMN retirement_date TYPE DATE USING NULLIF(retirement_date, '')::DATE;
ALTER TABLE certificate ALTER COLUMN expiration_date TYPE DATE USING NULLIF(expiration_date, '')::DATE;

-- (4) 근무부서. §1-2. 채우는 경로는 아직 없다.
ALTER TABLE certificate ADD COLUMN department VARCHAR(255);
```

(3)은 기존 행의 문자열이 `uuuu-MM-dd` 등 Postgres가 읽는 형식일 때만 통과한다. `24.1.1` 같은 값이 남아 있으면 캐스팅이 통째로 실패하므로 **먼저 확인한다.**

```sql
SELECT certificate_id, hire_date, expiration_date, retirement_date FROM certificate
 WHERE hire_date       !~ '^\d{4}-\d{2}-\d{2}$' AND NULLIF(hire_date, '')       IS NOT NULL
    OR expiration_date !~ '^\d{4}-\d{2}-\d{2}$' AND NULLIF(expiration_date, '') IS NOT NULL
    OR retirement_date !~ '^\d{4}-\d{2}-\d{2}$' AND NULLIF(retirement_date, '') IS NOT NULL;
```

`human_id`는 **nullable로 둔다.** 엑셀 업로드(`MappingConfirmService`)는 `humans`를 거치지 않고 `certificate`에 바로 넣기 때문에, NOT NULL로 잠그면 기존 업로드 경로가 전부 깨진다. (2)의 백필로 매칭 안 되는 행 = 인적사항이 아직 등록 안 된 사람이고, 그 행은 발급 대상에서 빠진다.

```sql
-- 백필 후 매칭 실패 행 확인. 0이 아니면 human 등록부터.
SELECT count(*) FROM certificate WHERE human_id IS NULL;
```

### 2-2. `hire_date` / `expiration_date` / `retirement_date` — `LocalDate`로 전환됨 ✅

**적용 완료.** 아래는 왜 했는지의 기록이다. DDL(§2-1 (3))은 아직 수동 적용 필요.

전환 전 `CertificateEntity`에서 이 셋은 `String`이었다. 엑셀 셀 원문을 그대로 담으려던 것으로 보이는데, 실제로는 이미 검증된 값이다 — `RowValidator`가 `CellValueConverter.parseDate()`로 파싱해 놓고 결과를 버리고 원문 문자열을 넣는다.

`String`으로 두면 이 도메인에서 세 군데가 깨진다.

1. **총 근무기간 계산**(§3)이 불가능하다. 매번 재파싱해야 하고, 파싱 실패하면 발급 시점에 500이 난다.
2. `까지 = retirement_date ?? expiration_date` 판정이 `"2024-01-01"`과 `"24.1.1"`을 다른 값으로 본다.
3. 근무기간 정렬이 문자열 정렬이 된다. `"2020.01.01"`과 `"2020-01-01"`이 섞여 있으면 순서가 뒤집힌다.

`CellValueConverter`가 이미 5종 + 2자리연도 패턴을 STRICT로 파싱하므로 (1) 엔티티 필드를 `LocalDate`로 바꾸고 (2) `RowValidator`가 파싱 결과를 그대로 넣게 했다.

`RowValidator`에서 `birthDate`에만 있던 "파싱 실패 시 행 실패" 처리가 이제 날짜 4종 전부에 걸린다 (`DATE_FIELDS` 루프). **엑셀 업로드 동작이 바뀐다** — 전에는 인식 못 하는 채용일이 원문 그대로 저장됐고, 이제는 그 행이 `failedRows`로 빠진다. DATE 컬럼에 `"24년 초"` 같은 값을 넣을 방법이 없으므로 불가피하다.

> 이건 §6 도메인 경계의 예외다. `human` 문서에서 `GlobalExceptionHandler` 하나를 예외로 뒀던 것과 같은 이유 — 우회하면 파싱 로직이 도메인 수만큼 복제되고, 그 사이에 날짜 해석이 갈라진다.

### 2-3. 신규 테이블

```sql
-- 발급된 증명서 1건 = 1행. certificate(재직사항)와 다른 것이다.
CREATE TABLE certificates_issued (
    certificate_issued_id BIGSERIAL PRIMARY KEY,
    human_id      BIGINT       NOT NULL REFERENCES humans(human_id),
    document_no   VARCHAR(32)  NOT NULL UNIQUE,
    purpose       VARCHAR(255),
    other_matters TEXT,
    total_months  INT          NOT NULL,
    total_days    INT          NOT NULL,
    issued_at     TIMESTAMP    NOT NULL,
    file_path     VARCHAR(512)            -- S3 object key. PDF 생성 후 채움
);

-- 발급 1건이 포함한 재직사항 행들. 서식 재직사항 표의 각 행.
-- line_no는 0부터. @OrderColumn이 채우므로 PK도 (발급건, line_no)다.
CREATE TABLE certificate_issued_items (
    certificate_issued_id BIGINT NOT NULL REFERENCES certificates_issued(certificate_issued_id),
    certificate_id        BIGINT NOT NULL REFERENCES certificate(certificate_id),
    line_no               INT    NOT NULL CHECK (line_no >= 0),
    PRIMARY KEY (certificate_issued_id, line_no)
);

-- 문서번호 채번. 연도별 1행.
CREATE TABLE document_number_seq (
    year     INT PRIMARY KEY,
    last_no  BIGINT NOT NULL
);
```

전용 엔티티(`CertificateIssuedItemEntity`) 대신 `CertificateIssuedEntity`의 `@ElementCollection` + `@OrderColumn`으로 잡았다. 이 목록은 발급 건 밖에서 조회될 일이 없어서 엔티티 + 리포지토리 한 벌이 통째로 필요 없다.

`certificates_issued`를 따로 두는 이유: `certificate`는 **재직 이력 한 줄**이고 발급 단위가 아니다. 한 번 발급에 재직 이력 여러 줄이 들어가고(서식 10행), 같은 이력으로 여러 번 발급될 수 있다. 명세의 `certificateId`는 문맥상 이 발급 건의 id다.

> **명세 용어 충돌:** 명세의 `certificateId`(발급 건)와 코드의 `certificate` 테이블(재직 이력)이 같은 단어를 다르게 쓴다. 표에서 `certificate` = 재직 이력, `certificates_issued` = 발급 건으로 읽는다. 테이블 이름을 `work_experience`로 바꾸는 게 맞지만 `file` 도메인 전체가 딸려 오므로 안 한다.

---

## 3. 발급 처리 로직

명세의 「처리 로직」 3단계를 그대로 따른다.

### 3-1. 문서번호 채번

```sql
INSERT INTO document_number_seq (year, last_no) VALUES (:year, 1)
ON CONFLICT (year) DO UPDATE SET last_no = document_number_seq.last_no + 1
RETURNING last_no;
```

한 문장이라 원자적이다. 포맷은 `"유성구-%d-%06d".formatted(year, lastNo)`.

Postgres 시퀀스를 안 쓴 이유: 연도별로 `000001`부터 다시 시작해야 하는데 시퀀스는 연초에 리셋을 누가 해줘야 한다. 이 방식은 연도 행이 없으면 알아서 1부터 시작한다.

> 명세의 `409: 문서번호 채번 충돌(재시도)`는 **필요 없다.** 위 문장이 행 잠금으로 직렬화하므로 충돌 자체가 안 난다. 재시도 루프도, 409도 구현하지 않는다.

```java
// ponytail: 연도 행 하나에 걸리는 행 잠금이라 동시 발급이 직렬화된다.
// 구청 창구 발급량에선 문제없음. 초당 수백 건이 되면 연도+구간 분할로 올린다.
```

### 3-2. 총 근무기간 산정

서식 칸이 `총  개월  일` — 년 단위가 없다. 개월 + 일로만 낸다.

```java
Period.between(from, to.plusDays(1));  // 재직일수는 양끝 포함
// → months = years * 12 + months, days = days
```

- 근무기간 여러 건은 각 구간을 일수로 환산해 합산한 뒤 개월/일로 되돌린다. `Period`끼리 더하면 정규화가 안 돼서 `1개월 45일` 같은 값이 나온다.
- `까지`가 비어 있으면(재직 중) `retirement_date ?? expiration_date`, 둘 다 없으면 **그 행은 총계에서 제외**하고 서식엔 공란. 발급일까지로 임의 연장하지 않는다.
- 구간이 겹치면 겹친 만큼 중복 계산된다. 전보 이력이 같은 날짜로 두 줄 들어간 경우가 실제로 있을 수 있다. 일단 단순 합산하고, 원본 데이터 확인 후 병합 여부를 정한다 (§7-1).

> **1개월 = 30일로 고정했다.** 일수 합계에는 기준일이 없어 달력상 개월로 되돌릴 방법이 없다. 경력 합산에 흔히 쓰는 관례지만 **유성구청 산정 기준 확인이 필요하다** (§7-1 8번). `WorkPeriodCalculator.DAYS_PER_MONTH` 한 곳만 고치면 된다.

### 3-3. 직인 삽입 + PDF

§4.

---

## 4. PDF 생성

### 4-1. hwpx는 안 쓴다

`.hwpx`를 서버에서 채워 넣을 방법이 없다. Java에 HWPX 편집 라이브러리가 없고(`hwplib`은 구형 `.hwp` 전용), 한글 프로그램을 서버에 띄우는 건 논외다.

**서식 파일은 레이아웃 명세로만 쓰고**, 같은 레이아웃의 XHTML을 만들어 PDF로 렌더한다. 서식은 표 3개 + 텍스트뿐이라 재현이 어렵지 않다.

```
implementation 'io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.28'
```

**새로 추가하는 의존성은 이거 하나.** 한글 폰트(나눔고딕 등) `.ttf`를 `src/main/resources/fonts/`에 넣고 `@font-face`로 임베드해야 한다 — 안 하면 전부 두부(□)로 나온다.

| 대안 | 왜 안 씀 |
|---|---|
| Thymeleaf 추가 | 템플릿 1개에 의존성 하나. 10행 루프는 `StringBuilder`로 충분하다 |
| POI로 .docx 생성 | POI는 이미 있지만 출력이 Word다. 명세는 `application/pdf` |
| 브라우저 인쇄용 HTML만 반환 | 명세가 PDF binary |

> openhtmltopdf는 XHTML만 받는다. 태그를 닫지 않으면 파싱 에러가 난다.

### 4-2. 저장

생성 PDF는 `S3Uploader`로 올리고 `certificates_issued.file_path`에 key를 넣는다. 다운로드(§5.5)는 그 key로 받아서 스트리밍한다.

재발급 시 매번 새로 렌더하지 않는다 — 발급된 증명서는 **불변**이어야 한다. 원본 데이터가 나중에 수정돼도 이미 발급된 PDF는 그대로여야 하므로, `file_path`가 있으면 그걸 내려준다.

---

## 5. API

`/api/certificates` 하위. 컨트롤러 1개.

### 5-0. 명세와 다르게 가는 부분

| # | 명세 | 여기 | 이유 |
|---|---|---|---|
| 1 | ~~`GET /api/certificates/{humanId}`~~ ✅ **명세 반영됨** | `GET /api/humans/{humanId}/certificates` | 상세 조회와 경로가 완전히 같았다. 둘 다 `/api/certificates/{x}`라 Spring이 시작 시 `Ambiguous mapping`으로 죽는다 — 이름만 다른 경로 변수는 구분이 안 된다 |
| 2 | ~~`workerId`, `workExperienceIds`~~ ✅ **명세 반영됨** | `humanId`, `certificateIds` | `worker`/`workExperience` 테이블이 없다. 스키마에 존재하는 이름을 쓴다. 배열이므로 복수형 `certificateIds` |
| 3 | ~~`PUT` 응답이 `{insertedCount, failedRows}`~~ ✅ **명세 반영됨** | `204` (본문 없음) | 파일 업로드 API 응답을 복사한 것으로 보인다. 단건 수정과 무관 |
| 4 | ~~상세 조회/수정 body가 `snake_case`~~ ✅ **명세 반영됨** | `camelCase` | 나머지 API가 전부 camelCase. Jackson 기본값 |

네 건 전부 명세에 반영 완료. 아래 §5.1~§5.6은 갱신된 명세와 일치한다.

### 5.1 POST `/api/certificates` — 발급 (민원 담당자)

Request
```json
{
  "humanId": 1,
  "certificateIds": [1, 2],
  "purpose": "은행 제출용",
  "otherMatters": ""
}
```

| 필드 | 검증 |
|---|---|
| `humanId` | `@NotNull` |
| `certificateIds` | `@NotEmpty`, `@Size(max = 10)` (§1-1) |
| `purpose` | `@NotBlank`, `@Size(max = 255)` — 서식에 인쇄되는 칸이라 필수 |
| `otherMatters` | nullable, `@Size(max = 1000)` |

Response `201`
```json
{
  "certificateId": 1,
  "documentNo": "유성구-2026-000001",
  "downloadUrl": "/api/certificates/1/download"
}
```

| 상태 | 조건 |
|---|---|
| 400 | 검증 실패, `certificateIds` 11개 이상 |
| 401 | 인증 미구현 (§7-1) |
| 404 | `humanId` 없음 → `HUMAN_NOT_FOUND` / `certificateIds` 중 없는 값 → `CERTIFICATE_NOT_FOUND` |
| 409 | ~~문서번호 채번 충돌~~ → §3-1. 발생하지 않으므로 구현 안 함 |

**`certificateIds`가 전부 `humanId`의 것인지 반드시 검증한다.** 안 하면 남의 재직 이력이 내 증명서에 찍힌다. `findAllByCertificateIdInAndHumanId()`로 조회해 개수가 다르면 404.

### 5.2 POST `/api/certificates/self` — 발급 (민원인 본인)

**구현하지 않는다.** 본인이 누구인지 아는 수단이 없다 (§7-1). auth 도메인 완료 후 §5.1에 `humanId = 인증주체.humanId`, `certificateIds = 전체`를 넣는 얇은 래퍼로 붙인다. 명세의 403(본인 경력 외 접근)도 그때 같이.

### 5.3 GET `/api/certificates/{certificateId}` — 상세 조회

발급 건 1개의 상세. Response `200`

```json
{
  "certificateId": 1,
  "documentNo": "유성구-2026-000001",
  "issuedAt": "2026-08-27T14:30:00",
  "purpose": "은행 제출용",
  "otherMatters": "",
  "human": { "humanId": 1, "name": "홍길동", "birthDate": "1990-01-01", "gender": "MALE", "address": "..." },
  "totalMonths": 26, "totalDays": 14,
  "items": [
    { "certificateId": 1, "hireDate": "2020-01-01", "retirementDate": "2022-03-14",
      "keyResponsibilities": "...", "jobTitle": "...", "division": "채용",
      "employmentType": "기간제", "reason": "...", "note": "..." }
  ]
}
```

명세의 응답은 `certificate` 한 행의 컬럼 나열인데, 발급 건은 재직 이력 여러 줄이라 그 형태로는 표현이 안 된다. `human` + `items`로 감싼다. 필드 자체는 명세에 있는 것 전부 포함한다.

400 (`certificateId` 숫자 아님) / 401 / 404 `CERTIFICATE_ISSUED_NOT_FOUND` / 500

### 5.4 GET `/api/humans/{humanId}/certificates` — 경력 증명 사항 찾기

발급 화면에서 "이 사람이 고를 수 있는 재직 이력"을 뿌리는 용도. 발급 건이 아니라 `certificate`(재직 이력) 배열이다.

Response `200` — 없으면 `[]`
```json
[
  { "certificateId": 1, "division": "채용", "employmentType": "기간제",
    "keyResponsibilities": "string", "hireDate": "2020-01-01",
    "retirementDate": "2022-03-14", "expirationDate": "2022-03-14",
    "reason": "string", "note": "string" }
]
```

| 상태 | 조건 |
|---|---|
| 400 | `humanId` 숫자 아님 |
| 401 | 인증 미구현 |
| 404 | `humanId`의 인적사항 없음 → `HUMAN_NOT_FOUND` |
| 500 | — |

명세 그대로: **인적사항은 있는데 재직 이력이 없으면 404가 아니라 `200 []`이다.** `humanRepository.existsById()` 먼저 확인하고 목록을 조회한다.

정렬은 `hire_date ASC, certificate_id ASC`. 서식의 재직사항 표가 시간순이므로 여기서도 같은 순서로 준다. `hire_date`가 NULL인 행은 뒤로 (`NULLS LAST`).

### 5.5 GET `/api/certificates/{certificateId}/download` — 다운로드/출력

Response `200` `application/pdf` (binary), `Content-Disposition: attachment; filename="유성구-2026-000001.pdf"`

`certificates_issued.file_path`의 S3 객체를 그대로 내려준다 (§4-2).

400 / 401 / 403 (본인·담당자 외 — **auth 이후**) / 404 `CERTIFICATE_ISSUED_NOT_FOUND` / 500

파일명에 한글이 들어가므로 `Content-Disposition`은 RFC 5987 `filename*=UTF-8''...` 형식을 같이 넣는다. 안 하면 브라우저가 깨진 이름으로 저장한다.

### 5.6 PUT `/api/certificates/{certificateId}` — 수정

명세는 "경력증명서 수정"인데 body 필드가 전부 `certificate`(재직 이력) 컬럼이다. **발급 건 수정이 아니라 재직 이력 수정**으로 읽는다. 이미 발급된 PDF는 §4-2대로 불변이므로 수정해도 안 바뀐다.

Request — `certificateId`는 경로 변수
```json
{
  "name": "홍길동", "birthDate": "1990-01-01", "gender": "MALE",
  "jobTitle": "string", "keyResponsibilities": "string",
  "hireDate": "2020-01-01", "expirationDate": "2022-03-14", "retirementDate": "2022-03-14",
  "division": "채용", "reason": "string", "employmentType": "기간제", "note": "string"
}
```

| 필드 | 검증 |
|---|---|
| `name` | `@NotBlank`, `@Size(max = 255)` |
| `gender` | `@NotNull` |
| `division` | nullable, `채용`/`전보`/`해지`/`퇴직` |
| `employmentType` | nullable, `기간제`/`단시간근로자` |
| 날짜 3종 | nullable, `LocalDate` (§2-2) |
| 나머지 | nullable |

`division`/`employmentType` 허용값은 `RowValidator`의 `VALID_DIVISIONS`/`VALID_EMPLOYMENT_TYPES`와 **같은 상수를 봐야 한다.** 두 벌로 두면 엑셀로는 들어가는데 API로는 막히는(혹은 그 반대) 상태가 된다. 상수를 `certificate` 패키지로 올리고 `RowValidator`가 그걸 참조하게 한다 — §2-2와 같은 커밋.

Response `204` / 400 / 401 / 404 `CERTIFICATE_NOT_FOUND` / 500

`hireDate > retirementDate`면 400. `@AssertTrue`로 처리하고 전용 에러 코드는 두지 않는다 (human §4.4와 동일).

---

## 6. 패키지 구조 / 에러 코드

```
commonly/commonlybe/certificate/
├── controller/
│   ├── CertificateController.java           // POST/GET/PUT/download (§5.1, 5.3, 5.5, 5.6)
│   ├── HumanCertificateController.java      // GET /api/humans/{humanId}/certificates (§5.4)
│   └── dto/
│       ├── CertificateIssueRequest.java
│       ├── CertificateIssueResponse.java
│       ├── CertificateDetailResponse.java
│       ├── CertificateHumanDto.java
│       ├── CertificateItemDto.java          // §5.3 items와 §5.4가 같은 모양을 쓴다
│       └── CertificateUpdateRequest.java
├── service/
│   ├── CertificateIssueService.java         // 발급 (§3)
│   └── CertificateService.java              // 조회 / 수정 / 다운로드
├── document/
│   ├── DocumentNumberGenerator.java         // §3-1
│   ├── WorkPeriod.java                      // (개월, 일)
│   └── WorkPeriodCalculator.java            // §3-2
├── repository/
│   ├── CertificateRepository.java           // 기존 + 조회 3개
│   └── CertificateIssuedRepository.java
├── entity/
│   ├── CertificateEntity.java               // 기존 + humanId / department / update()
│   ├── CertificateIssuedEntity.java
│   ├── CertificateCodes.java                // 구분/근무형태 허용값. RowValidator와 공유
│   └── Gender.java                          // 기존
└── exception/
    ├── CertificateErrorCode.java
    └── CertificateException.java
```

```java
@AllArgsConstructor
@Getter
public enum CertificateErrorCode implements ErrorProperty {
    CERTIFICATE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 경력사항을 찾을 수 없습니다."),
    CERTIFICATE_ISSUED_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 경력증명서를 찾을 수 없습니다."),
    CERTIFICATE_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "발급된 증명서 파일이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
```

`HUMAN_NOT_FOUND`는 `HumanErrorCode`를 그대로 import한다. 상태 코드 매핑은 `GlobalExceptionHandler`의 `CommonlyException` 핸들러가 이미 한다 (human §2).

### 도메인 경계

`human` 문서와 같은 규칙 — `certificate/` 밖은 안 건드린다. 다만 이 도메인은 **예외가 두 개** 있고 둘 다 별도 커밋으로 분리한다.

| 커밋 | 건드리는 곳 | 이유 |
|---|---|---|
| ~~`refactor: certificate 날짜 컬럼 LocalDate 전환`~~ ✅ | `file/excel/RowValidator` | §2-2. 완료 |
| ~~`refactor: 구분/근무형태 허용값 상수 통합`~~ ✅ | `file/excel/RowValidator` | §5.6. 완료. 상수는 `certificate/entity/CertificateCodes`로 |
| `build: openhtmltopdf 추가` | `build.gradle` | §4-1. **아직 안 함** — §7-1 2·9번이 풀려야 의미가 있다 |

---

## 7. 선행/차단 사항

### 7-1. 막힌 것

| # | 내용 | 영향 |
|---|---|---|
| 1 | **인증 미구현** — `SecurityConfig`가 `anyRequest().permitAll()` | 401/403 전부 구현 불가. `POST /self`(§5.2) 전체 보류. 서식의 `담당자`/`연락처` 칸도 채울 수 없다 |
| 2 | **유성구청장 직인 이미지 없음** | 발급물이 무효다. PNG(투명 배경) 확보 필요. 코드로 못 푼다 |
| 3 | **근무부서 데이터 원천 부재** (§1-2) | `certificate.department` 컬럼은 추가됨. 채울 데이터가 없어 서식 칸 공란 출력. 유성구청 담당자에게 원천 확인 요청 |
| 4 | **성명(영문) 없음** | 공란 출력. 필요하면 `humans`에 `name_en` 추가 |
| 5 | **발급 이력 연결** — 명세 처리로직 1번 | `구분 = 발급 이력`은 담당자가 다르다(`/api/issuance-histories`). certificate 도메인은 `certificates_issued`까지만 쓰고, 이력 적재 인터페이스는 그쪽과 합의 후 붙인다 |
| 6 | 근무기간 구간 중복 (§3-2) | 원본 데이터 확인 필요. 확인 전까지 단순 합산 |
| 7 | 마이그레이션 도구 없음 | §2 DDL 수동 적용. **`certificate` 백필(§2-1)은 되돌리기 어려우니 배포 전 스테이징에서 먼저** |
| 8 | **총 근무기간 산정 기준 미확인** (§3-2) | 1개월 = 30일로 구현. 유성구청 기준이 다르면 `WorkPeriodCalculator.DAYS_PER_MONTH` 한 줄 |
| 9 | **한글 폰트 파일 없음** | PDF 렌더링 시 전부 두부(□). 나눔고딕 등 `.ttf` 필요 (§4-1) |

1~4, 8~9는 코드로 해결 불가능하고 외부 확인/파일 확보가 필요하다. 나머지 API는 전부 구현했다 (§8).

### 7-2. 테스트 범위

`build.gradle`에 슬라이스 테스트 의존성(`spring-boot-starter-data-jpa-test`, `-webmvc-test`)은 있는데 **H2도 `src/test/resources/application.yaml`도 없다.** 그래서 컨텍스트가 뜨는 테스트는 여전히 하나도 못 돌린다 (human §6-1과 같은 상황).

| 테스트 | 상태 |
|---|---|
| `WorkPeriodCalculatorTest` (순수 JUnit) | **있음** — 양끝 포함, 여러 구간 합산, 만료예정일 대체, 끝나는 날 없음, 채용일 없음, 역전 구간, 윤년, 빈 목록 (8건) |
| `CertificateIssueServiceTest` (Mockito) | **있음** — 인적사항 404, 남의 이력 404, 중복 id 정규화, 총 근무기간 저장 (4건) |
| `DocumentNumberGeneratorTest` (`@DataJpaTest`) | **없음** — §7-3에서 수동 검증으로 대신했다 |
| `CertificateControllerTest` (`@WebMvcTest`) | **없음** — 같음 |

### 7-3. 실제 Postgres 수동 검증 (2026-08-27)

자동 테스트로 못 덮는 부분(네이티브 채번 SQL, JPA 매핑, 라우팅, 직렬화)은 임시 Postgres 16 컨테이너에 `ddl-auto: create`로 앱을 띄워 확인했다. **재현하려면 아래를 다시 돌리면 된다.**

| 확인한 것 | 결과 |
|---|---|
| 앱 기동 (경로 충돌 §5-0 1번) | `Ambiguous mapping` 없이 정상 기동 |
| 채번 `ON CONFLICT ... RETURNING`이 Hibernate `createNativeQuery`로 도는지 | **돈다.** `유성구-2026-000001` → `000002`, `document_number_seq(2026, 2)` |
| `@ElementCollection` + `@OrderColumn` | `certificate_issued_items` 생성, `line_no` 0부터 순서대로 |
| 총 근무기간 | 366일 + 306일 = 672일 → `22개월 12일` (§3-2 30일 기준) |
| §5.4 목록 / 404 | 본인 이력 2건만, 없는 `humanId`는 404 |
| §5.1 발급 / 남의 이력 404 / 11개 400 / `purpose` 누락 400 | 전부 명세대로 |
| §5.3 상세 | `human` + `items` 중첩, `totalMonths`/`totalDays` 포함 |
| §5.5 다운로드 | `file_path`가 없어 `404 CERTIFICATE_FILE_NOT_FOUND` (PDF 미구현) |
| §5.6 수정 204 / 잘못된 구분 400 / 채용일>퇴직일 400 / 없는 id 404 | 전부 명세대로 |

> 이건 자동 회귀 테스트가 **아니다.** 실행되는 검증으로 바꾸려면 human §6-1대로 H2 + `src/test/resources/application.yaml`이 필요하고, 채번 SQL은 Postgres 문법이라 H2로는 반쪽이다. Testcontainers를 넣는 게 정답이지만 의존성이 하나 늘어난다.

## 8. 작업 순서

1. **DDL 적용** (§2) — 스테이징 먼저. 날짜 캐스팅 실패 행과 `human_id IS NULL` 건수 확인. **아직 안 됨**
2. ~~날짜 `LocalDate` 전환 (§2-2) + `certificate.department` 추가 (§1-2)~~ ✅
3. ~~`CertificateIssuedEntity` / 리포지토리~~ ✅ (`CertificateIssuedItemEntity`는 `@ElementCollection`으로 대체)
4. ~~`WorkPeriodCalculator` + 테스트 (§3-2)~~ ✅ 8건
5. ~~`DocumentNumberGenerator` (§3-1)~~ ✅ 실제 Postgres에서 채번 확인 (§7-3)
6. ~~GET `/api/humans/{humanId}/certificates` (§5.4)~~ ✅
7. ~~PUT `/api/certificates/{certificateId}` (§5.6) + 허용값 상수 통합~~ ✅
8. `CertificatePdfRenderer` (§4) — **안 함.** 직인 이미지(§7-1 2번)와 한글 폰트(§7-1 9번)가 없으면 만들어도 무효한 문서가 나온다
9. ~~POST `/api/certificates` (§5.1) → GET 상세(§5.3) → GET 다운로드(§5.5)~~ ✅

### 지금 상태

발급 → 조회 → 수정까지 전부 동작한다. **PDF만 없다.** `certificates_issued.file_path`가 계속 null이라 §5.5는 `404 CERTIFICATE_FILE_NOT_FOUND`를 낸다. 직인과 폰트가 확보되면 `document/CertificatePdfRenderer`를 추가하고 발급 트랜잭션 끝에서 S3에 올린 뒤 `file_path`를 채우면 된다 — 다른 코드는 안 건드려도 된다.

auth 완료 후: `POST /api/certificates/self` (§5.2), 401/403 전반, 서식 담당자/연락처 칸.
