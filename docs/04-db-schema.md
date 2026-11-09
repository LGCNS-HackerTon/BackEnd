# DB 스키마 (초안)

> 상태: **초안, MySQL 기준**. DB 스택이 바뀌면 타입만 바꾸고 테이블·컬럼 이름은 유지한다 (BE 코드와 맞물리니까).
> 담당: DB. 컬럼을 바꾸면 BE에게 먼저 알리기.

## 테이블 5개 한눈에 보기

| 테이블 | 역할 | 데이터는 누가 넣나 |
|---|---|---|
| `users` | 선택 로그인 회원과 진단 기록 소유자 | 회원가입 API가 저장 |
| `registry_questions` | 등기부 질문 10개 + 점수 + 행동 안내 템플릿 | seed (docs/01에서 옮김) |
| `checklist_items` | 일정표 할 일 16개 | seed (docs/02에서 옮김) |
| `diagnoses` | 진단 1번 = 1줄. 입력 금액, 점수, 등급, AI 설명 | API가 저장 |
| `diagnosis_answers` | 진단별 질문 답 10개 | API가 저장 |

```
users 1 ──< diagnoses 1 ──< diagnosis_answers >── 1 registry_questions
checklist_items (혼자, 일정 계산용 원본)
```

- 매물 자동 조회용 `properties`, 실거래가 `trade_records`, 위험 요소 `risk_factors`는 뺐다. 로그인 회원의 진단은 `users`와 연결한다
- `DEBT_RATIO`와 공통 안내(`commonAdvice`) 문구는 질문이 아니라서 DB 대신 **BE 코드 상수**로 둔다 (원본은 docs/01 4절)

## DDL

초기화 SQL은 db/schema.sql에 있다. 질문·일정 순서 UNIQUE 2개와 CHECK 15개, 회원별 진단 조회 인덱스, updated_at 자동 갱신을 적용한다.

```sql
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,             -- 로그인 이메일 (FE에서 소문자로 정규화)
    password_hash   VARCHAR(255) NOT NULL,             -- BCrypt 등 단방향 해시만 저장
    name            VARCHAR(50)  NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_email (email)                  -- 같은 이메일로 중복 가입 방지
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE registry_questions (
    code            VARCHAR(30)  PRIMARY KEY,          -- T1_ADDRESS 등 (docs/01)
    section         VARCHAR(10)  NOT NULL,             -- TITLE, GAPGU, EULGU, BUILDING (화면의 '표제부' 등 표시명은 BE 상수로 변환)
    sort_order      INT          NOT NULL,
    text            VARCHAR(255) NOT NULL,             -- 질문
    help            TEXT         NOT NULL,             -- 쉬운 설명 (해요체)
    risky_answer    VARCHAR(3)   NOT NULL,             -- YES 또는 NO (UNKNOWN은 항상 위험 쪽)
    risk_score      INT          NOT NULL,             -- 위험한 답일 때 점수. B1_MORTGAGE는 10 (UNKNOWN일 때만 적용)
    needs_amount    BOOLEAN      NOT NULL DEFAULT FALSE,
    factor_title    VARCHAR(100) NOT NULL,             -- 결과 화면 제목
    ask_agent       VARCHAR(255) NOT NULL,             -- 중개사에게 물어볼 질문
    special_clause  TEXT         NULL,                 -- 특약 문구, 특약으로 해결 어려우면 NULL
    UNIQUE KEY uk_question_order (sort_order),
    CONSTRAINT ck_question_section CHECK (section IN ('TITLE', 'GAPGU', 'EULGU', 'BUILDING')),
    CONSTRAINT ck_question_risky   CHECK (risky_answer IN ('YES', 'NO')),
    CONSTRAINT ck_question_score   CHECK (risk_score >= 0),
    CONSTRAINT ck_question_order   CHECK (sort_order >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE checklist_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sort_order      INT          NOT NULL,             -- docs/02 순서
    anchor          VARCHAR(10)  NOT NULL,             -- CONTRACT, BALANCE, MOVE_IN, EXPIRY
    offset_days     INT          NOT NULL,             -- 기준일로부터 며칠 (음수 = 전)
    title           VARCHAR(100) NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    important       BOOLEAN      NOT NULL DEFAULT FALSE,
    link_name       VARCHAR(50)  NULL,
    link_url        VARCHAR(255) NULL,
    UNIQUE KEY uk_checklist_order (sort_order),
    CONSTRAINT ck_checklist_anchor CHECK (anchor IN ('CONTRACT', 'BALANCE', 'MOVE_IN', 'EXPIRY')),
    CONSTRAINT ck_checklist_order  CHECK (sort_order >= 1),
    -- 링크는 이름과 URL이 함께 있거나 함께 없어야 한다 (API 응답 link 객체가 반쪽이 되지 않게)
    CONSTRAINT ck_checklist_link   CHECK ((link_name IS NULL) = (link_url IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE diagnoses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NULL,                 -- 로그인 회원 (D10)
    property_name   VARCHAR(100) NULL,                 -- 마이페이지 매물 별칭 (D10)
    deposit         INT          NOT NULL,             -- 만원
    sale_price      INT          NOT NULL,             -- 만원
    mortgage_max    INT          NOT NULL DEFAULT 0,   -- 만원
    senior_deposit  INT          NOT NULL DEFAULT 0,   -- 만원
    senior_unknown  BOOLEAN      NOT NULL DEFAULT FALSE, -- 선순위 보증금 미입력(null)과 실제 0원 구분 (warnings용)
    debt_ratio      DECIMAL(6,1) NOT NULL,             -- %
    risk_score      INT          NOT NULL,
    risk_grade      VARCHAR(10)  NOT NULL,             -- SAFE, CAUTION, DANGER
    ai_explanation  TEXT         NOT NULL,             -- AI 실패 시 템플릿 문장
    ai_used         BOOLEAN      NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 재평가(덮어쓰기, D10) 시각. UPDATE에서 updated_at을 생략하고 같은 행의 다른 컬럼 값이 바뀌면 DB가 자동 갱신한다
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    -- 마이페이지 "내 진단 최근 5건, created_at 오름차순" 조회용 (D10). user_id FK 인덱스도 겸한다
    KEY idx_diag_user_created (user_id, created_at),
    -- 보증금·시세는 1 이상, 채권최고액·선순위 보증금·부채비율은 0 이상
    CONSTRAINT ck_diag_deposit    CHECK (deposit >= 1),
    CONSTRAINT ck_diag_sale_price CHECK (sale_price >= 1),
    CONSTRAINT ck_diag_mortgage   CHECK (mortgage_max >= 0),
    CONSTRAINT ck_diag_senior     CHECK (senior_deposit >= 0),
    CONSTRAINT ck_diag_debt_ratio CHECK (debt_ratio >= 0),
    -- 등급은 점수로 정해진다 (docs/01 3절: 0~29 SAFE, 30~59 CAUTION, 60 이상 DANGER)
    -- 등급 경계를 바꾸면 BE 상수와 이 제약을 함께 바꾼다 (하나만 바꾸면 저장이 실패한다)
    CONSTRAINT ck_diag_grade CHECK (
        (risk_score BETWEEN 0 AND 29 AND risk_grade = 'SAFE')
     OR (risk_score BETWEEN 30 AND 59 AND risk_grade = 'CAUTION')
     OR (risk_score >= 60 AND risk_grade = 'DANGER')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE diagnosis_answers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    diagnosis_id    BIGINT       NOT NULL,
    question_code   VARCHAR(30)  NOT NULL,
    answer          VARCHAR(10)  NOT NULL,             -- YES, NO, UNKNOWN
    score           INT          NOT NULL,             -- 이 답으로 받은 점수 (0 가능)
    FOREIGN KEY (diagnosis_id)  REFERENCES diagnoses(id) ON DELETE CASCADE,
    FOREIGN KEY (question_code) REFERENCES registry_questions(code),
    UNIQUE KEY uk_diag_question (diagnosis_id, question_code),  -- 진단별 같은 질문의 답은 1개 (재평가 시 덮어쓰기 기준)
    CONSTRAINT ck_answer_value CHECK (answer IN ('YES', 'NO', 'UNKNOWN')),
    CONSTRAINT ck_answer_score CHECK (score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### BE 참고 (제약과 매핑)

- **제약 위반**: CHECK·UNIQUE·FK 위반은 그대로 두면 500이 된다. 저장 전에 입력을 검증하고 400 `INVALID_INPUT`으로 돌려준다
- **등급 경계**: `ck_diag_grade`는 docs/01 3절 경계(29/59)를 그대로 쓴다. docs/01 점수·등급 경계를 바꾸면 **BE 상수와 `db/schema.sql`의 이 제약을 같은 PR에서 함께 바꾼다**. 하나만 바꾸면 경계 점수의 진단 저장이 실패한다
- **`created_at`·`updated_at` Hibernate 매핑**: `UPDATE`에서 `updated_at`을 생략하고 같은 행의 다른 컬럼 값이 실제로 바뀌면 DB가 수정 시각을 자동 갱신한다. 값이 그대로이거나 답변 테이블만 바뀌면 진단 행의 시각도 그대로다. Hibernate의 기본 정적 UPDATE는 갱신 가능한 컬럼에 기존 값을 포함할 수 있으므로 시각 관리 주체를 정한다. Hibernate에서 관리하려면 `@CreationTimestamp`·`@UpdateTimestamp`를 쓰고, DB에 맡기려면 두 필드에 `@Column(insertable = false, updatable = false)`를 사용한다. 현재 엔티티에서 DB가 생성한 값이 즉시 필요하면 다시 조회하거나 생성값 매핑을 사용한다.

## 저장하지 않는 데이터

| 데이터 | 이유 |
| --- | --- |
| OCR 서류(등기부등본, 건축물대장 파일) | 소유자 이름 등 개인정보 (docs/08 NFR-06, D6). 메모리에서 처리 후 폐기 |
| OCR 요청 주소, 소유자 이름, OCR 근거 문장 | 같은 이유. OCR API 응답으로만 쓰고 끝 |
| 비로그인 진단 | 결과만 응답하고 DB에는 저장하지 않음 |

OCR 원본 서류는 저장하지 않는다. 로그인 회원의 진단 원본 입력과 답변은 `users`에 연결해 마이페이지에서 수정·재평가할 수 있게 한다.

## DB 담당이 할 일

1. 위 DDL로 로컬 DB 만들기, 실행 방법을 `db/README.md`에 3줄로 적기
2. `db/seed.sql`: docs/01 질문 10개, docs/02 할 일 16개를 그대로 INSERT
3. QA와 함께 데모 케이스 A/B/C가 들어간 상태로 확인
4. 발표용 ERD 그림 1장 (위 관계도 기준)
5. OCR 데모용 **가짜 서류 샘플** (케이스 A/B/C, 실제 이름·주소 금지) → `db/samples/` (docs/09)
