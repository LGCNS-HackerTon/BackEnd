# DB 실행 방법 (MySQL 8.0.16 이상 기준, MariaDB 10.5 이상도 가능)

1. `BackEnd` 폴더에서 터미널(PowerShell·CMD 모두 가능)을 열고 `mysql -u root -p --default-character-set=utf8mb4 -e "source db/schema.sql"` 실행 → `jeonse` DB(없으면 생성)에 테이블 5개 생성 (**회원·진단을 포함한 앱 테이블 5개의 기존 데이터는 지워짐**, 실거래가 테이블 등 다른 테이블은 그대로)
2. 이어서 seed 실행 (질문 10개, 할 일 16개, 여러 번 실행해도 중복 안 됨)
   - CMD · Mac · Linux: `mysql -u root -p --default-character-set=utf8mb4 jeonse < db/seed.sql`
   - PowerShell: `cmd /c 'mysql -u root -p --default-character-set=utf8mb4 jeonse < db/seed.sql'`
   - seed는 오류 시 트랜잭션을 롤백할 수 있도록 배치 입력으로 실행하고, `--force`를 붙이지 않는다. 실행 중에는 질문·할 일에 대한 다른 쓰기 작업을 중지한다.
3. 확인: `mysql -u root -p jeonse -e "SELECT COUNT(*) FROM registry_questions; SELECT COUNT(*) FROM checklist_items;"` → 10, 16

> - `mysql`을 찾을 수 없다고 나오면 MySQL 설치 폴더의 `bin`(예: `C:\Program Files\MySQL\MySQL Server 8.0\bin`)을 PATH에 추가하거나, 그 폴더의 전체 경로로 `mysql.exe`를 실행한다.
> - PowerShell에서는 `mysql ... < db/schema.sql`처럼 `<`를 쓰면 오류가 나서 `-e "source ..."` 방식을 쓴다.
> - MySQL Workbench를 쓰면 `schema.sql` → `seed.sql` 순서로 파일을 열고 ⚡(Execute) 버튼을 누른다.
> - BE의 `.env`: `DB_URL=jdbc:mysql://localhost:3306/jeonse`, `DB_USERNAME`, `DB_PASSWORD`
