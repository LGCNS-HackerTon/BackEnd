# 전세역전 Design System

<!-- design-md:section experience -->
## 1. Experience

<!-- design-md:claim scope kind=product-surface lang=en -->
### Scope

등기부등본 같은 공문서를 처음 읽는 20~30대 사회초년생이 계약 전 위험 신호를 쉬운 말로 확인하고, 중개사 질문·특약 문구·D-day 할 일까지 챙기게 돕는 입문 가이드. HUG 안심전세 앱·인터넷등기소 같은 기존 서비스로 이어주는 역할이다.
<!-- design-md:claim-end -->

<!-- design-md:claim primary-tasks kind=user-outcomes count=3 lang=en -->
### Primary tasks

- 계약할 집 주소와 등기부등본·건축물대장을 올리고, 서류로 확인 못 한 질문에만 답하기 (S2)

- 등급·위험 이유·중개사에게 물어볼 질문·특약 문구를 읽고 복사하기 (S3)

- 계약일·잔금일·입주일로 지난 일/오늘 할 일/다가오는 일 확인하기 (S4)
<!-- design-md:claim-end -->

### Design direction

- 차분하고 믿을 수 있는 파란 톤

- 한 화면에 한 가지 할 일

- 어려운 서류를 대신 읽어 주는 친근한 안내

### Principles

- 위험 등급은 색만이 아니라 글자(안전/주의/위험)와 점수로도 함께 보여준다

- 법률 용어가 나오면 바로 옆에 쉬운 풀이를 붙인다

- 결과 화면에는 면책 문구를 항상 보여준다

- 판독하지 못한 항목은 안전하다고 표시하지 않고 사용자에게 묻는다

- 입력한 답은 오류가 나도 지우지 않는다

### Avoid

- 겁주는 빨간 화면이나 과장된 경고 연출

- 설명 없이 쓰는 법률 용어

- 점수·등급을 AI가 판단한 것처럼 보이게 하는 표현

- 장식용 그라데이션·아이콘 남발 (첫 화면 히어로 1곳만 허용)

<!-- design-md:section foundations -->
## 2. Foundations

<!-- design-md:claim foundations kind=rules-or-constraints lang=en -->
### Semantic tokens

- **color.accent.eyebrow**: `#2453bd` — 섹션 위 작은 라벨
- **color.action.on-primary**: `#ffffff` — 주 버튼 글자
- **color.action.primary**: `#2458d5` — 주 버튼·링크·로고 (--blue)
- **color.action.primary.hover**: `#1945b3` — 주 버튼 hover
- **color.action.quiet.text**: `#3e526d` — 조용한 버튼(헤더 메뉴 등) 글자
- **color.action.secondary.text**: `#244166` — 보조 버튼 글자
- **color.border.control**: `#cbd8eb` — 보조 버튼·선택지 테두리
- **color.border.default**: `#dce5f2` — 카드·헤더·푸터 구분선 (--border)
- **color.border.input**: `#aebed4` — 입력칸 테두리
- **color.choice.selected.border**: `#4978dc` — 선택된 답 테두리
- **color.choice.selected.surface**: `#eaf2ff` — 선택된 답 배경
- **color.choice.selected.text**: `#2049a8` — 선택된 답 글자
- **color.focus.ring**: `#507ffa` — focus-visible 3px 외곽선
- **color.grade.caution.border**: `#f1dab0` — CAUTION 등급 테두리
- **color.grade.caution.surface**: `#fff6e4` — CAUTION 등급 배경
- **color.grade.caution.text**: `#765019` — CAUTION 등급 글자
- **color.grade.danger.border**: `#f4c9c4` — DANGER 등급 테두리
- **color.grade.danger.surface**: `#fff1f0` — DANGER 등급 배경
- **color.grade.danger.text**: `#8c2821` — DANGER 등급 글자
- **color.grade.safe.border**: `#bfebd2` — SAFE 등급 테두리
- **color.grade.safe.surface**: `#eaf8f0` — SAFE 등급 배경
- **color.grade.safe.text**: `#195f3b` — SAFE 등급 글자
- **color.notice.error.surface**: `#fff1f1` — 오류 상자 배경
- **color.notice.error.text**: `#a72126` — 오류 상자·입력 오류 글자
- **color.notice.info.surface**: `#eff5ff` — 안내 상자 배경
- **color.notice.info.text**: `#28446e` — 안내 상자 글자
- **color.notice.warning.surface**: `#fff5df` — 주의 상자 배경
- **color.notice.warning.text**: `#75480e` — 주의 상자 글자
- **color.schedule.important**: `#bd3636` — important 일정 왼쪽 4px 강조선
- **color.surface.card**: `#ffffff` — 카드·입력칸·헤더 배경
- **color.surface.clause**: `#f2f5fa` — 특약 문구 상자
- **color.surface.page**: `#f7f9fd` — 페이지 배경
- **color.surface.tint**: `#edf4ff` — 보조 버튼 hover, 기본 점수 카드 배경
- **color.text.brand**: `#17263d` — 헤더 서비스 이름
- **color.text.muted**: `#52637b` — 보조 설명 글자 (--muted)
- **color.text.strong**: `#16243b` — 본문·제목 기본 글자색 (:root color)
- **motion.button**: `0.15s` — 버튼 배경 전환. prefers-reduced-motion: no-preference일 때만
- **radius.button**: `12px` — 버튼
- **radius.card**: `18px` — 카드
- **radius.hero**: `32px` — 첫 화면 히어로
- **radius.input**: `9px` — 입력칸
- **radius.notice**: `10px` — 안내 상자·선택지·특약 상자
- **radius.pill**: `99px` — 배지
- **shadow.card**: `0 3px 13px #28497906` — 카드 그림자 (거의 안 보이는 수준)
- **size.content**: `820px` — 결과·일정표 본문 폭
- **size.control**: `46px` — 버튼·입력칸 최소 높이
- **size.control.compact**: `36px` — 작은 버튼 최소 높이
- **size.frame**: `1120px` — 본문 최대 폭 (좌우 여백 24px, 720px 이하 14px)
- **size.narrow**: `620px` — 진단·로그인 폼 폭
- **space.card**: `20px` — 카드 안쪽 여백
- **space.panel**: `clamp(20px, 5vw, 38px)` — 폼 패널 안쪽 여백
- **space.stack**: `12px` — 카드 목록 간격

### Contrast pairs

- color.text.strong on color.surface.page: minimum 4.5:1
- color.text.muted on color.surface.card: minimum 4.5:1
- color.text.muted on color.surface.page: minimum 4.5:1
- color.action.on-primary on color.action.primary: minimum 4.5:1
- color.grade.safe.text on color.grade.safe.surface: minimum 4.5:1
- color.grade.caution.text on color.grade.caution.surface: minimum 4.5:1
- color.grade.danger.text on color.grade.danger.surface: minimum 4.5:1
- color.notice.error.text on color.notice.error.surface: minimum 4.5:1
- color.focus.ring on color.surface.card: minimum 3:1

### Reduced motion

Required.

### Foundation rules

- 주 버튼 파랑(color.action.primary)은 화면당 다음 행동 1개에만 쓴다

- 등급 색은 SAFE/CAUTION/DANGER 결과와 기록 배지에만 쓰고 장식에 쓰지 않는다

- 새 색은 이 토큰에 먼저 추가하고 쓴다 (CSS에 새 hex를 바로 넣지 않는다)

- 입력칸 테두리 #aebed4는 흰 배경 대비 1.89:1로 WCAG 비텍스트 대비 3:1보다 낮다. 바꿀 때는 팀 합의 후 이 토큰부터 고친다
<!-- design-md:claim-end -->

<!-- design-md:section typography-assets -->
## 3. Typography & Assets

### Type roles

| Role | Usage | Family | Size | Weight | Line height | Tracking |
|---|---|---|---|---|---|---|
| display | 첫 화면 히어로 제목 | Pretendard, Malgun Gothic, system-ui, sans-serif | clamp(2.15rem, 4vw, 3.25rem) | 850 | 1.22 | -0.06em |
| h1 | 화면 제목 | Pretendard, Malgun Gothic, system-ui, sans-serif | clamp(1.9rem, 4vw, 3.1rem); 패널 안 clamp(1.65rem, 4vw, 2.35rem) | 850 | 1.22 | -0.06em |
| h2 | 섹션 제목 | Pretendard, Malgun Gothic, system-ui, sans-serif | 1.45rem | 800 |  | -0.04em |
| h3 | 카드 제목 | Pretendard, Malgun Gothic, system-ui, sans-serif | 1.08rem | 800 |  | -0.025em |
| body | 본문 | Pretendard, Malgun Gothic, system-ui, sans-serif | 16px | 400 | 1.55 |  |
| eyebrow | 섹션 위 작은 라벨 |  | 0.83rem | 850 |  | 0.06em |
| score | 결과 점수 숫자 |  | 3rem (720px 이하 2.1rem) | 900 | 1 |  |
| button | 버튼 글자 |  | 1rem (compact 0.88rem) | 760 |  |  |

### Rules

- Pretendard는 font-family에 이름만 있고 웹폰트를 불러오지 않는다. 지금 화면은 Malgun Gothic 또는 시스템 글꼴로 보인다

- 한글 제목·본문은 word-break: keep-all과 overflow-wrap: anywhere를 함께 쓴다

- 굵기는 400(본문)·750~800(라벨·제목)·850~900(큰 제목·점수)만 쓴다

- 확인되지 않은 대체 글꼴을 프로젝트 글꼴처럼 표기하지 않는다

<!-- design-md:section components-states -->
## 4. Components & States

### Component: button

**Semantics:** 화면의 다음 행동을 실행한다. primary는 화면당 1개, secondary는 대안, quiet는 헤더 메뉴·뒤로가기.

- Anatomy: label, optional trailing arrow
- Variants: primary, secondary, quiet, compact, full, text-button
- States: default, hover, focus-visible, disabled, loading
- Token references: color.action.primary, color.action.primary.hover, color.action.on-primary, color.action.secondary.text, color.border.control, color.surface.tint, color.action.quiet.text, color.focus.ring, radius.button, size.control, size.control.compact, motion.button

- Interaction kind: interactive

#### State applicability

| State | Applicability | Reason |
|---|---|---|
| default | applicable |  |
| hover | applicable |  |
| focus-visible | applicable |  |
| disabled | applicable |  |
| loading | applicable | 요청 중에는 disabled + cursor: wait + opacity 0.65로 중복 제출을 막는다 |
| error | not-applicable | 오류는 버튼이 아니라 notice.error와 입력칸 아래 문구로 보여준다 |
| success | not-applicable | 성공하면 다음 화면으로 이동한다 |

### Component: field

**Semantics:** 금액(만원 단위)·날짜·주소를 입력받는다. 오류는 aria-invalid와 칸 아래 role=alert 문구로 알린다.

- Anatomy: label, hint, input, unit (만원 등), error message
- States: default, focus-visible, error
- Token references: color.border.input, color.surface.card, color.notice.error.text, color.focus.ring, radius.input, size.control

- Interaction kind: interactive

#### State applicability

| State | Applicability | Reason |
|---|---|---|
| default | applicable |  |
| hover | not-applicable | 입력칸 hover 스타일이 없다 |
| focus-visible | applicable |  |
| disabled | not-applicable | 비활성 입력칸을 쓰는 화면이 없다 |
| loading | not-applicable | 입력칸 자체는 불러오는 상태가 없다 |
| error | applicable |  |
| success | not-applicable | 성공 표시 없이 다음 단계로 간다 |

### Component: choice

**Semantics:** 등기부 질문의 YES/NO/UNKNOWN 3지선다. 모르겠어요는 위험 쪽으로 계산된다는 안내와 함께 쓴다.

- Anatomy: radio, label
- Variants: 예, 아니오, 모르겠어요
- States: default, focus-visible, selected
- Token references: color.border.control, color.choice.selected.surface, color.choice.selected.border, color.choice.selected.text, radius.notice

- Interaction kind: interactive

#### State applicability

| State | Applicability | Reason |
|---|---|---|
| default | applicable |  |
| hover | not-applicable | 선택지 hover 스타일이 없다 |
| focus-visible | applicable |  |
| disabled | not-applicable | 비활성 선택지가 없다 |
| loading | not-applicable | 선택은 즉시 반영된다 |
| error | not-applicable | 답을 고르기 전에는 다음 버튼을 막는다 |
| success | not-applicable | 선택된 답은 성공 상태가 아니라 selected 상태로 강조한다 (:has(:checked)) |

### Component: card

**Semantics:** 한 가지 정보 묶음(용어 풀이, 위험 항목, 일정 1건, 진단 기록 1건)을 담는다.

- Anatomy: container, title, body, optional action
- Variants: panel, action-card, term card, task, history-item
- States: default
- Token references: color.surface.card, color.border.default, radius.card, shadow.card, space.card

- Interaction kind: non-interactive
- Interaction reason: 카드 자체는 누르지 않고 안의 버튼·링크만 누른다

### Component: grade-card

**Semantics:** S3 결과 맨 위. 등급 글자·점수·색을 함께 보여준다 (SAFE 0~29, CAUTION 30~59, DANGER 60 이상).

- Anatomy: badge, grade message, score number, score caption
- Variants: safe, caution, danger
- States: default
- Token references: color.grade.safe.surface, color.grade.safe.border, color.grade.safe.text, color.grade.caution.surface, color.grade.caution.border, color.grade.caution.text, color.grade.danger.surface, color.grade.danger.border, color.grade.danger.text

- Interaction kind: non-interactive
- Interaction reason: 결과를 읽기만 한다

### Component: notice

**Semantics:** 안내·주의·오류 문장. error는 role=alert로 읽어 준다. 면책 문구도 이 상자를 쓴다.

- Anatomy: text
- Variants: info, warning, error
- States: default
- Token references: color.notice.info.surface, color.notice.info.text, color.notice.warning.surface, color.notice.warning.text, color.notice.error.surface, color.notice.error.text, radius.notice

- Interaction kind: non-interactive
- Interaction reason: 읽기 전용 안내

### Component: clause

**Semantics:** 계약서 특약 문구와 복사 버튼. 문구가 null이면 계약 보류를 권하는 문장을 보여준다.

- Anatomy: clause text, copy button, copied caption
- States: default, hover, focus-visible, success
- Token references: color.surface.clause, radius.notice, color.text.muted

- Interaction kind: interactive

#### State applicability

| State | Applicability | Reason |
|---|---|---|
| default | applicable |  |
| hover | applicable |  |
| focus-visible | applicable |  |
| disabled | not-applicable | 복사 버튼은 항상 누를 수 있다 |
| loading | not-applicable | 복사는 즉시 끝난다 |
| error | not-applicable | 복사 실패 안내가 아직 없다 |
| success | applicable | 복사 후 옆에 작은 복사됨 확인 문구를 보여준다 |

### Component: schedule-task

**Semantics:** S4 일정 1건. important(잔금 전 등기부 재확인, 당일 전입신고)는 빨간 왼쪽선과 배지로 강조한다.

- Anatomy: D-day, date, title, reason, external link, important badge
- Variants: default, important
- States: default
- Token references: color.schedule.important, color.text.muted, radius.card

- Interaction kind: non-interactive
- Interaction reason: 안의 외부 링크만 누른다

### Rules

- 모든 버튼·링크·입력칸은 focus-visible에 3px color.focus.ring 외곽선과 3px offset을 쓴다

- 요청 중인 버튼은 disabled로 바꿔 중복 제출을 막는다

- 등급·중요 표시는 색 + 글자(배지) 두 가지로 전달한다

<!-- design-md:section layout-platforms -->
## 5. Layout & Platforms

### Responsive constraints

- Minimum supported width: 320px
- Reflow target: 200% zoom

### Layout rules

- 본문은 .frame (최대 1120px, 좌우 24px, 720px 이하 14px) 안에 둔다

- 폼 화면은 620px, 결과·일정표는 820px 폭 가운데 정렬

- 720px 이하에서 히어로·날짜 입력은 1열, 용어 카드는 2열로 바꾼다

- 380px 이하에서 점수 카드는 세로로 쌓는다

- 누르는 요소는 높이 44px 이상을 지킨다

### Platform: web

- 모바일 브라우저와 데스크톱 브라우저를 같은 화면으로 지원한다 (발표는 데스크톱)

<!-- design-md:section content-locales -->
## 6. Content & Locales

### Voice

- 해요체로 쓴다 (~해요, ~있어요)

- 한 문장에 한 가지만 말한다

- 겁주기보다 다음에 할 일을 알려준다

- 모르는 건 모른다고 하고 중개사·전문가 확인을 권한다

### Terminology

| Term | Preferred form |
|---|---|
| 갑구 | 주인 이야기 (소유권, 압류 등) |
| 건축물대장 | 건물의 건강검진표. 위반건축물 여부가 나와요 |
| 등기부등본 | 집의 이력서. 누가 주인인지, 집에 빚이 걸려 있는지 적혀 있어요 |
| 면책 문구 | 참고용이며 법적 판단이 아닙니다. 계약 전 공인중개사·전문가에게 확인하세요. |
| 모르겠어요 | 위험한 답으로 계산하고 중개사 확인을 안내해요 |
| 빨간 줄 | 이미 지워진(말소된) 기록 |
| 을구 | 빚 이야기 (근저당권, 다른 세입자 권리) |

### Locale: ko (supported)

- html lang=ko
- 금액은 만원 단위 정수로 받고 화면에는 단위를 붙인다
- 날짜는 YYYY-MM-DD

<!-- design-md:section governance -->
## 7. Governance

<!-- design-md:claim authority kind=project-system lang=en -->
### Authority

This document is the project design contract for the declared scope.
<!-- design-md:claim-end -->

<!-- design-md:claim application-priority order=prompt-fact,repository-fact,system-contract,reference-inspiration lang=en -->
### Application priority

1. Direct user instructions for the requested scope.
2. Repository facts.
3. This system contract.
4. Reference inspiration.
<!-- design-md:claim-end -->

<!-- design-md:claim unknowns policy=absent-at-smallest-unresolved-boundary lang=en -->
### Unknowns

Omit only the smallest unresolved value or group. Do not replace it with a plausible default.
<!-- design-md:claim-end -->

<!-- design-md:claim changes policy=review-record-validate-before-adoption lang=en -->
### Changes

Record, review, and validate changes before adoption.
<!-- design-md:claim-end -->

### Project priority details

1. 사용자(팀장) 직접 지시

2. docs/ 명세 (원본은 BackEnd 저장소)

3. 지금 코드의 스타일 (src/styles, components, pages CSS)

4. 이 DESIGN.md

5. toss 레퍼런스 (톤 참고만)

### Additional change rules

- CSS를 바꾸면 이 문서의 토큰도 같이 고친다

- docs/와 다르면 docs/가 맞다

### Decision provenance

- /typography_assets/assets/0 — unresolved; evidence: index.html
