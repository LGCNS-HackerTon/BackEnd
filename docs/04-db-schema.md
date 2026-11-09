# DB 스키마 (초안)

> 상태: **초안, MySQL 기준**. DB 스택이 바뀌면 타입만 바꾸고 테이블·컬럼 이름은 유지한다 (BE 코드와 맞물리니까).
> 담당: DB. 컬럼을 바꾸면 BE에게 먼저 알리기.

## 테이블 4개 한눈에 보기

| 테이블 | 역할 | 데이터는 누가 넣나 |
|---|---|---|
| `registry_questions` | 등기부 질문 10개 + 점수 + 행동 안내 템플릿 | seed (docs/01에서 옮김) |
| `checklist_items` | 일정표 할 일 14개 | seed (docs/02에서 옮김) |
| `diagnoses` | 진단 1번 = 1줄. 입력 금액, 점수, 등급, AI 설명 | API가 저장 |
| `diagnosis_answers` | 진단별 질문 답 10개 | API가 저장 |

```
registry_questions 1 ──< diagnosis_answers >── 1 diagnoses
checklist_items (혼자, 일정 계산용 원본)
```

- 회원(`users`), 매물(`properties`), 실거래가(`trade_records`), 위험 요소(`risk_factors`)는 **뺐다**. 위험 요소는 `diagnosis_answers.score`와 `diagnoses.debt_ratio`로 다시 계산할 수 있다
- `DEBT_RATIO`와 공통 안내(`commonAdvice`) 문구는 질문이 아니라서 DB 대신 **BE 코드 상수**로 둔다 (원본은 docs/01 4절)

## DDL

```sql
CREATE TABLE registry_questions (
    code            VARCHAR(30)  PRIMARY KEY,          -- T1_ADDRESS 등 (docs/01)
    section         VARCHAR(10)  NOT NULL,             -- TITLE, GAPGU, EULGU, BUILDING
    sort_order      INT          NOT NULL,
    text            VARCHAR(255) NOT NULL,             -- 질문
    help            TEXT         NOT NULL,             -- 쉬운 설명
    risky_answer    VARCHAR(3)   NOT NULL,             -- YES 또는 NO (UNKNOWN은 항상 위험 쪽)
    risk_score      INT          NOT NULL,             -- 위험한 답일 때 점수. B1_MORTGAGE는 10 (UNKNOWN일 때만 적용)
    needs_amount    BOOLEAN      NOT NULL DEFAULT FALSE,
    factor_title    VARCHAR(100) NOT NULL,             -- 결과 화면 제목 (예: 주택이 아닌 용도)
    ask_agent       VARCHAR(255) NOT NULL,             -- 중개사에게 물어볼 질문
    special_clause  TEXT         NULL                  -- 특약 문구, 특약으로 해결 어려우면 NULL
);

CREATE TABLE checklist_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sort_order      INT          NOT NULL,             -- docs/02 순서
    anchor          VARCHAR(10)  NOT NULL,             -- CONTRACT, BALANCE, MOVE_IN, EXPIRY
    offset_days     INT          NOT NULL,             -- 기준일로부터 며칠 (음수 = 전)
    title           VARCHAR(100) NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    important       BOOLEAN      NOT NULL DEFAULT FALSE,
    link_name       VARCHAR(50)  NULL,
    link_url        VARCHAR(255) NULL
);

CREATE TABLE diagnoses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    deposit         INT          NOT NULL,             -- 만원
    sale_price      INT          NOT NULL,             -- 만원
    mortgage_max    INT          NOT NULL DEFAULT 0,   -- 만원
    senior_deposit  INT          NOT NULL DEFAULT 0,   -- 만원
    senior_unknown  BOOLEAN      NOT NULL DEFAULT FALSE, -- 선순위 보증금을 몰라서 0으로 계산했는지 (warnings용)
    debt_ratio      DECIMAL(6,1) NOT NULL,             -- %
    risk_score      INT          NOT NULL,
    risk_grade      VARCHAR(10)  NOT NULL,             -- SAFE, CAUTION, DANGER
    ai_explanation  TEXT         NOT NULL,             -- AI 실패 시 템플릿 문장
    ai_used         BOOLEAN      NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE diagnosis_answers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    diagnosis_id    BIGINT       NOT NULL,
    question_code   VARCHAR(30)  NOT NULL,
    answer          VARCHAR(10)  NOT NULL,             -- YES, NO, UNKNOWN
    score           INT          NOT NULL,             -- 이 답으로 받은 점수 (0 가능)
    FOREIGN KEY (diagnosis_id)  REFERENCES diagnoses(id) ON DELETE CASCADE,
    FOREIGN KEY (question_code) REFERENCES registry_questions(code),
    UNIQUE KEY uk_diag_question (diagnosis_id, question_code)
);
```

## 저장하지 않는 데이터

| 데이터 | 이유 |
| --- | --- |
| OCR 서류(등기부등본, 건축물대장 파일) | 소유자 이름 등 개인정보 (docs/08 NFR-06, D6). 메모리에서 처리 후 폐기 |
| OCR 요청 주소, 소유자 이름, OCR 근거 문장 | 같은 이유. OCR API 응답으로만 쓰고 끝 |
| 계정·마이페이지 기록 | 브라우저 저장 데모 (D3-2). 서버 테이블 없음 |

그래서 OCR이 들어와도 **테이블은 그대로 4개**다. 진단 결과는 지금처럼 `diagnoses`, `diagnosis_answers`에 금액과 답만 저장한다.

## DB 담당이 할 일

1. 위 DDL로 로컬 DB 만들기, 실행 방법을 `db/README.md`에 3줄로 적기
2. `db/seed.sql`: docs/01 질문 10개, docs/02 할 일 14개를 그대로 INSERT
3. QA와 함께 데모 케이스 A/B/C가 들어간 상태로 확인
4. 발표용 ERD 그림 1장 (위 관계도 기준)
5. OCR 데모용 **가짜 서류 샘플** (케이스 A/B/C, 실제 이름·주소 금지) → `db/samples/` (docs/09)
