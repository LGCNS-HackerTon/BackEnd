# API 계약서 (FE ↔ BE 약속)

> 상태: **초안 v0.1**. 바꾸려면 팀에 먼저 말하고 이 문서부터 고친다. 버전을 올리고 맨 아래 변경 이력에 적기.
> FE는 BE가 완성되기 전까지 **이 문서의 예시 JSON을 Mock으로** 써서 화면을 만든다.

## 공통

| 항목 | 약속 |
|---|---|
| 주소 | `/api/...` |
| 형식 | JSON, UTF-8 |
| 금액 | 만원 단위 정수 |
| 날짜 | `YYYY-MM-DD` |

### 에러 응답 (모든 API 공통)

```json
{
  "error": {
    "code": "INVALID_INPUT",
    "message": "보증금은 0보다 커야 해요",
    "field": "deposit"
  }
}
```

| HTTP | code | 언제 |
|---|---|---|
| 400 | `INVALID_INPUT` | 입력값이 규칙에 안 맞음 (`field`에 어떤 값인지) |
| 404 | `NOT_FOUND` | 없는 진단 id |
| 500 | `INTERNAL_ERROR` | 서버 오류 (`field`는 `null`) |

`message`는 화면에 그대로 띄울 수 있는 쉬운 한국어로 쓴다.

---

## 1. 질문 목록 조회

`GET /api/questions`

### 응답 200

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

- `section`: `TITLE`(표제부) / `GAPGU`(갑구) / `EULGU`(을구) / `BUILDING`(건축물대장)
- `order` 순서대로 보여준다. 질문 내용은 docs/01 1절과 같다
- `needsAmount: true`인 질문(`B1_MORTGAGE`)에 `YES`라고 답하면 FE는 **채권최고액 입력칸**을 보여준다
- 어떤 답이 위험한지, 몇 점인지는 **응답에 넣지 않는다** (계산은 서버만)

---

## 2. 진단하기

`POST /api/diagnoses`

### 요청

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

| 필드 | 필수 | 규칙 |
|---|---|---|
| `deposit` | ✅ | 1 이상 정수 |
| `salePrice` | ✅ | 1 이상 정수 |
| `mortgageMax` | | 0 이상. `B1_MORTGAGE`가 `YES`가 아니면 서버는 0으로 본다 |
| `seniorDeposit` | | 0 이상. **빼거나 `null`이면 0으로 계산하고 `warnings`에 경고 추가** |
| `answers` | ✅ | 10개 질문 코드가 **빠짐없이 한 번씩**. 값은 `YES`/`NO`/`UNKNOWN` |

### 응답 201 (위 요청 = docs/01 데모 케이스 B)

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
  "aiUsed": true,
  "warnings": [],
  "disclaimer": "참고용이며 법적 판단이 아닙니다. 계약 전 공인중개사·전문가에게 확인하세요.",
  "createdAt": "2026-11-09T14:30:00"
}
```

| 필드 | 설명 |
|---|---|
| `score`, `grade` | docs/01 2~3절 규칙으로 계산 |
| `debtRatio` | 소수점 첫째 자리까지 (반올림) |
| `factors` | 점수가 0보다 큰 항목, **점수 큰 순서**. 단, 근저당이 있으면(`B1_MORTGAGE`=`YES`) 0점이어도 마지막에 넣는다 (말소 특약을 보여주려고) |
| `factors[].code` | 질문 코드 또는 `DEBT_RATIO` |
| `factors[].askAgent`, `specialClause` | docs/01 4절 템플릿. 특약으로 해결이 어려운 항목은 `specialClause: null` |
| `aiExplanation` | AI가 쓴 전체 설명. **AI 실패 시 템플릿 문장**으로 채우고 `aiUsed: false` |
| `warnings` | 예: `["선순위 보증금을 모르면 0으로 계산했어요. 다가구주택이라면 중개사에게 꼭 물어보세요."]` |

---

## 3. 진단 결과 다시 보기 (공유 링크용)

`GET /api/diagnoses/{id}`

- 응답 200: 2번의 응답 201과 **똑같은 모양**
- 없으면 404 `NOT_FOUND`

---

## 4. 계약 일정표

`GET /api/schedule?contractDate=2026-11-20&balanceDate=2026-12-20&moveInDate=2026-12-20&termMonths=24`

| 파라미터 | 필수 | 규칙 |
|---|---|---|
| `contractDate` | ✅ | |
| `balanceDate` | ✅ | `contractDate` 이후거나 같은 날 |
| `moveInDate` | | 없으면 `balanceDate`와 같은 날. `balanceDate` 이후거나 같은 날 |
| `termMonths` | | 기본 24. 1 이상 |

저장하지 않고 계산만 해서 돌려준다.

### 응답 200

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

| 필드 | 설명 |
|---|---|
| `date` | 기준일 + `offsetDays` (docs/02) |
| `daysLeft` | `date - today` (일). 음수면 지난 일 |
| `status` | `PAST`(daysLeft < 0) / `TODAY`(0) / `UPCOMING`(> 0) |
| `items` 정렬 | `date` 오름차순, 같으면 `order` 오름차순 |
| `link` | 없으면 `null` |

---

## 변경 이력

| 버전 | 날짜 | 내용 | 누가 |
|---|---|---|---|
| v0.1 | 11/9 | 초안 | |
