# A.chat

JavaScript 기반 React 프론트엔드와 Spring Boot 3.5 / Java 21 / Spring Data JPA / MySQL 백엔드입니다.

증강 27종의 동작, 구간별 선택 규칙, 동시성 처리, API·저장 변경과 테스트는 [증강 시스템 문서](docs/augment-system.md)에 정리했습니다.

## 프론트엔드

기존 `achat-front`, `achat-front-backup-web`, `achat-mobile` 및 루트의 `test.html`을 삭제하고 `achat-front/`에 새 반응형 웹 앱을 만들었습니다. 새 폴더는 서브모듈이 아닌 일반 소스 폴더입니다. TypeScript를 사용하지 않으며 모든 앱 코드는 `.js`와 `.jsx`입니다.

Node.js 22.12 이상이 필요합니다. 저장소 루트에서 실행합니다.

```powershell
npm ci
npm run dev
```

개발 화면은 `http://127.0.0.1:5173`에서 열립니다. 아래 방법으로 백엔드도 별도 터미널에서 실행해주세요. 기본적으로 `/backend` 요청과 WebSocket 연결을 `http://localhost:8080`으로 전달합니다. 다른 주소를 사용하려면 `achat-front/.env.example`을 `achat-front/.env.local`로 복사하고 `BACKEND_URL`을 변경합니다.

백엔드 없이 화면만 확인하려면 `npm run dev:ui`를 실행하고 **로그인 없이 화면 둘러보기**를 선택합니다. 이 개발 모드는 샘플 데이터를 사용하고 실제 REST·WebSocket 요청을 보내지 않습니다.

미리보기 서버가 실행 중일 때 별도 터미널에서 `npm run test:ui`로 Playwright PC·모바일 화면 검사를 재실행할 수 있습니다. 검사 방법과 스크린샷 위치는 [프론트엔드 검증 문서](achat-front/README.md#검증)에 정리했습니다.

```powershell
npm run lint
npm run test
npm run build
npm run preview
```

빌드 결과는 `achat-front/dist/`입니다. `preview`는 빌드 결과 확인용이며 운영 API 프록시를 제공하지 않습니다. 화면 구성, API 연결과 운영 설정은 [프론트엔드 문서](achat-front/README.md)를 참고하세요.

## 백엔드 실행

### Docker MySQL에 연결하기

MySQL만 Docker로 실행하고 백엔드는 IDE 또는 Gradle로 실행한다면, DB 주소에는 **호스트에 공개한 포트**를 사용합니다. 예를 들어 `3307:3306`이면 `127.0.0.1:3307`로 연결합니다.

```powershell
docker ps --format "table {{.Names}}\t{{.Ports}}\t{{.Status}}"
Copy-Item .env.example .env
```

루트 `.env`의 `DB_URL`을 실제 포트와 DB 이름에 맞추고, `DB_USERNAME`과 `DB_PASSWORD`를 컨테이너 생성 시 지정한 계정으로 설정합니다. `MYSQL_ROOT_PASSWORD`만 지정했다면 계정은 `root`입니다. 이미 `.env`가 있으면 복사하지 말고 필요한 값만 수정합니다.

백엔드는 [Spring Boot의 외부 설정 가져오기](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html#importing-extensionless-files)를 통해 `.env`를 자동으로 읽습니다. 이 파일은 Java properties 형식이므로 값에 따옴표를 붙이지 않습니다. 비밀번호에 역슬래시(`\`)가 있으면 `\\`로 적습니다. `.env`는 Git에서 제외됩니다. 실행 작업 디렉터리는 저장소 루트로 지정하세요. 기존 `DB_*` 환경 변수가 있으면 `.env`보다 우선하므로 오래된 값도 확인합니다.

```powershell
.\gradlew.bat bootRun
```

현재 개발 컨테이너 `achat-mysql`이 중지되어 있다면 `docker start achat-mysql`로 시작합니다. 백엔드도 별도 컨테이너에 넣는 경우 DB 주소는 같은 Docker 네트워크의 MySQL 서비스 이름과 컨테이너 포트 `3306`을 사용합니다.

`8080` 포트를 다른 프로그램이 사용 중이면 루트 `.env`에 `SERVER_PORT=8081`을 설정합니다. 프론트엔드도 `achat-front/.env.local`에 `BACKEND_URL=http://127.0.0.1:8081`을 설정한 뒤 다시 실행해야 같은 백엔드에 연결됩니다.

### 직접 접속 정보 지정하기

Lombok은 Spring Boot의 의존성 관리 버전을 사용하며, 메인·테스트 소스 모두 `compileOnly`와 `annotationProcessor` 설정으로 적용합니다. 설정 방식은 [Lombok Gradle 문서](https://projectlombok.org/setup/gradle)를 따릅니다.

단순 의존성 주입 생성자는 `@RequiredArgsConstructor`, 접근자는 `@Getter`와 필요한 필드의 `@Setter`, 로거는 `@Slf4j`를 사용합니다. JPA 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 유지하고, 초기화·검증·방어적 복사 로직과 설정값 주입 생성자는 직접 구현합니다.

MySQL에서 데이터베이스를 먼저 생성합니다. 테이블은 애플리케이션이 JPA 매핑으로 생성합니다.

```sql
CREATE DATABASE IF NOT EXISTS achat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

PowerShell에서 DB 접속 정보를 지정한 뒤 실행합니다.

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/achat?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
$env:DB_USERNAME = '사용할 DB 계정'
$env:DB_PASSWORD = '해당 계정의 비밀번호'
.\gradlew.bat bootRun
```

설정 위치는 `src/main/resources/application.yml`입니다. `.env`와 환경 변수가 없을 때 `DB_URL` 기본값은 위 주소이고, `DB_USERNAME` 기본값은 `root`입니다. `DB_DDL_AUTO` 기본값은 `update`이며, 이미 준비된 스키마만 검증하려면 `validate`로 지정할 수 있습니다.

## 저장 범위

회원, 재화·경험치·랭크·직업, 채팅방과 멤버, 메시지와 좋아요, 피드·게시판·미니홈 글과 댓글, 친구 관계·요청·초대, 홈 꾸미기, 보유 아이템·장착 상태, 카드 뽑기 기록, 게임·시즌 기록, 신고·제재·상호작용·방 랭킹을 DB에 저장합니다.

회원가입 후 이메일 인증 없이 바로 로그인할 수 있습니다. 기존 미인증 계정도 동일하게 로그인할 수 있으며, `users.email_verified` 컬럼은 기존 DB 스키마와의 호환용으로만 유지하고 API 응답이나 로그인 판단에는 사용하지 않습니다.

현재 가입·로그인 요청 제한과 비밀번호 재설정은 미구현입니다. 기본 관리자 계정 등 추가 발견 사항, 격리 환경 재현 결과와 보완 우선순위는 [인증 보안 점검](docs/auth-security-audit.md)에 정리했습니다.

게시글 일일 작성 횟수와 마지막 작성 시각도 DB에 저장하여 재시작이나 글 삭제 후에도 작성 제한이 유지됩니다. 구매 시 재화 차감과 아이템 지급, 게시글·댓글·좋아요 변경은 트랜잭션으로 처리합니다. 변경 충돌과 중복 데이터는 HTTP 409로 응답합니다.

접속 상태, 매칭 대기열, 메시지 쿨타임, 진행 중 라운드와 증강 선택 옵션은 임시 상태로 메모리에 유지합니다. 메시지 효과·공개 시각·증강 보상 영수증·퀘스트 진행은 DB에 저장합니다. 상점 상품 목록은 코드에 정의된 카탈로그입니다. 게시판의 임의 샘플 글은 자동 생성하지 않습니다.

기존 저장 방식은 서버 메모리에만 데이터를 보관했으므로, 종료된 서버의 데이터를 읽어 이관하는 기능은 없습니다. JPA 전환 이후 생성하는 영구 데이터는 재시작 후에도 유지됩니다.

## 테스트

```powershell
.\gradlew.bat test
```

기본 테스트는 별도 DB 설치 없이 H2의 MySQL 호환 모드를 사용합니다. 서비스 호출 간 저장 여부, 구매 롤백, 중복 방지, 댓글·좋아요 삭제, 프로필 변경, 시즌 유지 및 실제 애플리케이션 종료·재기동 후 데이터 유지 여부를 검증합니다. 모델 테스트는 JSON 속성명, 컬렉션의 방어적 복사, 가구 좌표 범위 제한도 확인합니다.

MySQL에서도 같은 통합 테스트를 실행할 수 있습니다. 테스트 스키마는 `create-drop`으로 생성·삭제되므로 전용 테스트 DB를 사용합니다.

```sql
CREATE DATABASE achat_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```powershell
$env:TEST_DB_URL = 'jdbc:mysql://localhost:3306/achat_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
$env:TEST_DB_USERNAME = '테스트 DB 계정'
$env:TEST_DB_PASSWORD = '해당 계정의 비밀번호'
$env:TEST_DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
.\gradlew.bat test --rerun-tasks
```

재기동 검증 테스트는 독립적인 임시 H2 파일 DB를 사용합니다.

## 공개 배포 보안

운영 준비, 환경변수, JWT 회전·폐기, WebSocket 방별 인가, 남은 제약과 수동 배포·롤백은
[보안 강화 및 배포 안내](docs/security-deployment.md)를 참조하세요.

`compose.prod.yml`은 AChat 전용 프로젝트이며 백엔드와 MySQL 포트를 호스트에 공개하지 않습니다.
운영에서는 `prod` 프로필과 강한 `JWT_SECRET`, 명시적인 DB 마이그레이션이 필요하며 JPA는 `validate`를 사용합니다.
기존 DB 데이터나 Caddy 설정을 자동 초기화하지 않습니다. `deploy/schema/mysql84-fresh.sql`은 빈 DB 전용입니다.

관리자 기본 계정은 자동 생성하지 않습니다. 회원가입은 이메일 인증 없이 가능하며,
프로필에서 현재 비밀번호를 확인한 뒤 비밀번호를 변경할 수 있습니다.
이메일 소유 확인이 없는 비밀번호 분실 복구는 제공하지 않습니다.
