# BE1 → BE2 진단 결과 전달 형식

상태 · 협의 초안

BE1과 BE2는 진단 API에 두 로직을 연결하기 전에 내부 전달 형식을 합의한다. BE1이 진단 점수와 항목별 결과를 계산해 넘기면, BE2가 사용자에게 보여줄 안내 문구를 만든다. BE1은 두 결과를 합쳐 프론트엔드에 반환한다. 최종 응답은 [API 계약서 2절](03-api-contract.md#2-진단하기)을 따른다.

## BE1이 BE2에 넘길 값

| 필드 | Java 타입 | B 케이스 예시 |
|---|---|---|
| `score` | `int` | `70` |
| `grade` | `RiskGrade` | `DANGER` |
| `debtRatio` | `BigDecimal` | `98.6` |
| `debtRatioScore` | `int` | `40` |
| `scoredAnswers` | `List<ScoredAnswer>` | 질문 10개의 `code`, `answer`, `score`. 예를 들어 `T2_USAGE / NO / 20`, `A2_RECENT_OWNER / YES / 10`, `B1_MORTGAGE / YES / 0` |
| `mortgageMax` | `int` | 계산에 반영된 채권최고액 `3600`(만원) |
| `seniorDepositMissing` | `boolean` | `false`. B 케이스는 선순위 보증금에 `0`을 직접 입력 |

위 필드명과 타입은 백엔드 내부 전달에 사용한다. `grade`는 BE1이 계산하며, enum은 `com.lgcns.jeonse.domain.diagnosis.entity.RiskGrade`에 둔다. 값은 `SAFE`, `CAUTION`, `DANGER`이며, BE2의 `domain.diagnosis.advice` 패키지는 이 enum을 import해서 사용한다. 최종 API 응답은 기존 명세대로 `"grade": "DANGER"`처럼 JSON 문자열로 보낸다.

BE2는 `AdviceResult`에 `factors`, `commonAdvice`, `aiExplanation`, `warnings`와 세 문자열 필드 `gradeLabel`, `gradeMessage`, `disclaimer`를 채워 돌려준다. BE1은 이 세 문구를 최종 응답에 그대로 복사한다. `disclaimer`는 등급과 관계없이 항상 포함한다. 템플릿으로 설명을 제공할 때 최종 응답의 `aiUsed`는 `false`다.

## 1. 근저당권은 왜 0점이어도 안내하는가

B 케이스의 사용자는 근저당권 질문에 `YES`로 답하고 채권최고액 3,600만 원을 입력한다. BE1은 이 금액을 부채비율 계산에 반영한다.

```text
(내 보증금 24,000 + 채권최고액 3,600 + 선순위 보증금 0)
÷ 매매 시세 28,000 × 100 ≈ 98.6% → 40점
```

`B1_MORTGAGE`의 0점은 **근저당 질문 자체의 추가 점수**다. 채권최고액은 부채비율에 이미 반영됐다. 사용자는 잔금일에 근저당을 갚고 등기에서 지울 수 있는지 확인해야 하므로, BE2는 `scoredAnswers`에서 `B1_MORTGAGE`가 `YES`인지 확인하고 0점이어도 `factors` 마지막에 말소 관련 질문과 특약 안내를 넣는다.

| B 케이스 표시 순서 | 항목 | 점수 |
|---|---|---:|
| 1 | `DEBT_RATIO` · 부채비율 | 40 |
| 2 | `T2_USAGE` · 주택이 아닌 용도 | 20 |
| 3 | `A2_RECENT_OWNER` · 최근 소유자 변경 | 10 |
| 4 | `B1_MORTGAGE` · 근저당권 안내 | 0 |

## 2. 선순위 보증금이 둘 다 0인데 왜 경고가 다른가

선순위 보증금은 내 보증금보다 먼저 돌려받을 권리가 있는 다른 세입자의 보증금이다. 사용자가 금액을 생략하면 서버는 계산에 0을 사용한다. BE1은 **0으로 바꾸기 전에 누락 여부**를 확인해서 BE2에 넘긴다.

| 사용자가 보낸 값 | BE1이 계산에 쓰는 값 | `seniorDepositMissing` | BE2의 선순위 보증금 경고 |
|---|---:|---|---|
| `seniorDeposit: 0` | 0만 원 | `false` | 없음 |
| 필드 생략 또는 `seniorDeposit: null` | 0만 원 | `true` | 선순위 보증금을 모르면 0으로 계산했어요. 다가구주택이라면 중개사에게 꼭 물어보세요. |

B 케이스의 다른 입력이 같으면 두 경우 모두 **부채비율 98.6%, 총점 70점**이다. 경고만 달라진다. `seniorDepositMissing`은 사용자가 0을 직접 입력했는지 서버가 누락을 0으로 처리했는지 구분하기 위한 내부 필드명이다.

## 함께 확인할 기준

- [ ] B 케이스는 70점, `DANGER`이며 `factors`가 위 순서대로 표시된다.
- [ ] B 케이스에서 `seniorDeposit`을 생략하거나 `null`로 보내면 점수는 그대로이고 누락 경고가 추가된다.
- [ ] 최종 응답의 필드명과 구조가 API 계약서 2절에 맞는다.

[질문과 점수 규칙](01-registry-questions.md) · [API 계약서](03-api-contract.md) · [역할 문서](05-roles.md)
