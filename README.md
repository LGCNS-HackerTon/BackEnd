# 전세역전 백엔드

공감 팀의 첫 전세 계약 가이드 프로젝트입니다.

Java 17 이상에서 `./gradlew bootRun`으로 실행합니다 (Windows는 `gradlew.bat bootRun`). Gradle은 따로 설치하지 않아도 됩니다.
환경 변수를 설정하지 않으면 데모용 H2 메모리 DB와 문서 기반 seed 데이터로 실행됩니다.
MySQL 사용 시 `.env.example`을 참고해 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 설정하세요.
