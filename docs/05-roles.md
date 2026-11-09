# 역할 분담과 체크리스트 (오늘 17:00 완성 기준)

> **목표: 오늘(11/9) 17:00 중간 공유 때 메인 기능 2개가 실제로 돌아가는 전체 데모.**
> 내일 오전은 AI 설명, 사용자 테스트, 버그 수정, 발표 준비에 쓴다.
> AI에게 일 시킬 때는 **docs/AGENTS.md + 내 역할의 "AI 시작 문장"**을 같이 준다.

## 1. 오늘 만드는 것 (메인 기능 2개)

| # | 기능 | 만드는 사람 | 17:00 상태 |
|---|---|---|---|
| 1 | **등기부 진단**: 주소·서류 첨부 → OCR → 미확인 항목 보완 → 규칙 기반 점수·등급 → 결과 화면(위험 이유, 중개사 질문, 특약 복사, 면책 문구) | BE1, BE2, FE | 기존 진단 기준. OCR 흐름은 개정 초안, 담당·일정 협의 중 |
| 2 | **계약 일정표**: 날짜 입력 → D-day 할 일 14개 | BE3, FE | ✅ 실제 구현 |
| + | AI 쉬운 말 설명 | BE2 | 🟡 오늘은 템플릿. **AI는 17시 이후 / 내일 오전** |

**오늘 안 만드는 것**: AI 호출, 공유 링크(`GET /api/diagnoses/{id}`), 실거래가 API, 블랙리스트. **OCR은 오늘 FE Mock, 실제 연동은 내일 12시 전** ([docs/09](09-feedback-ocr.md)). 누가 "이것도 넣자" 하면 decision-log에 적어두고 내일 판단.

## OCR 흐름 개정 체크리스트

- **FE**: 주소·첨부 단계, 추출 진행 및 실패 표시, 미확인 질문만 표시, 모두 확인되면 금액 단계로 이동.
- **BE**: 첨부 서류 OCR, 주소 일치 확인, 질문별 확인 여부와 채권최고액 추출. OCR 구현 담당과 파일 제약은 별도 협의.
- **QA**: 정보 충분, 일부 미확인, 판독 실패, 주소 불일치, 근저당 금액만 미확인인 케이스를 구분해 확인.
- 업로드 API 계약은 [docs/03 5번](03-api-contract.md), 서류 처리·보관 정책(저장 안 함)은 [docs/09](09-feedback-ocr.md)를 따른다. OCR 담당은 BE2 최다인.

## 2. 역할표

| 역할 | 이름 | 맡는 것 | 담당 위치 | 17:00까지 결과물 |
|---|---|---|---|---|
| **팀장 + FE 드라이버** | 이상혁 | 결정 마무리, PR 머지, 16:00 통합 진행 + **화면 4개 직접 개발** | FrontEnd 전체 | 화면 4개 동작, 17:00 데모 |
| **FE 리뷰어** | 레퍼런스 임수경·탁유진, 순서도·발표 안지산 | 드라이버가 만든 화면을 보고 **첨삭**(문구, 흐름, 빠진 것), 아래 3절 할 일 | FrontEnd PR 리뷰 | 화면별 수정 요청 목록, 3절 결과물 |
| **BE1** 진단 | 송석주 | `POST /api/diagnoses` + **점수 계산 로직** + 테스트 | BackEnd `diagnosis` | 데모 케이스 A/B/C = 0·70·40점 |
| **BE2** 결과 문구 | 최다인 | docs/01 4절 템플릿 → 코드 상수, `factors`·`commonAdvice`·템플릿 `aiExplanation`, BE1 지원. 17시 이후 AI 연동 | BackEnd `diagnosis.advice` | 진단 응답에 질문·특약 문구가 채워짐 |
| **BE3** 뼈대·일정 | 문한일 | 프로젝트 뼈대(에러 형식, CORS), `GET /api/questions`, `GET /api/schedule` | BackEnd `common`, `question`, `schedule` | 13:40까지 뼈대 PR, API 2개 동작 |
| **DB** | 손호성 | DDL, `seed.sql`(질문 10개·할 일 14개), 실행 방법 3줄, 발표용 ERD | BackEnd `db/` | 14:00까지 전원 로컬 DB 실행 |
| **QA** | 이지원 | 테스트 시나리오, 계약서대로 동작하는지 확인, decision-log 관리, 17:00 데모 대본 | BackEnd `qa/`, `docs/decision-log.md` | 시나리오 표 + 데모 리허설 1회 |

> **팀장이 FE까지 맡으니까 시간 관리를 나눠 들자.** 정각마다 체크(막힌 사람, PR 대기)와 16:00 통합 시간 알림은 FE 리뷰어 중 한 명이 맡아주면 팀장이 화면에 집중할 수 있어. (제안: 안지산)

## 3. FE는 이렇게 굴러간다 (드라이버 1명 + 리뷰어 3명)

화면 코드는 **이상혁 한 명만** 짠다 (충돌 없음). 리뷰어는 코드 대신 **화면을 보고 첨삭**한다.

```
이상혁: 화면 하나 완성 → PR 올리고 단톡에 캡처 → 리뷰어: 10분 안에 첨삭 → 이상혁: 반영 → 다음 화면
```

| 화면 | 순서 | 첨삭 담당 (제안) | 첨삭 기준 |
|---|---|---|---|
| 뼈대 + S1 첫 화면 | 1 (13:40) | 임수경 | docs/06 3절 S1, 용어 6개가 쉬운 말인지 |
| S2 진단 | 2 (14:40) | 안지산 | docs/06 S2, 주소·첨부 흐름, 미확인 질문 순서, 근저당 금액 보완, 만원 단위 표시 |
| S3 결과 | 3 (15:30) | 탁유진 | docs/06 S3 블록 8개 순서, 특약 복사, 면책 문구 |
| S4 일정표 | 4 (15:50) | 임수경 | docs/02, 지난 일/오늘/다가오는 일, 빨간 강조 |

### 리뷰어가 화면 기다리는 동안 할 일 (제안)
| 이름 | 할 일 |
|---|---|
| 안지산 | 시간 관리(정각 체크, 16:00 통합 알림), docs/01 질문·설명 문구가 해요체·쉬운 말인지 검수 |
| 탁유진 | 발표 슬라이드 뼈대 (Problem & Target, Key Decision) |
| 임수경 | 동기 사용자 테스트 질문지 5문항, 17시 이후 동기 2~3명에게 실제로 써보게 하기 |

## 4. 오늘 시간표와 통과 기준

| 시각 | 끝나 있어야 하는 것 | 확인하는 사람 |
|---|---|---|
| **13:40** | BackEnd 뼈대(문한일), FrontEnd 뼈대(이상혁), DB DDL(손호성)이 main에 머지 | 팀장 |
| **14:00** | 전원 clone 후 **각자 컴퓨터에서 BE·DB 실행됨** (FE는 이상혁 + 데모 노트북) | 각자 → 팀장 |
| **15:00** | BE: API 3개가 Postman에서 응답 / FE: Mock으로 S1→S2→S3 흐름 | BE·FE 각자 |
| **16:00 통합** | main pull → Mock 끄고 FE↔BE 실제 연결 | 팀장 진행, 안지산 알림 |
| **16:30 오늘 동결** | 새 기능 금지, 버그만. QA 시나리오 1회 통과 | 이지원 |
| **16:50** | 데모 리허설 1회 (발표 노트북에서) | 이지원·팀장 |
| **17:00 중간 공유** | 메인 기능 2개 전체 데모 | 전원 |

## 5. 역할별 체크리스트

### 공통 (전원)
- [ ] `git config` 이메일을 GitHub 계정 이메일로 맞추기 (docs/07 2절)
- [ ] 내 저장소 clone, 이슈 만들고 브랜치 따기 (`feat/3/diagnosis-score`처럼 `태그/이슈번호/기능`)
- [ ] AI에게 docs/AGENTS.md + 내 역할 시작 문장 주기
- [ ] 1시간에 한 번은 PR 올리기 (모아서 한 번에 올리지 않기)
- [ ] 15분 넘게 막히면 바로 단톡에 공유
- [ ] API 모양을 바꾸고 싶으면 **계약 변경 요청 이슈**부터

### BE1 진단 (송석주)
- [ ] 점수 계산 함수 먼저 (뼈대 없이도 가능): docs/01 1~3절
- [ ] 테스트: 케이스 A 0점 🟢, B 70점 🔴, C 40점 🟡
- [ ] `UNKNOWN`은 위험한 답으로 계산, `B1_MORTGAGE` `UNKNOWN`은 +10
- [ ] 입력 검증: 금액 1 이상, 질문 10개 빠짐없이 → 아니면 400 `INVALID_INPUT`
- [ ] `diagnoses`, `diagnosis_answers` 저장
- [ ] 응답 모양이 docs/03 2절 예시와 똑같은지 비교

### BE2 결과 문구 · OCR (최다인)
- [ ] docs/01 4절 질문·특약 문구, `DEBT_RATIO`·공통 안내를 코드 상수로
- [ ] `factors` 정렬: 점수 큰 순서, 근저당 있으면 0점이어도 마지막에
- [ ] 템플릿 `aiExplanation` 만들기, `aiUsed: false`
- [ ] 선순위 보증금이 없으면 `warnings`에 문장 추가
- [ ] (17시 이후) AI 연동: 타임아웃 5초, 실패하면 템플릿. 키는 `.env`
- [x] **OCR 엔진 결정: Naver CLOVA OCR** → decision-log D9 기록
- [ ] `ocr` 패키지에 `POST /api/ocr/registry` (docs/03 5번): `address` + `registry` + `buildingLedger`(선택) multipart
- [ ] 응답은 질문 **10개 전부** `CONFIRMED/UNCONFIRMED`. 판독 못 한 건 `NO`가 아니라 `UNCONFIRMED`
- [ ] `application.yml`에 업로드 10MB 설정 (`spring.servlet.multipart.max-file-size`, `max-request-size`. 기본 1MB)
- [ ] 서류·주소는 **저장 금지**(디스크·DB 쓰기 없음), 로그에 이름·주소·파일 내용 금지. 엔진 타임아웃 10초 → 422 `OCR_UNREADABLE`
- [ ] 손호성 샘플(케이스 B)로 응답이 docs/03 5번 예시와 같은지 확인

### BE3 뼈대·일정 (문한일)
- [ ] **13:40까지** 뼈대 PR: 실행되는 서버, 에러 응답 형식, CORS(`http://localhost:5173` 허용), docs/AGENTS.md 패키지 구조
- [ ] `.env.example`, 실행 방법을 README에 3줄
- [ ] `GET /api/questions` (seed 데이터 읽기)
- [ ] `GET /api/schedule`: 날짜 계산, `daysLeft`, `status`, 정렬
- [ ] 날짜 순서 검증 (계약일 ≤ 잔금일 ≤ 입주일)

### FE 드라이버 (이상혁)
- [ ] **13:40까지** 뼈대 PR: 라우팅 4개(`/`, `/diagnosis`, `/result`, `/schedule`), API 클라이언트 + Mock 스위치, 공통 스타일(등급 색 3개)
- [ ] S1 첫 화면: 용어 카드 6개, 버튼 2개
- [ ] S2 진단: 질문 하나씩 + 진행 표시, 예/아니오/모르겠어요, 근저당 금액칸, 금액 입력, 요청 JSON이 docs/03과 같은지
- [ ] S3 결과: 등급 카드, 위험 항목, 중개사 질문, **특약 복사 버튼**, warnings, 면책 문구
- [ ] S4 일정표: 날짜 입력, 세 묶음, 빨간 강조, 외부 링크 새 탭
- [ ] 화면 하나 끝날 때마다 PR + 캡처 공유 → 첨삭 반영

### FE 리뷰어 (안지산, 탁유진, 임수경)
- [ ] 담당 화면 PR이 올라오면 **10분 안에** 첨삭 (PR 댓글이나 단톡)
- [ ] 첨삭은 "어디가 / 왜 / 어떻게"로 짧게 (예: "S3 특약 문구가 너무 길어서 잘림 → 접기 버튼")
- [ ] 화면 문구가 쉬운 말인지, docs/06 순서와 같은지 확인
- [ ] 3절 "기다리는 동안 할 일"

### DB (손호성)
- [ ] docs/04 DDL을 `db/schema.sql`로
- [ ] `db/seed.sql`: 질문 10개(설명은 해요체로), 할 일 14개, 링크 URL
- [ ] `db/README.md` 실행 방법 3줄 → **14:00까지 BE 전원 로컬 실행 확인**
- [ ] 발표용 ERD 그림 1장
- [ ] **OCR 데모용 가짜 서류 샘플** 케이스 A/B/C: 등기부등본 + 건축물대장 이미지·PDF (실제 사람 이름·주소 금지, "말소사항 미포함" 형태) → `db/samples/`

### QA (이지원)
- [ ] 데모 케이스 A/B/C 기대 결과표 (docs/01 5절)
- [ ] 입력 오류 케이스: 금액 0, 질문 빠짐, 날짜 순서 거꾸로
- [ ] 15:00부터 Postman으로 BE 응답이 docs/03과 같은지 확인
- [ ] decision-log.md에 D5(팀 이름) 등 채우기
- [ ] docs/02 "확인 필요" 항목 공식 출처로 확인, 링크 4개 눌러보기
- [ ] 17:00 데모 대본: 케이스 B로 진단 → 결과 → 특약 복사 → 일정표
- [ ] (내일) OCR 시나리오 5개: 정보 충분 / 일부 미확인 / 전체 판독 실패(422) / 주소 불일치 / 근저당 금액만 미확인 → 샘플 서류로 확인
- [ ] (내일) 업로드 오류: 파일 없음, PDF·이미지 아님, 10MB 초과 → 400 `INVALID_INPUT`

## 6. 협업 규칙

> Git·GitHub 사용법(이슈, 브랜치, 커밋, PR, 충돌 해결)은 **[docs/07-git-rules.md](07-git-rules.md)**가 기준이야.

1. **docs/03(API), docs/04(DB)를 바꾸려면** 계약 변경 요청 이슈 → 문서 고치고 → 코드
2. **자기 위치만 수정.** 다른 곳은 담당자에게 요청
3. 브랜치 `태그/이슈번호/기능` (예: `feat/7/result-page`), 이슈·PR 제목과 커밋 `태그 : [역할] 한 일` (예: `feat : [BE1] 부채비율 계산 추가`)
4. PR은 **팀장이 머지.** 같은 영역 한 명이 훑어봤으면 바로 머지 (오늘은 리뷰보다 속도)
5. 막히면 15분 넘게 혼자 붙잡지 말기
6. 결정이 바뀌면 이지원(QA)에게 알려서 decision-log.md에 한 줄
7. `docs/AGENTS.md`와 `docs/`는 **BackEnd 저장소가 원본.** 고치면 팀장이 FrontEnd에도 똑같이 반영

## 7. 역할별 AI 시작 문장

docs/AGENTS.md를 첨부하고 시작.

| 역할 | AI 시작 문장 |
|---|---|
| BE1 | "docs/AGENTS.md와 docs/01, docs/03을 읽어. 나는 BE1이야. Spring Boot 3(Java 17, Gradle, JPA)으로 docs/01 2~3절 점수 규칙을 계산하는 함수와, 데모 케이스 A/B/C(0·70·40점) 테스트를 먼저 만들어줘." |
| BE2 | "docs/AGENTS.md와 docs/01 4절, docs/03 2절을 읽어. 나는 BE2야. 진단 점수 결과를 받아 factors, commonAdvice, 템플릿 aiExplanation, warnings를 docs/03 모양 그대로 만드는 코드를 Spring Boot 3(Java 17, Gradle, JPA)으로 만들어줘. 오늘은 AI를 부르지 않고 aiUsed=false." |
| BE2 OCR | "docs/AGENTS.md와 docs/01 1.1절, docs/03 5번, docs/09를 읽어. 나는 BE2야. ocr 패키지에 POST /api/ocr/registry를 Spring Boot 3로 만들어줘. multipart(address, registry, buildingLedger), 응답은 질문 10개 CONFIRMED/UNCONFIRMED. 엔진 호출은 인터페이스 하나로 감싸고, 파일은 저장하지 말고 로그에 개인정보를 남기지 마. 업로드 10MB 설정도 해줘." |
| BE3 | "docs/AGENTS.md와 docs/02, docs/03을 읽어. 나는 BE3야. Spring Boot 3(Java 17, Gradle, JPA) 프로젝트 뼈대(docs/AGENTS.md 패키지 구조, 에러 응답 형식, localhost:5173 CORS 허용)와 GET /api/questions, GET /api/schedule을 docs/03 그대로 만들어줘." |
| FE 뼈대 | "docs/AGENTS.md와 docs/03, docs/06을 읽어. React + Vite(JavaScript, Tailwind)로 docs/AGENTS.md 폴더 구조대로 뼈대를 만들고, 라우팅 4개와 API 클라이언트, docs/03 예시 JSON을 돌려주는 Mock 모드(VITE_USE_MOCK)를 만들어줘." |
| FE S2 | "docs/AGENTS.md와 docs/01 1~2절, docs/03 1~2절, docs/06 S2를 읽어. 질문을 섹션 순서대로 하나씩 보여주고 예/아니오/모르겠어요로 답하는 화면과 금액 입력 화면을 만들어줘. 마지막에 POST /api/diagnoses 요청 모양으로 모아줘." |
| FE S3 | "docs/AGENTS.md와 docs/03 2절, docs/06 S3를 읽어. 진단 응답 JSON으로 결과 화면을 만들어줘. 등급 색, factors 목록, 중개사 질문, 특약 복사 버튼, 면책 문구, warnings 표시가 필요해." |
| FE S4 | "docs/AGENTS.md와 docs/02, docs/03 4절, docs/06 S4를 읽어. 날짜 입력 → GET /api/schedule 결과를 지난 일/오늘/다가오는 일로 묶어 보여주는 화면을 만들어줘. important=true는 빨간 강조." |
| DB | "docs/AGENTS.md와 docs/01, docs/02, docs/04를 읽어. 나는 DB 담당이야. docs/04 DDL을 MySQL 8에 맞게 확인하고, docs/01 질문 10개(설명은 해요체로)와 docs/02 할 일 14개를 seed.sql로 만들어줘." |
| QA | "docs/AGENTS.md와 docs/01, docs/03을 읽어. 나는 QA야. 데모 케이스 A/B/C와 입력 오류 케이스로 테스트 시나리오 표를 만들고, 17시 데모 대본을 케이스 B 기준으로 써줘." |
