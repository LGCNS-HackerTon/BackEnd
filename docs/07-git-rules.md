# GitHub 협업 규칙

> 하루 반짜리 해커톤이라 규칙은 최소한으로만 정했어. 대신 **아래 규칙은 전원 지킨다.**
> Organization: https://github.com/LGCNS-HackerTon · 저장소: [BackEnd](https://github.com/LGCNS-HackerTon/BackEnd), [FrontEnd](https://github.com/LGCNS-HackerTon/FrontEnd)

## 1. 한눈에 보기

```
이슈 만들기 → 브랜치 따기 → 작게 커밋 → PR 올리기 → 팀장이 머지 → main pull
  #3          be1/3-score    [BE1] ...     Closes #3     Squash and merge
```

| 규칙 | 한 줄 |
|---|---|
| main | **직접 push 금지.** 무조건 PR로만 들어간다. GitHub 설정으로 막혀 있어서 push하면 거절돼 (팀장만 문서 동기화용으로 예외) |
| 이슈 | 작업 하나 = 이슈 하나. 템플릿으로 만든다 |
| 브랜치 | `역할/이슈번호-짧은영어` (예: `be1/3-diagnosis-score`) |
| 커밋 | `[역할] 한 일` (예: `[BE1] 부채비율 계산 추가`) |
| PR | **1시간에 한 번 이상**, 템플릿 채우기, `Closes #이슈번호` |
| 머지 | 팀장이 **Squash and merge** |
| 마감 | 16:00 통합 전 **15:50까지 PR** 올리기 |

## 2. 처음 한 번만 (13:00 전에)

```bash
git config --global user.name "내 GitHub 아이디"
```

```bash
git config --global user.email "내 GitHub 계정 이메일"
```

이메일이 GitHub 계정 이메일과 다르면 **기여자 목록에 내 이름이 안 나와.** GitHub → Settings → Emails에 있는 주소를 써.

```bash
git clone https://github.com/LGCNS-HackerTon/BackEnd.git
```

FE는 `FrontEnd.git`으로 바꿔서 clone.

## 3. 이슈

### 언제 만드나
| 상황 | 템플릿 | 라벨 |
|---|---|---|
| 기능 하나 만들 때 (체크리스트 묶음 단위) | 기능 개발 | `enhancement` |
| 버그 찾았을 때 (주로 QA) | 버그 신고 | `bug` |
| API·DB 모양을 바꾸고 싶을 때 | 계약 변경 요청 | `documentation` |

- 제목: `[역할] 할 일` (예: `[BE1] 진단 점수 계산`, `[FE] 결과 화면`)
- **Assignees에 나 자신**을 넣는다
- 크기: 1~2시간 안에 끝나는 크기. `docs/05-roles.md` 4절 체크리스트를 2~3개 이슈로 나누면 적당해
- 계약 변경 요청은 **올리고 단톡에도 링크**. 관련 역할이 확인하기 전에는 코드 먼저 바꾸지 않는다

## 4. 브랜치

```bash
git switch main
```

```bash
git pull
```

```bash
git switch -c be1/3-diagnosis-score
```

- 이름: `역할/이슈번호-짧은영어` → `fe/5-question-page`, `db/2-seed`, `qa/9-scenario`
- **항상 최신 main에서** 딴다
- 브랜치 하나 = 이슈 하나. PR이 머지되면 그 브랜치는 끝이고, 다음 일은 새 브랜치

## 5. 커밋

- 형식: `[역할] 한 일` (한국어, 무엇을 했는지만)
  - `[BE1] 부채비율 계산 추가`
  - `[FE] 특약 복사 버튼 추가`
  - `[DB] 질문 10개 seed 추가`
  - `[QA] 진단 테스트 시나리오 추가`
- **작게 자주.** 기능 하나가 돌아가면 바로 커밋
- 올리면 안 되는 것: `.env`(API 키, DB 비밀번호), `node_modules/`, `build/`, `target/`, IDE 설정 폴더. BE3와 FE 드라이버가 뼈대에 `.gitignore`를 넣는다

### 커밋·PR에 자동 서명을 남기지 않는다
AI 도구가 커밋 메시지나 PR 본문 끝에 `Co-Authored-By: ...`, `Generated with ...` 같은 문구를 자동으로 붙이는 경우가 있어. **이런 문구는 지우고 올린다.** 기여자 목록과 코드에는 팀원 이름만 남긴다.

push 전에 최근 커밋 메시지 확인:

```bash
git log -3 --format=%B
```

이미 붙었으면 push 전에 마지막 커밋 메시지 고치기:

```bash
git commit --amend
```

코드 주석에도 "자동 생성됨" 같은 문구는 남기지 않는다.

## 6. PR (Pull Request)

```bash
git push -u origin be1/3-diagnosis-score
```

push하면 GitHub에 **Compare & pull request** 버튼이 뜬다. 눌러서 템플릿을 채운다.

| 항목 | 규칙 |
|---|---|
| 방향 | 내 브랜치 → `main` |
| 제목 | 이슈 제목 그대로 (`[BE1] 진단 점수 계산`) |
| 본문 | 템플릿 체크리스트 채우기, `Closes #3` (머지되면 이슈가 자동으로 닫힘) |
| Reviewers | 같은 영역 1명 (BE끼리 / FE는 화면 담당 리뷰어) |
| 머지 | **팀장이** Squash and merge. 오늘은 리뷰보다 속도라 훑어보고 문제없으면 바로 |
| 크기 | 파일 10개 넘으면 쪼개기 |

### PR에 자동으로 달리는 것
| 이름 | 무엇 | 어떻게 대응 |
|---|---|---|
| **CI** (GitHub Actions) | BackEnd: Gradle 빌드 + 테스트 (MySQL 포함) / FrontEnd: 설치 + lint + 빌드 | ❌ 빨간불이면 **고치고 다시 push.** 로그는 PR 아래 Details |
| **CodeRabbit** | PR마다 자동 코드 리뷰 댓글 (한국어) | 참고용. 반영할지는 본인이 판단. 질문은 댓글에 `@coderabbitai` 붙여서 |
| **디스코드 알림** | PR·이슈·push가 팀 채널에 올라옴 | 내 PR이 머지됐는지 여기서 확인 |

- CodeRabbit의 **"커밋 제안 적용", `@coderabbitai generate ...` 기능은 쓰지 않는다.** 봇 이름이 커밋·기여자에 남아. 고칠 내용은 직접 고쳐서 커밋한다

### 머지된 다음
```bash
git switch main
```

```bash
git pull
```

다른 사람 PR이 머지되면 **나도 main을 pull**해서 내 브랜치에 반영한다 (아래 7절).

## 7. 충돌이 났을 때

PR에 "This branch has conflicts"가 뜨거나 push가 거절되면:

```bash
git pull origin main
```

1. 충돌난 파일을 열면 `<<<<<<<`, `=======`, `>>>>>>>` 표시가 있다
2. 남길 코드만 남기고 표시를 지운다. **내 폴더가 아닌 파일이면 그 담당자를 부른다**
3. 저장 후 커밋, 다시 push

```bash
git add .
```

```bash
git commit -m "[BE1] main 충돌 해결"
```

```bash
git push
```

15분 넘게 안 풀리면 팀장 호출.

## 8. 문서(docs/, docs/AGENTS.md) 바꾸기

1. **계약 변경 요청** 이슈를 올리고 단톡에 공유
2. 관련 역할이 OK하면 **BackEnd 저장소**에서 docs를 고치는 PR
3. 머지되면 **팀장이 FrontEnd에도 똑같이** 반영
4. FrontEnd 저장소에서 docs를 직접 고치지 않는다

## 9. 자주 쓰는 명령

| 하고 싶은 것 | 명령 |
|---|---|
| 지금 상태 보기 | `git status` |
| 지금 브랜치 보기 | `git branch` |
| 바뀐 내용 보기 | `git diff` |
| 전부 커밋 준비 | `git add .` |
| 커밋 | `git commit -m "[역할] 한 일"` |
| 최근 커밋 보기 | `git log --oneline -5` |
| 실수로 main에서 작업했을 때 (커밋 전) | `git switch -c 역할/번호-이름` → 바뀐 내용이 새 브랜치로 따라온다 |

## 10. 오늘 시간 기준

| 시각 | Git으로 할 일 |
|---|---|
| 13:00 | 전원 clone, 내 첫 이슈·브랜치 만들기 |
| 13:40 | BE3·FE·DB 뼈대 PR 머지 → **전원 main pull** |
| 매시 정각 | 각자 PR 1개 이상, 팀장은 PR 대기 목록 비우기 |
| 15:50 | 오늘 통합할 PR 전부 올리기 |
| 16:00 | 팀장이 머지 → 전원 main pull → 통합 |
| 16:30 | 오늘 동결. 이후 버그 PR만 |
