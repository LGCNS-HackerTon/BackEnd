# 공감 팀 프로젝트 공통 컨텍스트 (모든 AI가 먼저 읽는 파일)

> **팀원에게**: AI(Claude, ChatGPT, Cursor 등)에게 일을 시킬 때 **이 파일(`docs/AGENTS.md`)을 먼저 첨부하거나 붙여넣고** 시작해.
> Claude Code는 저장소 맨 위 `CLAUDE.md`를 통해 자동으로 읽어. 다른 도구는 이 파일을 직접 첨부해.
> 모두 같은 자료를 보고 만들어야 코드가 서로 맞물려.

## 1. 프로젝트 한 줄 요약

**전세역전** (팀 공감 · 공공 + 금융). 사회초년생이 계약할 집 주소와 관련 서류를 제출하고 OCR로 확인되지 않은 질문에 답하면 → 위험 신호를 쉬운 말로 알려주고 → 중개사에게 물어볼 질문, 계약서 특약 문구, 계약 일정표(D-day 할 일)까지 안내한다.

- 타겟: 처음 전세 계약을 하는 20~30대 사회초년생
- 포지션: HUG 안심전세 앱, 인터넷등기소 같은 **기존 서비스로 이어주는 입문 가이드** (경쟁자가 아님)
- 해커톤: 2026-11-09 ~ 11-10, 발표 11/10 15시경. **오늘(11/9) 17:00까지 메인 기능 2개(등기부 진단, 계약 일정표) 완성**, 11/10 12:00 기능 동결

## 2. 기술 스택

| 영역 | 스택 |
|---|---|
| 백엔드 | **Java 17 + Spring Boot 3 + Spring Data JPA**, Gradle, 포트 `8080` |
| 프론트엔드 | **React + Vite (JavaScript)**, Tailwind CSS, 포트 `5173` |
| DB | **MySQL 8**, DB 이름 `jeonse` |
| AI | 내일 BE2가 연동 (오늘은 템플릿). API 키는 `.env`에만, 절대 커밋 금지 |

### 백엔드 패키지 구조
기능별로 나눠서 BE 3명이 같은 파일을 덜 건드리게 한다. 기본 패키지 `com.lgcns.jeonse`.

| 패키지 | 담당 | 내용 |
|---|---|---|
| `common` | BE3 | 에러 응답 형식, 예외 처리, CORS 설정 |
| `question` | BE3 | `GET /api/questions`, `registry_questions` 엔티티 |
| `schedule` | BE3 | `GET /api/schedule`, `checklist_items` 엔티티 |
| `diagnosis` | BE1 | `POST /api/diagnoses`, 점수 계산, `diagnoses`·`diagnosis_answers` 엔티티 |
| `diagnosis.advice` | BE2 | 결과 문구(factors, commonAdvice, 템플릿 설명) |
| `ocr` | BE2 | `POST /api/ocr/registry`, OCR 엔진 연동. **서류는 저장하지 않는다** |
| `auth`, `member` | 담당 결정 필요 | 선택 로그인·회원가입, 회원별 진단 기록 조회·수정 권한 |

각 패키지 안은 `Controller` / `Service` / `Repository` / 엔티티 / `dto`로 나눈다.

### 프론트 폴더 구조

| 폴더 | 담당 | 내용 |
|---|---|---|
| `src/api/` | FE | API 클라이언트, Mock 데이터 (docs/03 예시 JSON) |
| `src/components/common/` | FE | 버튼, 카드, 등급 색 |
| `src/pages/Home/` | FE | S1 첫 화면 `/` |
| `src/pages/Diagnosis/` | FE | S2 진단 `/diagnosis` |
| `src/pages/Result/` | FE | S3 결과 `/result` |
| `src/pages/Schedule/` | FE | S4 일정표 `/schedule` |
| `src/pages/Auth/` | 담당 결정 필요 | 선택 로그인·회원가입 `/login`, `/signup` |
| `src/pages/MyPage/` | 담당 결정 필요 | S5 진단 기록 조회·수정·재평가 `/mypage` |

### 환경 변수 (`.env.example`만 커밋)

| 저장소 | 변수 |
|---|---|
| BackEnd | `DB_URL=jdbc:mysql://localhost:3306/jeonse`, `DB_USERNAME`, `DB_PASSWORD`, `CLOVA_OCR_URL`, `CLOVA_OCR_SECRET`, JWT 비밀값 |
| FrontEnd | `VITE_API_BASE_URL=http://localhost:8080`, `VITE_USE_MOCK=true` (BE 연결 전엔 true) |

- CORS는 `http://localhost:5173`을 허용한다 (BE3)

## 3. 진실의 원천 (Source of Truth)

코드보다 **문서가 우선**이다. 문서와 코드가 다르면 문서가 맞다.

| 문서 | 내용 | 주로 보는 사람 |
|---|---|---|
| [docs/01-registry-questions.md](01-registry-questions.md) | 등기부 질문 10개, 점수 규칙, 등급, 행동 안내 템플릿, 데모 케이스 | 전원 |
| [docs/02-schedule-rules.md](02-schedule-rules.md) | 계약 일정표 D-day 규칙 14개 | BE, FE, DB |
| [docs/03-api-contract.md](03-api-contract.md) | **API 명세서**: 공통 규칙, API 목록, 요청·응답 필드, 에러 (FE와 BE의 약속) | BE, FE, QA |
| [docs/04-db-schema.md](04-db-schema.md) | 테이블 5개, DDL 초안 | DB, BE |
| [docs/06-service-flow.md](06-service-flow.md) | **화면 흐름, 화면 경로**, 화면별 내용, 시스템·에러 흐름 | FE, BE, QA |
| [docs/05-roles.md](05-roles.md) | 역할 분담, 오늘 시간표, **역할별 체크리스트** | 전원 |
| [docs/08-requirements.md](08-requirements.md) | **요구사항 명세서**: 기능·비기능 요구사항, 우선순위(P0/P1/P2), 담당 | 전원 |
| [docs/07-git-rules.md](07-git-rules.md) | **GitHub 협업 규칙**: 이슈, 브랜치, 커밋, PR, 충돌 해결 | 전원 |
| [docs/09-feedback-ocr.md](09-feedback-ocr.md) | **강사님 피드백 반영 (11/9)**: OCR 적용, 블랙리스트 제외, 역할별 할 일 | 전원 |
| [docs/decision-log.md](decision-log.md) | 결정 기록 (처음 생각 → 확인한 내용 → 바뀐 판단) | 전원 |

## 4. AI에게 일 시킬 때 지켜야 할 규칙

1. **문서에 없는 기능을 만들지 않는다.** 서류 OCR과 미확인 항목 보완은 진단 흐름에 포함한다 (docs/06 S2, docs/03 5번, docs/09). OCR은 답을 채워주는 도우미일 뿐이고 **판독 못 한 것은 `NO`로 처리하지 않고 사용자에게 묻는다.** 실거래가 API 자동 조회, 블랙리스트(악성 임대인) 조회는 **이번 범위 밖**. 로그인은 **선택 기능**: 비로그인도 진단·일정표를 전부 쓰고, 로그인하면 진단 기록을 마이페이지에 저장한다 (docs/06 S5)
2. **위험 점수는 규칙 기반으로만 계산한다.** AI에게 점수나 등급을 판단시키지 않는다. AI는 설명·질문·특약 문구를 **다듬는 역할만**
3. **AI 호출이 실패해도 서비스는 동작해야 한다.** 실패하면 docs/01의 템플릿 문장을 그대로 쓴다 (`aiUsed: false`)
4. **API 모양(필드 이름, 타입, 단위)을 마음대로 바꾸지 않는다.** 바꿔야 하면 팀에 먼저 말하고 docs/03을 먼저 고친 뒤 코드를 고친다
5. **자기 담당 폴더만 수정한다** (docs/05 참고). 다른 폴더를 고쳐야 하면 담당자에게 말한다
6. **결과 화면에 면책 문구 필수**: "참고용이며 법적 판단이 아닙니다. 계약 전 공인중개사·전문가에게 확인하세요."
7. **사용자에게 보이는 문구는 쉬운 한국어**로. 법률 용어가 나오면 바로 옆에 풀이를 붙인다
8. **비밀값(API 키, DB 비밀번호)은 `.env`에** 두고 `.env.example`만 커밋한다
9. **커밋 메시지, PR 본문, 코드 주석에 `Co-Authored-By`, `Generated with` 같은 도구 서명을 넣지 않는다.** 이슈·PR 제목과 커밋은 `태그 : 내용` 형식만 쓴다 (예: `feat : 결과 화면`, docs/07)

## 5. 공통 약속 (용어와 단위)

| 항목 | 약속 |
|---|---|
| 금액 | **만원 단위 정수** (2억 4천만 원 = `24000`) |
| 날짜 | `YYYY-MM-DD` 문자열 |
| 답변 값 | `YES` / `NO` / `UNKNOWN` (모르겠어요). **`UNKNOWN`은 위험한 답으로 계산** |
| 등급 | `SAFE`(0~29) / `CAUTION`(30~59) / `DANGER`(60 이상) |
| 질문 코드 | `T1_ADDRESS` 처럼 docs/01에 정의된 코드 그대로 사용 |
| 일정 기준일 | `CONTRACT` / `BALANCE` / `MOVE_IN` / `EXPIRY` |

## 6. 실제 구현 / Mock / 미구현

| 기능 | 상태 |
|---|---|
| 등기부 질문 흐름 + 규칙 기반 위험 점수 | ✅ 실제 구현 |
| 계약 일정표 | ✅ 실제 구현 |
| AI 쉬운 말 설명 | 🟡 **오늘(11/9)은 템플릿 문장만.** AI 연동은 17시 이후 / 내일 오전 |
| 시세 | 🟡 사용자가 직접 입력 |
| 선택 로그인·마이페이지 진단 기록 | 🟡 실제 구현 예정 (회원별 서버 저장·수정·재평가) |
| 서류 OCR 및 보완 질문 | 🟡 진행 중 (D3-3, D6): 오늘 FE Mock, 실제 연동은 내일 12시 전 |
| 실거래가 API | ❌ 보류 (시간) |
| 블랙리스트 조회 | ❌ 제외 (D7) |

## 7. 저장소 구조

GitHub Organization: https://github.com/LGCNS-HackerTon

| 저장소 | 누가 | 안에 들어가는 것 |
|---|---|---|
| [BackEnd](https://github.com/LGCNS-HackerTon/BackEnd) | BE 3명, DB | 백엔드 코드, `db/` (DDL, seed), `CLAUDE.md`, `docs/` |
| [FrontEnd](https://github.com/LGCNS-HackerTon/FrontEnd) | FE 4명 | 프론트 코드, `CLAUDE.md`, `docs/` |

- **명세 문서와 에이전트 참고 파일은 전부 `docs/`에 둔다.** 맨 위에는 `README.md`와 `CLAUDE.md`(한 줄: `@docs/AGENTS.md`)만 둔다
- `CLAUDE.md`, `docs/`는 **두 저장소에 똑같이** 들어간다
- **원본은 BackEnd 저장소.** 문서를 고치면 팀장이 FrontEnd에도 똑같이 반영한다. FrontEnd에서 문서를 직접 고치지 않는다
