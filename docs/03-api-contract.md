# 전세역전 API 명세서

전세역전 (팀 공감) · LG CNS AM Inspire 6기 해커톤 · 1팀 · **FE ↔ BE 계약**

- 작성자: 이상혁 (팀장) · 작성일: 2026.11.09 · 버전: v1.3 · 기준: docs/01 점수 규칙, docs/02 일정 규칙, docs/08 요구사항
- 바꾸려면 **계약 변경 요청 이슈**부터 올리고 이 문서를 먼저 고친 뒤 코드를 고친다. 맨 아래 변경 이력에 적기
- FE는 BE가 완성되기 전까지 이 문서의 예시 JSON을 Mock으로 쓴다

## 1. 개요

| 항목 | 내용 |
| --- | --- |
| 범위 | API 4개 = P0 3개(질문 조회, 진단, 일정표) + P1 1개(등기부 OCR). 3번(진단 다시 보기)은 삭제 (D11). 회원 API 5개(6~10번, P1)는 6장 |
| 호출 주소 | `http://localhost:8080/api/...` (FE는 `VITE_API_BASE_URL`로 지정) |
| 형식 | JSON, UTF-8. **OCR(5번)만 요청이 `multipart/form-data`** |
| 인증 | 질문·진단·일정표·OCR은 인증 없이 호출한다. 회원 API는 **JWT Bearer 인증**을 사용한다 (6장). 진단(2번)은 로그인 회원이 토큰을 보내면 기록을 저장한다 |
| 담당 | 공통 규칙·에러 형식·CORS 문한일(BE3) · 진단 송석주(BE1), 결과 문구 최다인(BE2) · 질문·일정표 문한일(BE3) · OCR 최다인(BE2) · 회원가입·로그인·JWT·내 정보 문한일(BE3) · 로그인 회원 진단 저장·내 진단 기록·재평가 송석주(BE1) |
| 요구사항 | docs/08-requirements.md의 ID(DG, RS, SC)를 API마다 표기 |

## 2. 공통 규칙

| 항목 | 내용 |
| --- | --- |
| ① 경로 | 모두 `/api`로 시작한다. 복수형 명사 (`/api/questions`, `/api/diagnoses`) |
| ② 금액 | **만원 단위 정수** (2억 4천만 원 = `24000`). 소수 금지 |
| ③ 날짜 | 날짜 `YYYY-MM-DD`, 일시 `YYYY-MM-DDTHH:mm:ss` (시간대 없는 KST) |
| ④ enum | 답변 `YES` · `NO` · `UNKNOWN` / 등급 `SAFE` · `CAUTION` · `DANGER` / 섹션 `TITLE`(표제부) · `GAPGU`(갑구) · `EULGU`(을구) · `BUILDING`(건축물대장) / 일정 기준일 `CONTRACT` · `BALANCE` · `MOVE_IN` · `EXPIRY` / 일정 상태 `PAST` · `TODAY` · `UPCOMING` |
| ⑤ 질문 코드 | `T1_ADDRESS`, `T2_USAGE`, `A1_OWNER`, `A2_RECENT_OWNER`, `A3_TRUST`, `A4_SEIZURE`, `B1_MORTGAGE`, `B2_LEASE_REG`, `B3_JEONSE_RIGHT`, `R1_VIOLATION` (docs/01). 위험 항목에는 `DEBT_RATIO`도 쓴다 |
| ⑥ 에러 | 모든 API 공통 본문 `{"error": {"code", "message", "field"}}`. `message`는 화면에 그대로 띄울 수 있는 쉬운 한국어 해요체 (아래 표) |
| ⑦ CORS | `http://localhost:5173` 허용 (BE3) |
| ⑧ 점수 | 점수·등급은 **서버만** 계산한다. 질문 조회 응답에 위험한 답이나 점수를 넣지 않는다 |

### 공통 에러

```json
{
  "error": {
    "code": "INVALID_INPUT",
    "message": "보증금은 0보다 커야 해요",
    "field": "deposit"
  }
}
```

| code | HTTP | 상황 | field |
| --- | --- | --- | --- |
| `INVALID_INPUT` | 400 | 입력값이 규칙에 안 맞음, JSON 형식 오류, 없는 enum 값, 날짜 형식 오류 | 문제 필드 이름 (없으면 `null`) |
| `UNAUTHORIZED` | 401 | 로그인 필요한 API에 토큰 없음, 토큰이 잘못됐거나 만료됨, 로그인 실패 (6장) | 로그인 실패는 `password`, 나머지 `null` |
| `NOT_FOUND` | 404 | 없는 경로, 내 것이 아닌 진단 기록 (6장) | `null` |
| `DUPLICATE_EMAIL` | 409 | 이미 가입한 이메일로 회원가입 (6장) | `email` |
| `OCR_UNREADABLE` | 422 | 사진에서 등기부 글자를 읽지 못함, OCR 엔진 실패·시간 초과 | `null` |
| `INTERNAL_ERROR` | 500 | 처리 못 한 서버 오류. 내부 메시지는 숨긴다 | `null` |

## 3. API 목록

| No | 분류 | 이름 | 메서드 | EndPoint | 순위 | 담당 | 요구사항 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 진단 | 질문 목록 조회 | GET | `/api/questions` | P0 | 문한일 | DG-02 |
| 2 | 진단 | 진단하기 | POST | `/api/diagnoses` | P0 | 송석주, 최다인 | DG-07~11, RS-01~07 |
| 3 | ~~진단~~ | ~~진단 결과 다시 보기~~ | - | ~~`/api/diagnoses/{id}`~~ | **삭제 (D11)** | - | - |
| 4 | 일정표 | 계약 일정표 | GET | `/api/schedule` | P0 | 문한일 | SC-01~05 |
| 5 | 진단 | 등기부 OCR (서류로 답 채우기) | POST | `/api/ocr/registry` | P1 | 최다인 | DG-13~15 |
| 6 | 회원 | 회원가입 | POST | `/api/auth/signup` | P1 | 문한일 | AC-01 |
| 7 | 회원 | 로그인 | POST | `/api/auth/login` | P1 | 문한일 | AC-01 |
| 8 | 회원 | 내 정보 | GET | `/api/members/me` | P1 | 문한일 | AC-04 |
| 9 | 회원 | 내 진단 기록 (최근 5건) | GET | `/api/members/me/diagnoses` | P1 | 송석주 | AC-03, AC-04 |
| 10 | 회원 | 진단 기록 재평가 (덮어쓰기) | PUT | `/api/members/me/diagnoses/{id}` | P1 | 송석주 | AC-05 |

## 4. API 상세

### 1. [진단] 질문 목록 조회

`GET /api/questions` · 인증 없음 · P0 · 담당 문한일 · DG-02

| 항목 | 내용 |
| --- | --- |
| 기능 | 등기부 점검 질문 10개를 화면에 보여줄 순서대로 돌려준다 |
| 데이터 | `registry_questions` 테이블 (seed: docs/01 1절) |
| 화면 | S2 진단 · "준비됐어요"를 누를 때 호출 |

#### Query String / Path Variable

없음

#### Request Body

없음

#### Response Body — HTTP 200

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `questions` | array | 질문 10개, `order` 오름차순 |
| `questions[].code` | string | 질문 코드 (2장 ⑤) |
| `questions[].section` | string | `TITLE` / `GAPGU` / `EULGU` / `BUILDING` |
| `questions[].sectionName` | string | 표제부 / 갑구 / 을구 / 건축물대장 |
| `questions[].order` | int | 1~10 |
| `questions[].text` | string | 질문 문장 |
| `questions[].help` | string | 쉬운 설명 (해요체) |
| `questions[].needsAmount` | boolean | `true`면 `YES` 답에 채권최고액 입력칸을 연다 (`B1_MORTGAGE`만 `true`) |

```json
{
  "questions": [
    {
      "code": "T1_ADDRESS",
      "section": "TITLE",
      "sectionName": "표제부",
      "order": 1,
      "text": "표제부의 주소(동·호수까지)가 계약할 집 주소와 똑같나요?",
      "help": "다른 집 등기부를 보고 있을 수 있어요. 동·호수까지 맞는 등기부로 다시 발급해야 해요.",
      "needsAmount": false
    },
    {
      "code": "B1_MORTGAGE",
      "section": "EULGU",
      "sectionName": "을구",
      "order": 7,
      "text": "을구에 \"근저당권\"이 (빨간 줄 없이) 있나요?",
      "help": "이 집을 담보로 은행에서 돈을 빌렸다는 뜻이에요.",
      "needsAmount": true
    }
  ]
}
```

위험한 답(`risky_answer`)과 점수(`risk_score`)는 **응답에 넣지 않는다** (2장 ⑧).

#### 에러

| code | HTTP | 상황 |
| --- | --- | --- |
| `INTERNAL_ERROR` | 500 | 질문 데이터를 읽지 못함 |

---

### 2. [진단] 진단하기

`POST /api/diagnoses` · 인증 선택 (6장) · P0 · 담당 송석주(점수), 최다인(결과 문구) · DG-07~11, RS-01~07

| 항목 | 내용 |
| --- | --- |
| 기능 | 답 10개와 금액으로 위험 점수·등급을 계산하고, 위험 항목별 안내를 붙여 돌려준다. **`Authorization: Bearer` 토큰이 있으면(로그인 회원)** 진단 1건과 답 10개를 그 회원 기록으로 저장한다. 토큰이 없으면 저장하지 않는다 (D10) |
| 계산 규칙 | docs/01 1~3절 (점수표, 부채비율, 등급) |
| 데이터 | `diagnoses`, `diagnosis_answers` 저장 (로그인 회원만) · `registry_questions` 읽기 |
| 헤더 | `Authorization: Bearer <accessToken>` 선택. 없으면 비회원으로 계산만 한다. **있는데 잘못됐거나 만료되면 401 `UNAUTHORIZED`** (6장) |
| 화면 | S2 금액 입력 → "진단하기" · 응답으로 S3 결과를 그린다 |

#### Query String / Path Variable

없음

#### Request Body

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `deposit` | int | ✅ | 내 보증금. 1 이상 |
| `salePrice` | int | ✅ | 매매 시세. 1 이상 |
| `mortgageMax` | int | | 채권최고액 합계. 0 이상. `B1_MORTGAGE`가 `YES`가 아니면 서버는 0으로 본다 |
| `seniorDeposit` | int \| null | | 선순위 보증금. 0 이상. **빼거나 `null`이면 0으로 계산하고 `warnings`에 경고 추가.** `0`을 직접 보내면 경고 없음 |
| `answers` | array | ✅ | 질문 10개 코드가 **빠짐없이 한 번씩** |
| `answers[].code` | string | ✅ | 질문 코드 |
| `answers[].answer` | string | ✅ | `YES` / `NO` / `UNKNOWN` |

```json
{
  "deposit": 24000,
  "salePrice": 28000,
  "mortgageMax": 3600,
  "seniorDeposit": 0,
  "answers": [
    { "code": "T1_ADDRESS", "answer": "YES" },
    { "code": "T2_USAGE", "answer": "NO" },
    { "code": "A1_OWNER", "answer": "YES" },
    { "code": "A2_RECENT_OWNER", "answer": "YES" },
    { "code": "A3_TRUST", "answer": "NO" },
    { "code": "A4_SEIZURE", "answer": "NO" },
    { "code": "B1_MORTGAGE", "answer": "YES" },
    { "code": "B2_LEASE_REG", "answer": "NO" },
    { "code": "B3_JEONSE_RIGHT", "answer": "NO" },
    { "code": "R1_VIOLATION", "answer": "NO" }
  ]
}
```

#### Response Body — HTTP 201

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | long \| null | 저장된 진단 id. **비회원(토큰 없음)은 저장하지 않으므로 `null`** |
| `score` | int | 합계 점수 (docs/01 2~3절) |
| `grade` | string | `SAFE` / `CAUTION` / `DANGER` |
| `gradeLabel` | string | 안전 / 주의 / 위험 |
| `gradeMessage` | string | 등급 문구 (docs/01 3절) |
| `debtRatio` | number | 부채비율 %, 소수 첫째 자리 반올림. 점수 구간 판정은 반올림 전 값으로 |
| `factors` | array | 점수 > 0인 항목, **점수 큰 순서**. 단 `B1_MORTGAGE`가 `YES`면 0점이어도 **마지막에** 넣는다 (말소 특약 안내용) |
| `factors[].code` | string | 질문 코드 또는 `DEBT_RATIO` |
| `factors[].score` | int | 이 항목 점수 |
| `factors[].title` | string | 결과 화면 제목 (docs/01 4절 `factor_title`) |
| `factors[].explanation` | string | 쉬운 설명 |
| `factors[].askAgent` | string | 중개사에게 물어볼 질문 |
| `factors[].specialClause` | string \| null | 특약 문구. 특약으로 해결이 어려운 항목은 `null` → 화면에 "계약 보류를 권해요" |
| `commonAdvice` | object | 모든 진단에 붙는 공통 안내 (`askAgent`, `specialClause`) |
| `aiExplanation` | string | 결과 한 문단 설명. **AI 실패·미연동 시 템플릿 문장** |
| `aiUsed` | boolean | AI가 쓴 설명이면 `true`, 템플릿이면 `false` (오늘은 `false`) |
| `warnings` | string[] | 경고 문장. 없으면 `[]` |
| `disclaimer` | string | 면책 문구. **항상** 포함 |
| `createdAt` | string | 저장 일시 |

```json
{
  "id": 12,
  "score": 70,
  "grade": "DANGER",
  "gradeLabel": "위험",
  "gradeMessage": "계약을 다시 생각해 보세요. 전문가 상담을 권해요.",
  "debtRatio": 98.6,
  "factors": [
    {
      "code": "DEBT_RATIO",
      "score": 40,
      "title": "부채비율 98.6%",
      "explanation": "집을 팔아도 내 보증금을 다 못 돌려받을 수 있는 \"깡통전세\" 수준이에요.",
      "askAgent": "이 집 매매 시세가 얼마인지, 근거(실거래가)를 보여주실 수 있나요?",
      "specialClause": "전세보증금반환보증 가입이 거절될 경우 본 계약은 무효로 하며, 임대인은 받은 계약금을 즉시 반환한다."
    },
    {
      "code": "T2_USAGE",
      "score": 20,
      "title": "주택이 아닌 용도",
      "explanation": "\"근린생활시설\"은 원래 사람이 사는 집이 아니에요. 전세보증보험 가입이 거절될 수 있어요.",
      "askAgent": "이 건물 용도가 주택이 아닌데, 전세보증보험 가입이 되나요?",
      "specialClause": "전세보증금반환보증 가입이 거절될 경우 본 계약은 무효로 하며, 임대인은 받은 계약금을 즉시 반환한다."
    },
    {
      "code": "A2_RECENT_OWNER",
      "score": 10,
      "title": "최근 3개월 안에 집주인 변경",
      "explanation": "집을 사자마자 전세를 놓는 경우 \"바지 집주인\" 수법일 수 있어요.",
      "askAgent": "최근에 집주인이 바뀌었는데, 매매와 전세 계약이 동시에 진행되는 건가요?",
      "specialClause": "임대인은 잔금일 다음 날까지 소유권을 이전하지 않으며, 위반 시 임차인은 계약을 해제할 수 있다."
    },
    {
      "code": "B1_MORTGAGE",
      "score": 0,
      "title": "근저당권 있음 (채권최고액 3,600만 원)",
      "explanation": "이 집을 담보로 빌린 돈이 있어요. 금액은 부채비율에 반영했어요.",
      "askAgent": "근저당 채권최고액이 얼마이고, 잔금일에 갚고 말소해 주실 수 있나요?",
      "specialClause": "임대인은 잔금 지급과 동시에 을구의 근저당권을 말소하며, 말소되지 않으면 임차인은 계약을 해제하고 계약금 반환을 청구할 수 있다."
    }
  ],
  "commonAdvice": {
    "askAgent": "국세·지방세 체납은 없으신가요?",
    "specialClause": "임대인은 잔금일 다음 날까지 등기부상 권리관계를 현재 상태로 유지하며, 근저당권 등 새로운 권리를 설정하지 않는다. 위반 시 임차인은 계약을 해제할 수 있고 임대인은 손해를 배상한다."
  },
  "aiExplanation": "이 집은 위험 등급이에요. 가장 큰 이유는 빚과 보증금을 합친 금액이 집값의 98.6%라서, 집이 경매로 넘어가면 보증금을 다 돌려받기 어려울 수 있다는 점이에요. ...",
  "aiUsed": false,
  "warnings": [],
  "disclaimer": "참고용이며 법적 판단이 아닙니다. 계약 전 공인중개사·전문가에게 확인하세요.",
  "createdAt": "2026-11-09T14:30:00"
}
```

위 예시는 docs/01 **데모 케이스 B**다. `seniorDeposit`을 빼면 점수는 같고 `warnings`에 `"선순위 보증금을 모르면 0으로 계산했어요. 다가구주택이라면 중개사에게 꼭 물어보세요."`가 들어간다.

#### 에러

| code | HTTP | 상황 | field 예시 |
| --- | --- | --- | --- |
| `INVALID_INPUT` | 400 | 금액이 0 이하 또는 정수가 아님 | `deposit`, `salePrice` |
| `INVALID_INPUT` | 400 | `mortgageMax`·`seniorDeposit`이 음수 | `mortgageMax`, `seniorDeposit` |
| `INVALID_INPUT` | 400 | 질문이 빠졌거나 중복, 없는 질문 코드, 답이 enum이 아님 | `answers` |
| `INVALID_INPUT` | 400 | JSON 형식 오류 | `null` |
| `UNAUTHORIZED` | 401 | `Authorization` 헤더가 있는데 토큰이 잘못됐거나 만료됨 | `null` |

---

### 3. ~~[진단] 진단 결과 다시 보기~~ (삭제, D11)

`GET /api/diagnoses/{id}`는 **만들지 않는다.** 공유 기능을 하지 않고, 인증 없이 id만 알면 남의 진단을 볼 수 있는 구조라 위험하다. 로그인 회원의 기록 조회는 6장 회원 API가 맡는다. 번호는 다른 문서의 "5번 API" 참조가 바뀌지 않게 비워 둔다.

---

### 4. [일정표] 계약 일정표

`GET /api/schedule` · 인증 없음 · P0 · 담당 문한일 · SC-01~05

| 항목 | 내용 |
| --- | --- |
| 기능 | 계약 날짜로 D-day 할 일 16개의 날짜·남은 날·상태를 계산한다. **저장하지 않는다** |
| 계산 규칙 | docs/02 (기준일 + `offsetDays`) |
| 데이터 | `checklist_items` 읽기 |
| 화면 | S4 일정표 · "일정표 만들기" |

#### Query String

| 이름 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `contractDate` | date | ✅ | 계약일 |
| `balanceDate` | date | ✅ | 잔금일. `contractDate` 이후거나 같은 날 |
| `moveInDate` | date | | 입주일. 없으면 `balanceDate`. `balanceDate` 이후거나 같은 날 |
| `termMonths` | int | | 계약 기간(개월). 기본 24, 1 이상 |

```
GET /api/schedule?contractDate=2026-11-20&balanceDate=2026-12-20&moveInDate=2026-12-20&termMonths=24
```

#### Request Body

없음

#### Response Body — HTTP 200

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `today` | date | 서버 기준 오늘 |
| `expiryDate` | date | 만기일 = 입주일 + `termMonths` |
| `items` | array | 할 일 16개. `date` 오름차순, 같으면 `order` 오름차순 |
| `items[].order` | int | docs/02 순서 번호 (1~16) |
| `items[].anchor` | string | `CONTRACT` / `BALANCE` / `MOVE_IN` / `EXPIRY` |
| `items[].date` | date | 기준일 + `offsetDays` |
| `items[].daysLeft` | int | `date - today` (일). 음수면 지난 일 |
| `items[].status` | string | `PAST`(daysLeft < 0) / `TODAY`(0) / `UPCOMING`(> 0) |
| `items[].title` | string | 할 일 |
| `items[].reason` | string | 왜 해야 하는지 |
| `items[].important` | boolean | `true`면 빨간 강조 (9번, 11번) |
| `items[].link` | object \| null | `{ "name", "url" }`. 없으면 `null` |

```json
{
  "today": "2026-11-09",
  "expiryDate": "2028-12-20",
  "items": [
    {
      "order": 1,
      "anchor": "CONTRACT",
      "date": "2026-11-19",
      "daysLeft": 10,
      "status": "UPCOMING",
      "title": "등기부등본 발급해서 등기부 도우미로 점검",
      "reason": "계약 전에 위험 신호를 걸러내려고",
      "important": false,
      "link": { "name": "인터넷등기소", "url": "https://www.iros.go.kr" }
    },
    {
      "order": 11,
      "anchor": "MOVE_IN",
      "date": "2026-12-20",
      "daysLeft": 41,
      "status": "UPCOMING",
      "title": "당일 전입신고 (+ 확정일자 안 받았으면 같이)",
      "reason": "대항력은 전입신고 다음 날 0시에 생겨요. 하루라도 늦으면 그 사이에 생긴 빚보다 순위가 밀려요.",
      "important": true,
      "link": { "name": "정부24", "url": "https://www.gov.kr" }
    }
  ]
}
```

#### 에러

| code | HTTP | 상황 | field |
| --- | --- | --- | --- |
| `INVALID_INPUT` | 400 | 필수 날짜 없음, 날짜 형식 오류 | `contractDate`, `balanceDate`, `moveInDate` |
| `INVALID_INPUT` | 400 | 날짜 순서가 거꾸로 | `balanceDate` 또는 `moveInDate` |
| `INVALID_INPUT` | 400 | `termMonths`가 1 미만 | `termMonths` |

---

### 5. [진단] 등기부 OCR (서류로 답 채우기)

`POST /api/ocr/registry` · 인증 없음 · **P1** · 담당 최다인 · DG-13~15 · 결정 D3-3, D6 · 팀 안내 docs/09

| 항목 | 내용 |
| --- | --- |
| 기능 | 주소와 서류(등기부등본, 건축물대장)로 **질문 10개 각각의 확인 여부와 답**, 채권최고액 합계를 돌려준다. 점수는 계산하지 않는다 (점수는 2번 API만) |
| 흐름 | docs/06 S2: 주소 → 첨부 → **이 API** → 미확인 질문만 보완 → 금액 → 2번 API |
| 개인정보 | 서류·주소는 **저장하지 않는다** (메모리 처리 후 폐기). 응답·로그에 소유자 이름을 남기지 않는다. 단 `A1_OWNER` 비교용 소유자 이름은 `evidence`로 화면에만 보여준다 (NFR-06) |
| 엔진 | **Naver CLOVA OCR** (D9, docs/09 6절). 서류는 OCR 처리에만 전송하고 서버에 저장하지 않는다. 엔진이 바뀌어도 이 응답 모양은 같다 |
| 시간 | 엔진 호출 타임아웃 10초. 넘으면 422 `OCR_UNREADABLE` |

#### Request — `multipart/form-data`

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `address` | string | ✅ | 계약할 집 주소 + 동·호수 (예: `서울 마포구 ○○로 12, 101동 1204호`). 서류와 같은 집인지 비교하는 데만 쓴다 |
| `registry` | file | ✅ | 등기부등본 1개. JPG · PNG · PDF, 10MB 이하. **"말소사항 미포함"** 발급본 권장 |
| `buildingLedger` | file | ✅ | 건축물대장 1개 (같은 형식·크기). 계약할 호수가 나오는 것 (정부24 무료) |

#### Response Body — HTTP 200

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `addressMatch` | string | `MATCH` / `MISMATCH` / `UNKNOWN`. `MISMATCH`면 FE는 이 서류 결과를 쓰지 않고 **주소 수정 또는 재첨부**로 보낸다 |
| `answers` | array | **질문 10개 전부**, `order` 순서 |
| `answers[].code` | string | 질문 코드 |
| `answers[].status` | string | `CONFIRMED`(서류로 확인) / `UNCONFIRMED`(확인 못 함 → 사용자가 직접 답) |
| `answers[].answer` | string \| null | `CONFIRMED`면 `YES`/`NO`, `UNCONFIRMED`면 `null`. **판독 실패를 `NO`로 채우지 않는다** |
| `answers[].evidence` | string \| null | 근거 문장 (화면에 보여줌). 예: `"을구 1번 근저당권설정 채권최고액 금36,000,000원"` |
| `mortgageMax` | int \| null | 유효한 근저당 채권최고액 합계 (만원). 근저당이 `CONFIRMED YES`인데 금액을 못 읽으면 `null` → FE는 **금액만** 입력받는다 |
| `housingType` | string \| null | 건축물대장의 주택 종류: `APARTMENT` · `MULTIPLEX`(다세대·연립) · `MULTI_HOUSEHOLD`(다가구) · `DETACHED`(단독) · `OFFICETEL` · `NON_RESIDENTIAL`(근린생활시설 등) · 못 읽으면 `null`. FE는 `MULTI_HOUSEHOLD`·`DETACHED`일 때 **선순위 보증금 확인 안내**를 띄운다 (RS-11) |
| `warnings` | string[] | 예: `"을구 일부를 읽지 못했어요. 해당 질문은 직접 답해 주세요."` |

질문별로 확인하는 근거는 docs/01 1.1절을 따른다. `A1_OWNER`는 계약 상대방과 비교할 근거가 서버에 없으므로 보통 `UNCONFIRMED`로 돌려주고, `evidence`에 등기부상 소유자 이름을 보여준다.

```json
{
  "addressMatch": "MATCH",
  "answers": [
    { "code": "T1_ADDRESS", "status": "CONFIRMED", "answer": "YES", "evidence": "표제부 주소·동·호수가 입력 주소와 같음" },
    { "code": "T2_USAGE", "status": "CONFIRMED", "answer": "NO", "evidence": "표제부 건물내역: 제2종근린생활시설" },
    { "code": "A1_OWNER", "status": "UNCONFIRMED", "answer": null, "evidence": "등기부상 소유자: 홍○○ (계약 상대방과 같은지 확인해 주세요)" },
    { "code": "A2_RECENT_OWNER", "status": "CONFIRMED", "answer": "YES", "evidence": "갑구 3번 소유권이전 2026년9월1일 접수" },
    { "code": "A3_TRUST", "status": "CONFIRMED", "answer": "NO", "evidence": "갑구에 유효한 신탁 기록 없음" },
    { "code": "A4_SEIZURE", "status": "CONFIRMED", "answer": "NO", "evidence": "갑구에 유효한 압류·가압류·가처분·가등기·경매 기록 없음" },
    { "code": "B1_MORTGAGE", "status": "CONFIRMED", "answer": "YES", "evidence": "을구 1번 근저당권설정 채권최고액 금36,000,000원" },
    { "code": "B2_LEASE_REG", "status": "CONFIRMED", "answer": "NO", "evidence": "을구에 유효한 주택임차권 기록 없음" },
    { "code": "B3_JEONSE_RIGHT", "status": "UNCONFIRMED", "answer": null, "evidence": "을구 2번 일부를 읽지 못함" },
    { "code": "R1_VIOLATION", "status": "CONFIRMED", "answer": "NO", "evidence": "건축물대장에 위반건축물 표시 없음" }
  ],
  "mortgageMax": 3600,
  "housingType": "NON_RESIDENTIAL",
  "warnings": []
}
```

위 예시는 데모 케이스 B 등기부다. FE는 `CONFIRMED` 답을 유지하고 `UNCONFIRMED` 2개(`A1_OWNER`, `B3_JEONSE_RIGHT`)만 질문으로 보여준 뒤, 합친 10개로 2번 API를 부른다.

#### 에러

| code | HTTP | 상황 | field |
| --- | --- | --- | --- |
| `INVALID_INPUT` | 400 | 주소 없음 | `address` |
| `INVALID_INPUT` | 400 | 등기부 또는 건축물대장 파일 없음, 형식이 JPG·PNG·PDF가 아님, 10MB 초과 | `registry` 또는 `buildingLedger` |
| `OCR_UNREADABLE` | 422 | 서류 전체를 읽지 못함, 등기부가 아님, 엔진 실패·시간 초과 → FE는 재첨부 또는 10개 직접 답하기로 진행 | `null` |

## 6. 선택 로그인과 마이페이지 진단 기록

- 로그인은 선택이다. 비로그인도 진단·결과·일정표를 모두 사용한다
- 비로그인 진단은 서버에 저장하지 않는다
- 로그인 회원의 매물 별칭·금액·질문 10개 원본 답변·결과는 서버 DB에 저장한다
- 마이페이지에서는 본인의 **최근 5건**을 `createdAt` 오름차순(최초 진단 순)으로 조회한다
- 입력을 수정해 재평가하면 전체 결과를 다시 계산하고 **기존 기록을 덮어쓴다**
- 다른 회원의 기록 조회·수정은 거부한다

### 6.1 인증 규칙

| 항목 | 내용 |
| --- | --- |
| 방식 | 서버가 발급한 **JWT**(HS256)를 `Authorization: Bearer <accessToken>` 헤더로 보낸다. 쿠키·세션은 쓰지 않는다 |
| 토큰 | 회원가입(6번)·로그인(7번) 응답의 `accessToken`. 유효 시간 **24시간**. 서명 키는 서버 환경 변수 `JWT_SECRET` |
| 로그아웃 | 서버 API 없음. FE가 저장한 토큰을 지운다 |
| 토큰 필수 | 8 · 9 · 10번. 토큰이 없거나 잘못됐거나 만료되면 401 `UNAUTHORIZED` → FE는 로그아웃 처리 후 로그인 화면으로 보낸다 |
| 토큰 선택 | 2번 진단. 없으면 비회원(저장 안 함), 있으면 그 회원 기록으로 저장, **있는데 잘못됐으면 401** |
| 토큰 안 봄 | 1번 질문 · 4번 일정표 · 5번 OCR은 `Authorization` 헤더를 읽지 않는다. 토큰 상태와 상관없이 항상 동작한다 |
| 이메일 | 서버가 앞뒤 공백을 지우고 **소문자로 바꿔** 저장·비교한다 (`Demo@Example.com` = `demo@example.com`) |
| 비밀번호 | 서버에 **BCrypt 해시로만** 저장한다 (NFR-06). 응답·로그에 넣지 않는다 |
| Mock 모드 | `VITE_USE_MOCK=true`이면 FE는 지금처럼 브라우저 저장 데모 로그인·기록을 그대로 쓰고 이 API들을 부르지 않는다 |
| 데모 계정 | 서버 시작 때 없으면 만든다: 이메일 `demo@example.com`, 이름 `데모 사용자`, 비밀번호 `DEMO_PASSWORD` (기본 `jeonse-demo-2026`). 기록은 미리 넣지 않는다. FE "데모 계정으로 시작"은 서버 모드에서 이 계정으로 7번 로그인 |

### 6.2 API 상세

#### 6. [회원] 회원가입

`POST /api/auth/signup` · 인증 없음 · P1 · 담당 문한일 · AC-01

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `name` | string | ✅ | 앞뒤 공백 제거 후 1~30자 |
| `email` | string | ✅ | 이메일 형식, 254자 이하. 소문자로 정규화 |
| `password` | string | ✅ | 8~128자. 그리고 UTF-8 **72바이트 이하** (BCrypt 한계. 영문·숫자는 72자, 한글은 24자까지) |

```json
{ "name": "김전세", "email": "jeonse@example.com", "password": "password123" }
```

**Response — HTTP 201**: 가입과 동시에 로그인된다. 모양은 7번 로그인 응답과 같다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `accessToken` | string | JWT. 이후 요청의 `Authorization: Bearer` 값 |
| `member.id` | long | 회원 id |
| `member.name` | string | 이름 |
| `member.email` | string | 정규화한 이메일 |

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzYyNzMwMDAwLCJleHAiOjE3NjI4MTY0MDB9.xxxx",
  "member": { "id": 1, "name": "김전세", "email": "jeonse@example.com" }
}
```

| code | HTTP | 상황 | field |
| --- | --- | --- | --- |
| `INVALID_INPUT` | 400 | 이름·이메일·비밀번호 규칙 위반, JSON 형식 오류 | `name`, `email`, `password` (JSON 오류는 `null`) |
| `DUPLICATE_EMAIL` | 409 | 이미 가입한 이메일 (대소문자만 다른 경우 포함). 메시지 `"이미 사용 중인 이메일이에요."` | `email` |

#### 7. [회원] 로그인

`POST /api/auth/login` · 인증 없음 · P1 · 담당 문한일 · AC-01

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `email` | string | ✅ | 소문자로 정규화해 찾는다 |
| `password` | string | ✅ | |

```json
{ "email": "jeonse@example.com", "password": "password123" }
```

**Response — HTTP 200**: 6번 응답과 같은 모양 (`accessToken`, `member`).

| code | HTTP | 상황 | field |
| --- | --- | --- | --- |
| `INVALID_INPUT` | 400 | 이메일·비밀번호가 비었음, JSON 형식 오류 | `email`, `password` (JSON 오류는 `null`) |
| `UNAUTHORIZED` | 401 | 없는 이메일 또는 비밀번호 틀림 (둘을 구분하지 않는다). 메시지 `"이메일 또는 비밀번호를 확인해 주세요."` | `password` |

#### 8. [회원] 내 정보

`GET /api/members/me` · **토큰 필수** · P1 · 담당 문한일 · AC-04 (마이페이지 프로필)

**Response — HTTP 200**

```json
{ "id": 1, "name": "김전세", "email": "jeonse@example.com" }
```

| code | HTTP | 상황 | field |
| --- | --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 없음, 잘못됨, 만료됨, 탈퇴 등으로 회원이 없음 | `null` |

#### 9. [회원] 내 진단 기록 (최근 5건)

`GET /api/members/me/diagnoses` · **토큰 필수** · P1 · 담당 송석주 · AC-03, AC-04

| 항목 | 내용 |
| --- | --- |
| 기능 | 내 진단 중 `createdAt`이 가장 최근인 **5건**을 골라 `createdAt` **오름차순**(최초 진단 순)으로 돌려준다. 오래된 기록은 지우지 않고 목록에서만 빠진다 |
| 저장 시점 | 로그인 회원이 2번 진단을 부를 때 (토큰 있음) |
| 데이터 | `diagnoses` (`user_id` = 나), `diagnosis_answers` |

**Response — HTTP 200**

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `diagnoses` | array | 최대 5건, `createdAt` 오름차순. 없으면 `[]` |
| `diagnoses[].id` | long | 진단 id (10번 재평가 경로에 쓴다) |
| `diagnoses[].location` | string | 매물 별칭 (`property_name`). 저장 시 서버가 `"보증금 2억 4,000만 원 · 시세 2억 8,000만 원"` 형식으로 만든다 (FE 데모와 같은 문구). 재평가하면 새 금액으로 다시 만든다 |
| `diagnoses[].createdAt` | string | 최초 진단 일시. 재평가해도 바뀌지 않는다 |
| `diagnoses[].updatedAt` | string \| null | 마지막 재평가 일시. 재평가한 적 없으면 `null` |
| `diagnoses[].score` | int | 점수 |
| `diagnoses[].grade` | string | `SAFE` / `CAUTION` / `DANGER` |
| `diagnoses[].gradeLabel` | string | 안전 / 주의 / 위험 |
| `diagnoses[].request` | object | 진단할 때 보낸 입력 그대로 (2번 Request Body와 같은 모양). 마이페이지에서 조건을 고쳐 10번으로 다시 보낸다 |
| `diagnoses[].request.deposit` | int | 내 보증금 (만원) |
| `diagnoses[].request.salePrice` | int | 매매 시세 (만원) |
| `diagnoses[].request.mortgageMax` | int | 채권최고액 합계 (만원). `B1_MORTGAGE`가 `YES`가 아니면 `0` |
| `diagnoses[].request.seniorDeposit` | int \| null | 선순위 보증금. 진단 때 비웠으면(`senior_unknown`) `null` |
| `diagnoses[].request.answers` | array | 질문 10개 `{ "code", "answer" }`, 질문 `order` 순서 |

```json
{
  "diagnoses": [
    {
      "id": 12,
      "location": "보증금 2억 4,000만 원 · 시세 2억 8,000만 원",
      "createdAt": "2026-11-09T14:30:00",
      "updatedAt": "2026-11-10T09:12:00",
      "score": 70,
      "grade": "DANGER",
      "gradeLabel": "위험",
      "request": {
        "deposit": 24000,
        "salePrice": 28000,
        "mortgageMax": 3600,
        "seniorDeposit": 0,
        "answers": [
          { "code": "T1_ADDRESS", "answer": "YES" },
          { "code": "T2_USAGE", "answer": "NO" },
          { "code": "A1_OWNER", "answer": "YES" },
          { "code": "A2_RECENT_OWNER", "answer": "YES" },
          { "code": "A3_TRUST", "answer": "NO" },
          { "code": "A4_SEIZURE", "answer": "NO" },
          { "code": "B1_MORTGAGE", "answer": "YES" },
          { "code": "B2_LEASE_REG", "answer": "NO" },
          { "code": "B3_JEONSE_RIGHT", "answer": "NO" },
          { "code": "R1_VIOLATION", "answer": "NO" }
        ]
      }
    }
  ]
}
```

| code | HTTP | 상황 | field |
| --- | --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 없음, 잘못됨, 만료됨 | `null` |

#### 10. [회원] 진단 기록 재평가 (덮어쓰기)

`PUT /api/members/me/diagnoses/{id}` · **토큰 필수** · P1 · 담당 송석주 · AC-05

| 항목 | 내용 |
| --- | --- |
| 기능 | 고친 입력으로 2번과 **같은 규칙**으로 전체 결과를 다시 계산하고, 그 기록(진단 1건 + 답 10개)을 **덮어쓴다**. 새 기록을 만들지 않는다 |
| 권한 | `{id}`가 **내 진단일 때만**. 남의 진단이거나 없는 id면 404 (남의 기록이 있는지도 알려주지 않는다) |
| 유지 | `id`, `createdAt`은 그대로. `updatedAt`이 갱신된다 |

**Path Variable**: `id` (long) — 9번 목록의 `diagnoses[].id`

**Request Body**: 2번 진단하기 Request Body와 **같다** (`deposit`, `salePrice`, `mortgageMax`, `seniorDeposit`, `answers`).

**Response — HTTP 200**: 2번 진단하기 Response Body와 **같은 모양**. `id`는 `{id}`와 같고, `createdAt`은 최초 진단 일시다. FE는 이 응답으로 S3 결과를 그린다.

| code | HTTP | 상황 | field |
| --- | --- | --- | --- |
| `INVALID_INPUT` | 400 | 2번과 같은 입력 검증 | 2번과 같음 |
| `UNAUTHORIZED` | 401 | 토큰 없음, 잘못됨, 만료됨 | `null` |
| `NOT_FOUND` | 404 | 내 것이 아니거나 없는 진단 id. 메시지 `"진단 기록을 찾을 수 없어요."` | `null` |

### 6.3 FE 화면과 대응

| FE 동작 (지금 데모) | 서버 모드 (`VITE_USE_MOCK=false`) |
| --- | --- |
| `registerAccount({ name, email, password })` | 6번 → 받은 `accessToken` 저장 후 로그인 상태 |
| `verifyAccount(email, password)` | 7번 |
| `readProfile(email)` | 8번 |
| `readHistory(email)` | 9번 (`date` ← `createdAt`) |
| 진단 후 `saveDiagnosis(...)` (새 진단) | 2번에 토큰을 붙이면 서버가 저장. 별도 호출 없음 |
| 마이페이지에서 고쳐 다시 진단 (`replaceId`) | 2번 대신 10번 `PUT .../diagnoses/{id}` |
| 로그아웃 | 토큰 삭제 (API 없음) |

## 변경 이력

| 버전 | 날짜 | 내용 | 작성 |
| --- | --- | --- | --- |
| v0.1 | 11/9 | 초안 (예시 JSON 중심) | 이상혁 |
| v1.0 | 11/9 | 명세서 형식으로 정리: 공통 규칙, API 목록, 필드 표, 에러 표, 요구사항 ID, 프론트 데모 기능. API 모양은 v0.1과 같음 | 이상혁 |
| v1.1 | 11/9 | 강사님 피드백 반영 (D6): 5번 등기부 OCR API 추가 (안지산 #11 흐름: 주소+서류, 질문별 CONFIRMED/UNCONFIRMED), 에러 `OCR_UNREADABLE` 추가. 1~4번은 변경 없음 | 이상혁 |
| v1.2 | 11/9 | D11: 3번 `GET /api/diagnoses/{id}` 삭제, 5번 건축물대장 필수 + `housingType` 추가, 일정표 할 일 16개 | 이상혁 |
| v1.3 | 11/10 | D10 회원 API 확정: 6~10번(회원가입·로그인·내 정보·내 진단 기록·재평가) 추가, 에러 `UNAUTHORIZED`·`DUPLICATE_EMAIL` 추가, 2번 진단은 토큰이 있을 때만 저장하고 비회원은 `id: null`. 2번 요청·응답 필드 이름은 그대로 | 이상혁 |
| v1.3 | 11/10 | 최다인 #21 안 반영: 경로 `/api/auth/signup`·`/api/auth/login`, 가입 필드 제한, 담당(회원가입·로그인·JWT·내 정보 문한일 / 진단 저장·기록·재평가 송석주). 기록 개수·정렬은 #21(10건·최신순)이 아니라 현재 FE(5건·오름차순, D10) 기준. 재평가(10번)는 FE에 기능(D8)이 있어 포함 | 이상혁 |
| v1.3 | 11/10 | 6.1에 데모 계정(`demo@example.com`, 서버 시작 때 생성) 한 줄 추가. API 모양은 그대로 | 송석주 |
