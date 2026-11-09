# 전세역전 백엔드

팀 공감 · Spring Boot 3 (Java 17) · Spring Data JPA · MySQL 8 (로컬 기본은 H2)

## 실행

Java 17 이상에서 `./gradlew bootRun`으로 실행합니다 (Windows는 `gradlew.bat bootRun`). Gradle은 따로 설치하지 않아도 됩니다.
환경 변수를 설정하지 않으면 데모용 H2 메모리 DB와 문서 기반 seed 데이터로 실행됩니다.
MySQL 사용 시 `.env.example`을 참고해 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 설정하세요.

서버 주소는 `http://localhost:8080`, 프론트(`http://localhost:5173`) 요청은 CORS로 허용합니다.

## 테스트

```bash
./gradlew test
```

PR마다 CI가 `./gradlew build`(테스트 포함)를 실행합니다. CI는 MySQL 8 컨테이너를 띄우고 `db/schema.sql`, `db/seed.sql`이 있으면 먼저 불러옵니다.
데모 케이스 A/B/C(0·70·40점, docs/01 5절)는 테스트로 지킵니다.

## 환경 변수

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `DB_URL` | | 예: `jdbc:mysql://localhost:3306/jeonse`. 없으면 H2 |
| `DB_USERNAME`, `DB_PASSWORD` | | MySQL 계정 |
| `CLOVA_OCR_URL`, `CLOVA_OCR_SECRET` | | Naver CLOVA OCR 호출 정보. 없으면 OCR API는 422 `OCR_UNREADABLE`로 실패하고 화면은 직접 답하기로 진행합니다 |
| `JWT_SECRET` | | 회원 인증 JWT 서명 키 |

실제 값은 `.env`에만 두고 커밋하지 않습니다.

## API

정확한 요청·응답은 [API 명세서](docs/03-api-contract.md)가 기준입니다.

| No | 메서드 | 경로 | 내용 | 순위 | 담당 | 패키지 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | GET | `/api/questions` | 등기부 질문 10개 | P0 | 문한일 | `question` |
| 2 | POST | `/api/diagnoses` | 규칙 기반 점수·등급 + 위험 항목 안내 | P0 | 송석주, 최다인 | `diagnosis`, `diagnosis.advice` |
| 3 | GET | `/api/diagnoses/{id}` | 진단 다시 보기 | P2 | 송석주 | `diagnosis` |
| 4 | GET | `/api/schedule` | 계약 일정표 14개 | P0 | 문한일 | `schedule` |
| 5 | POST | `/api/ocr/registry` | 주소 + 서류로 질문별 확인 여부·답 (multipart) | P1 | 최다인 | `ocr` |

공통 에러, 예외 처리, CORS는 `common` 패키지(문한일)에 둡니다.

## 백엔드에서 꼭 지킬 것

1. **점수·등급은 규칙 기반으로만** 계산합니다 (docs/01). AI·OCR은 점수를 정하지 않습니다
2. 모든 에러는 `{"error": {"code", "message", "field"}}` 형식입니다 (`common/ApiError`)
3. 금액은 **만원 단위 정수**, 날짜는 `YYYY-MM-DD`
4. **OCR 서류와 주소는 저장하지 않습니다.** 파일을 디스크·DB에 쓰지 않고, 로그에 소유자 이름·주소·파일 내용을 남기지 않습니다 (docs/08 NFR-06)
5. OCR이 판독하지 못한 질문은 `NO`가 아니라 `UNCONFIRMED`로 돌려줍니다
6. 업로드는 파일당 10MB까지 받습니다. Spring 기본값은 1MB라서 `spring.servlet.multipart.max-file-size`, `max-request-size`를 설정해야 합니다

## 문서

- 처음이면: [docs/AGENTS.md](docs/AGENTS.md) (프로젝트 공통 규칙, AI에게도 이 파일을 줍니다)
- 강사님 피드백 반영(OCR): [docs/09-feedback-ocr.md](docs/09-feedback-ocr.md)
- 점수 규칙 [docs/01](docs/01-registry-questions.md) · 일정 [docs/02](docs/02-schedule-rules.md) · API [docs/03](docs/03-api-contract.md) · DB [docs/04](docs/04-db-schema.md) · 역할 [docs/05](docs/05-roles.md) · 화면 흐름 [docs/06](docs/06-service-flow.md) · Git 규칙 [docs/07](docs/07-git-rules.md) · 요구사항 [docs/08](docs/08-requirements.md) · [결정 기록](docs/decision-log.md)
