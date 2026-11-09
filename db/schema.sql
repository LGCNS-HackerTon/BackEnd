-- =====================================================================
-- jeonse DB 초기화 스크립트 (원본: docs/04-db-schema.md, 테이블 5개)
-- MySQL 8 기준, MariaDB 10.5 이상에서도 실행됨
--
-- ⚠ 앱 테이블 5개를 지우고 다시 만든다 (회원·진단 데이터 삭제). 같은 DB의 다른 테이블(실거래가 등)은 그대로 둔다
-- 실행 순서: 이 파일 → db/seed.sql
-- =====================================================================

-- 정렬 규칙(COLLATE)은 서버 기본값을 쓴다 (MariaDB 호환)
CREATE DATABASE IF NOT EXISTS jeonse
    DEFAULT CHARACTER SET utf8mb4;

USE jeonse;

-- FK가 걸린 자식 테이블부터 지운다
DROP TABLE IF EXISTS diagnosis_answers;
DROP TABLE IF EXISTS diagnoses;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS checklist_items;
DROP TABLE IF EXISTS registry_questions;

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
