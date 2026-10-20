# AChat 보안 강화 및 수동 배포 안내

작성: 2026-10-20. 대상은 이 저장소의 AChat뿐이다. 운영 Lightsail, GitHub Actions, MOA, Groover, 기존 Caddy 및 board-net은 변경하지 않았다.

## 1. 확인한 취약점과 조치

| 위험도 | 확인한 문제 | 적용한 조치 |
| --- | --- | --- |
| 치명적 | 모든 환경에서 소스에 고정된 관리자 계정을 자동 생성 | 자동 생성 제거. prod 시작 시 기존 기본 비밀번호를 사용하는 계정이 남아 있으면 시작 거부. 기존 사용자 행은 보존 |
| 치명적 | 일반 사용자의 시즌 종료 및 임의 직업 경험치 지급 API 접근 | 서버 DB의 관리자 권한으로 제한. 경험치 입력 범위 제한 |
| 높음 | Refresh Token 재사용, 로그아웃/비밀번호 변경 뒤에도 재발급 가능 | DB 세션, 해시 저장, 행 잠금 회전, 재사용 시 해당 세션 전체 폐기, 로그아웃 및 비밀번호 변경에 따른 무효화 |
| 높음 | WebSocket Origin 전체 허용, 연결 후 만료/정지/탈퇴 반영 부족 | prod 도메인 제한, Access Token만 허용, SEND/SUBSCRIBE 및 실제 outbound 전달 때 재검증 |
| 높음 | 임의 매치/파티 입장 및 다른 방 ID로 하위 방 삭제 가능 | 매치 배정/파티 초대 멤버 검사, 부모-자식 관계 검사, REST/서비스/메시지 처리 권한 확인 |
| 높음 | 계정 대량 생성·로그인 추측 제한 부재 | IP 가입 제한, IP 및 계정별 로그인 제한, 429와 Retry-After, 존재하지 않는 계정에도 BCrypt 비용 적용 |
| 높음 | 큐 중복 가입 및 동일 스케줄러 중복 실행 | 원자적 큐 조작, 매칭 작업 전체 잠금, 진행 중 매치 재가입 거부, 실패 시 메모리 복구, 커밋 후 알림/3초 지연 시작 |
| 보통 | 반복 신고 경쟁, 일부 무제한 입력/조회, 예외 내용 노출 | 신고 대상별 직렬화, 요청 64 KiB 제한, 필드/페이지 제한, 401/403/409/429 및 안전한 예외 응답 |
| 보통 | 실제 발송 수단과 비밀번호 복구 경로 부재 | 앞서 요청한 이메일 인증 제거 유지. 코드 발송/인증 경로 재도입 안 함. 현재 비밀번호를 확인하는 변경 API와 UI 추가 |

현재 `application.yml`의 키는 이미 `${JWT_SECRET}`였으므로 “현재도 하드코딩”으로 판정하지 않았다. 이번 변경은 강도 검사와 운영 설정 분리에 중점을 둔다.

## 2. 주요 변경 파일

- 인증: `auth/JwtProvider`, `AuthService`, `AuthController`, `AuthSession`, `AuthSessionRepository`, `AuthSessionService`, 인증 DTO, `user/User`, `UserStore`.
- 공통 보안: `security/SecurityConfig`, `JwtAuthFilter`, `RequestProtectionFilter`, `RateLimitService`, `SecurityOrigins`, `SecurityResponses`, `UnauthorizedException`, `AccountAccess`, `RoomAccess`, `ProductionAccountCheck`, `HealthController`.
- 실시간/권한/동시성: `websocket/WebSocketConfig`, `WebSocketAuthInterceptor`, `ChatController`, `common/GlobalExceptionHandler`, `RoomExecution`, `room/RoomService`, `RoomController`, `PartyController`, `match/MatchQueue`, `MatchService`, `friend/FriendService`, `sanction/SanctionService`, `report/ReportService`, `season/SeasonController`, `job/JobController`, `round/RoundController`.
- 입력/조회 제한: 방 DTO, `home/dto/HomeDto`, `board/BoardService`, `feed/FeedService`, `minihome/MiniHomeService`, `post/PostStore`, `history/GameHistoryStore`.
- React: `lib/api.js`, `lib/api.test.js`, `contexts/AuthContext.jsx`, `hooks/useChat.js`, `components/PasswordSettings.jsx`, `pages/ProfilePage.jsx`.
- 설정/배포: `application.yml`, `application-dev.yml`, `application-prod.yml`, `.env.example`, `.env.prod.example`, `.gitignore`, `.dockerignore`, 루트 및 프론트 Dockerfile, `compose.prod.yml`, `deploy/nginx.conf.template`, `deploy/Caddyfile.achat.example`.
- SQL: `deploy/migrations/20261020_01_auth_sessions.sql`, 빈 DB 전용 `deploy/schema/mysql84-fresh.sql`.
- 검증: `SecurityHardeningTests`, `AugmentApiTests`, `AugmentWebSocketTests`, `AuthFlowTests`, `JpaPersistenceTests`, `JpaRestartPersistenceTests`, opt-in 인증 감사 스크립트.

Java 파일 경로는 `src/main/java/com/achat/`, 테스트는 `src/test/java/com/achat/`, React는 `achat-front/src/` 기준이다. 기존 증강 작업의 변경 파일은 `augment-system.md`를 별도로 참조한다.

## 3. JWT와 WebSocket 계약

- Access 최대 15분, Refresh 최대 7일. Refresh는 로그인 때 정해진 절대 만료 시간을 연장하지 않는다.
- 서명, issuer=`achat`, `type`, UUID subject/session/JTI, 발급·만료 시각을 검사한다. 기존 서명만 있는 토큰은 새 세션 모델에서 무효이므로 배포 후 한 번 재로그인이 필요하다.
- Refresh Token 원문 대신 SHA-256 해시를 저장한다. `PESSIMISTIC_WRITE`로 회전을 직렬화한다. 이전 토큰 재사용은 최신 토큰을 포함한 세션 전체를 무효화하며, 거부 응답에도 폐기 트랜잭션을 커밋한다.
- `/auth/logout`은 해당 Refresh Token 세션을 폐기한다. `/auth/password`는 JWT 사용자와 현재 비밀번호를 확인하며 모든 기기의 기존 세션을 무효화한다.
- React는 동시 갱신을 하나로 합치며 401에서만 갱신한다. 403에는 재시도하지 않는다. 로그아웃은 진행 중인 갱신을 마친 후 서버 세션을 폐기한다. STOMP는 만료 전에 재연결해 새 Access Token을 사용한다.
- `/app/rooms/{UUID}/{chat,like,join,leave}`만 SEND 가능하다. 채팅 발신자는 Principal에서 결정한다.
- 방별 `messages`, `message-effects`, `likes`, `events`, `round`, `round/end`, `round/highlights`, `augments`, `quest` Topic은 실제 참여자만 구독한다.
- `/user/queue/{error,cooldown,quests,friends}`는 본인용이며 `/topic/match/{본인ID}`도 유지한다. 임의 `/queue`, 다른 사람의 `/user/{ID}`, wildcard 및 임의 Topic은 거부한다.
- 연결 시 인증 헤더를 제거하고 오류 프레임에는 일반 메시지만 넣는다. 토큰을 URL에 붙이지 않는다.
- 실제 전달 시에도 세션 만료·로그아웃·정지·방 탈퇴를 검사한다. 이미 구독해 놓은 소켓으로 이후 방 메시지를 계속 받는 것을 막는다.

구현은 Spring의 [STOMP 토큰 인증 방식](https://docs.spring.io/spring-framework/reference/6.2/web/websocket/stomp/authentication-token-based.html)에 맞춘다.

## 4. 인증번호·가입·비밀번호

앞선 명시적 요청에 따라 `VerificationCodeStore`, `/auth/verify`, `/auth/resend-code`는 삭제되어 있다. 현재 인증번호 생성·저장·전송·출력 경로가 없으므로 인증번호 실패 횟수/쿨타임을 위한 가짜 기능을 추가하지 않았다. 반복 호출 시 경로가 존재하지 않는 것을 테스트한다.

- 가입: IP당 10분에 5회. 로그인: IP당 1분에 30회, 정규화된 계정당 15분에 8회. 계정 제한에는 성공 시도도 포함한다.
- 인증 세션 갱신/로그아웃: IP당 1분에 60회. 신뢰하지 않는 프록시 헤더는 IP 기준에 사용하지 않는다.
- 회원가입은 이메일 소유를 증명하지 않는다. **이메일만 안다는 이유로 비밀번호를 재설정하는 경로를 열면 안 된다.**
- 현재 비밀번호를 아는 사용자는 프로필의 비밀번호 변경 기능을 사용할 수 있다. 비밀번호를 잊은 사용자의 셀프 복구는 아직 제공하지 않는다. 이메일 발송했다고 안내하지도 않는다.
- 중복 이메일 가입은 일반 오류 문구로 응답하지만 성공/실패 상태 차이는 남는다. 계정 존재 여부를 완전히 숨기는 가입 프로토콜로 변경한 것은 아니다.

## 5. 동시성·권한 검증

인메모리 큐의 개별 컬렉션만 thread-safe인 상태에서는 `leave → add → joinTime 저장`과 두 번 등록된 스케줄러가 서로 끼어들 수 있었다. 큐 메서드를 원자화하고, 전체 매칭 조작을 같은 락/트랜잭션으로 보호했다. 방·매칭 락은 DB 커밋/롤백까지 유지하며 실패 시 큐와 매칭 맵을 복구한다. 락 저장소는 고정 1024개 stripe로 제한해 임의 ID에 의한 영구 메모리 증가를 막는다.

상점 코인/재고는 기존 트랜잭션, `@Version`, 사용자-아이템 유니크 제약으로 중복 지급을 방지한다. 실제 동시 구매 테스트에서 하나만 성공하고 코인은 한 번 차감된다. 증강/퀘스트 보상 영수증 및 라운드 종료 복구 구조는 유지했다. 신고는 동일 대상의 검사·저장·자동 제재를 같은 임계 구역에서 수행한다.

관리자 API는 SecurityFilterChain의 역할 검사와 실제 DB 관리자 상태를 사용한다. 신고·제재 대상 ID는 클라이언트가 선택할 수 있지만 **행위자** ID는 토큰에서 정한다. 제재 서비스도 관리자 상태를 검사한다.

## 6. 게임·채팅 호환성

6/8/10/12인 매칭, 20초 후 인원 확장, 매칭 완료 3초 후 시작, 10분 게임과 기존 3회 선택 흐름은 유지했다. 메시지/증강/퀘스트 응답 구조와 개인 큐 이름도 유지했다. 공개 영구방은 기존처럼 목록과 입장이 가능하고, 파티는 초대된 친구, 매치는 배정된 사용자에게 제한한다. 타인의 프로필·공개 게시글 조회는 제품 기능으로 유지한다.

현재 저장소는 Java 21 toolchain과 Java 21 API를 사용하므로 Docker도 Java 21이다. Spring Boot 3.5.8은 유지했다. 이 변경에서 Java 17로 다운그레이드하지 않았다.

## 7. 검증 기록

- 최종 H2 전체 회귀 테스트: 117개 통과, 실패/오류 0개. 별도 `authSecurityAudit` 4개도 통과.
- 실제 로컬 MySQL 8.4.11의 보안·동시성 테스트: 24개 통과. 메모리 기반 임시 DB만 사용했다.
- 실제 STOMP: 메시지·답장·효과 업데이트, 외부 Origin 차단, 무인증/Refresh CONNECT 차단 통과.
- React 테스트 18개, ESLint, 프로덕션 빌드 통과.
- Compose 구성 검증 및 백엔드/프론트 Docker 빌드 통과. 비관리자·읽기 전용 컨테이너에서 실행 확인.
- 빈 MySQL 스키마 SQL 적용 후 prod 백엔드가 `ddl-auto=validate`, MySQL TLS 연결로 정상 시작하고 healthcheck 통과.
- `scripts/check-security-proxy.mjs`로 실제 Nginx를 거친 정적 화면/CSP, 가입·로그인, Principal 소유권, 관리자 403, Refresh Token API 거부, 외부 Origin 403, `/backend/ws`·`/ws` 연결, 실제 채팅 및 로그아웃 폐기 확인.
- 추가한 SQL은 별도 빈 MySQL 테스트 DB에서 실행해 컬럼 구성을 확인했다.
- 초기 실패: 테스트 `Message` import 충돌 수정, 관리자 없이 `SYSTEM`으로 제재를 해제하던 fixture를 실제 관리자 계정으로 교체, 프론트 Dockerfile을 루트 npm workspace lockfile 구조로 수정. 최신 Nginx의 PID 경로를 `/tmp`로 변경해 읽기 전용 실행 오류도 해결했다. 프록시 점검 스크립트는 SimpleBroker가 지원하지 않는 subscription receipt 대신 실제 메시지 수신을 검증한다.
- 테스트 전용 컨테이너 3개와 네트워크 2개는 라벨을 확인한 뒤 정리했다. 임시 MySQL은 tmpfs만 사용했으며 기존 DB/볼륨에는 접근하지 않았다. 로컬 검증 이미지 2개는 남겨 두었다.

예시 명령(항상 테스트 DB):

```powershell
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
.\gradlew.bat test bootJar --console=plain
.\gradlew.bat --init-script scripts/security-audit/init.gradle authSecurityAudit --console=plain
npm test
npm run lint
npm run build
```

기본 테스트 설정은 H2이며 MySQL 검증에서는 임시 컨테이너 주소만 `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`, `TEST_DB_DRIVER`로 전달했다. 운영 DB를 테스트에 연결하면 안 된다.

## 8. 남는 위험과 운영 전 확인

- 단일 JVM 전제: 매칭·라운드·증강 선택·신고 락과 제한 횟수는 인메모리다. 재시작하면 제한 카운터가 초기화된다. 여러 인스턴스로 확장하려면 공유 rate limiter/분산 락/DB 기반 매칭 상태가 필요하다. DB Refresh 회전 자체는 행 잠금으로 공유된다.
- 여러 IP를 쓰는 계정 농장은 IP 제한만으로 완전히 막을 수 없다. 시작 코인 1000은 게임 규칙을 유지했다. 공개 트래픽에 맞춰 CAPTCHA/공유 제한/운영 탐지 추가를 검토해야 한다.
- 계정별 로그인 제한으로 공격자가 특정 계정의 로그인을 잠시 방해할 수 있다. 자동 해제는 15분이다.
- 이메일 미검증 계정의 비밀번호 분실 복구는 미구현이다. 현재 비밀번호 변경은 가능하다.
- 활성 라운드 복원과 다중 서버 실시간 브로커는 이번 보안 변경 범위에서 재설계하지 않았다. 재시작 시 진행 중 게임이 중단될 수 있으므로 점검 시간에 전환한다.
- 기존 운영 스키마와 데이터는 읽지 않았다. 현재 코드 전체에 필요한 테이블·enum·유니크 제약을 운영 복제본과 대조해야 한다. 새 `auth_sessions` SQL 하나가 이전 증강 변경까지 자동으로 마이그레이션한다고 가정하면 안 된다.
- 패키지 전반의 CVE 감사, 대규모 부하 테스트, 실제 Lightsail 메모리 경쟁, 실제 DNS/TLS/Caddy 연결은 별도 검증 대상이다.
- Java/Node/Nginx/MySQL 이미지는 실제 검증한 digest로 고정했다. 이 작업에서는 애플리케이션 의존성 전체 업그레이드를 하지 않았다.
- MySQL의 `sslMode=REQUIRED`는 전송을 암호화하지만 CA/호스트 신원을 검증하는 설정은 아니다. DB는 전용 Docker 내부망에 한정했다. 별도 CA 기반 검증은 운영 인증서 준비가 필요하다.
- 네트워크 장애 중 로그아웃은 로컬 토큰을 지우지만 서버 폐기 성공을 보장할 수 없다. 서버가 정상 응답한 로그아웃의 폐기는 테스트했다.

## 9. Lightsail 운영 변수와 네트워크

`.env.prod.example`을 `.env.prod`로 복사하고 소유자만 읽게 설정한다. 파일은 Git과 Docker 빌드 컨텍스트에서 제외된다. 운영 파일을 채팅·로그·PR에 붙이지 않는다.

| 변수 | 값/용도 |
| --- | --- |
| `JWT_SECRET` | 독립적으로 생성한 64자 이상 무작위 키. `openssl rand -base64 64` 결과를 안전하게 파일에 저장 |
| `MYSQL_DATABASE` | AChat 전용 DB명 |
| `MYSQL_USER`, `MYSQL_PASSWORD` | AChat 전용 계정. 기존 볼륨에서는 초기화 환경변수만 바꿔도 DB 비밀번호가 바뀌지 않음 |
| `MYSQL_ROOT_PASSWORD` | AChat MySQL 초기 관리용. 백엔드에는 전달하지 않음 |
| `ACHAT_MYSQL_VOLUME` | 확인한 AChat 전용 외부 볼륨 이름. MOA/Groover 볼륨 지정 금지 |
| `ACHAT_RELEASE` | 변경할 때마다 새 이미지 태그. 이전 태그는 롤백용으로 보존 |
| `CADDY_TRUSTED_CIDR` | 기존 Caddy의 proxy-net 주소 `/32`. 광범위한 0.0.0.0/0 금지 |
| `ACHAT_APP_SUBNET`, `ACHAT_FRONT_INTERNAL_IP` | 다른 네트워크와 겹치지 않는 AChat 내부 대역과 프론트 주소 |

Compose가 백엔드에 `SPRING_PROFILES_ACTIVE=prod`, 내부 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, 프론트 주소만 신뢰하는 `TRUSTED_PROXY_CIDRS`, JVM 메모리 한도를 전달한다. 운영 JPA는 `validate`로 고정한다.

```text
Internet HTTPS
  → 기존 Caddy(변경은 별도 승인)
  → proxy-net의 achat-web:8080 (정적 React / Nginx)
  → AChat 내부 app 네트워크의 backend:8080
  → 별도 내부 data 네트워크의 mysql:3306
```

호스트에 AChat 8080/3306을 publish하지 않는다. 프론트에만 proxy-net을 연결한다. Caddy는 클라이언트가 보낸 `X-Real-IP`를 실제 접속 주소로 덮어쓰고 Nginx는 지정한 Caddy만 신뢰한다. `/backend/...`를 백엔드 `/...`로 전달하며 `/backend/ws`와 `/ws` 모두 WebSocket Upgrade를 처리한다. HTTPS에서 기본 클라이언트 주소는 `wss://achat.grooverapp.com/backend/ws`다.

[Docker 외부 네트워크](https://docs.docker.com/compose/how-tos/networking/), [Nginx WebSocket 프록시](https://nginx.org/en/docs/http/websocket.html), [Caddy reverse_proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy)를 기준으로 구성했다.

총 메모리 한도는 MySQL 1200 MiB, 백엔드 1600 MiB, 프론트 128 MiB다. 8GB 서버의 기존 서비스 여유 메모리는 실제 서버에서 승인 후 확인해야 한다. 이미지 빌드는 별도 머신에서 수행하는 편이 기존 서비스의 메모리 경쟁을 줄인다.

## 10. 수동 배포와 롤백

아래는 운영자가 **승인 후** 수행할 절차이며 이번 작업에서는 실행하지 않았다.

1. AChat 전용 DB/볼륨과 현재 서비스·Caddy 네트워크를 확인한다. 기존 서비스 설정은 덮어쓰지 않는다. `.env.prod`를 준비하고 `chmod 600 .env.prod`를 적용한다.
2. AChat DB의 일관된 백업을 만들고 별도 복구 DB로 복원 시험한다. DB 비밀번호는 명령행 인자나 로그에 넣지 않고 접근 제한된 MySQL option file을 사용한다.
3. 운영 DB 복제본과 `deploy/schema/mysql84-fresh.sql`을 비교한다. **fresh SQL은 빈 새 DB 전용**이다. 기존 DB에는 검토한 추가 마이그레이션만 적용한다. 이번 인증 변경은 `20261020_01_auth_sessions.sql`이며 실행 이력/체크섬을 기록한다. MySQL DDL은 자동 커밋되므로 트랜잭션 롤백에 의존하지 않는다.
4. 테스트/빌드를 완료한 소스로 새 `ACHAT_RELEASE`를 지정한다.

   ```sh
   docker compose --env-file .env.prod -f compose.prod.yml config --quiet
   docker compose --env-file .env.prod -f compose.prod.yml build backend frontend
   ```

5. 기존 AChat DB 볼륨을 그대로 사용할지, **새 AChat 전용 빈 볼륨에 검증된 백업을 복원**할지 정한다. 같은 볼륨을 두 MySQL 프로세스가 동시에 마운트하면 안 된다. 볼륨 삭제나 `down -v`를 실행하지 않는다.
6. 기존 기본 관리자 비밀번호를 사용하는 계정이 있으면 안전하게 변경한다. 새 관리자가 필요하면 정상 가입한 정확한 AChat 사용자 ID를 운영자가 확인한 후 해당 행만 관리자 권한으로 승격한다. 기본 계정/비밀번호를 배포 스크립트로 만들지 않는다.
7. 승인된 점검 시간에 AChat만 시작한다.

   ```sh
   docker compose --env-file .env.prod -f compose.prod.yml up -d mysql backend frontend
   docker compose --env-file .env.prod -f compose.prod.yml ps
   ```

8. 기존 Caddy가 proxy-net에 연결되어 있는지 확인한다. 연결/설정 변경이 필요하면 별도 승인 후 `deploy/Caddyfile.achat.example`의 AChat 사이트 블록만 병합하고 기존 설정 전체를 validate한 뒤 reload한다. 기존 board-net이나 사이트 블록을 제거하지 않는다.
9. 가입/로그인/갱신/로그아웃, 일반 사용자의 관리자 403, 외부 Origin 거부, 타인 방 구독 거부, 정상 채팅·답장·좋아요·매칭·라운드·상점·랭킹을 확인한다. 호스트 8080/3306이 외부에 열리지 않았는지도 확인한다.
10. 롤백 시 AChat 프론트/백엔드 이미지 태그만 직전 검증된 릴리스로 되돌려 `up -d --no-build backend frontend` 한다. 추가 테이블은 남겨 둔다. 스키마 호환성을 먼저 확인하고 필요한 경우 새 DB에 백업을 복원해 검증한 뒤 전환한다. 기존 데이터를 덮어쓰거나 전체 DB를 초기화하지 않는다. 취약한 이전 버전으로 돌아가야 한다면 공개 접근을 제한한 상태로 수정 버전을 준비한다.

기존 Caddy 설정을 변경했다면 해당 사이트 블록의 이전 사본으로 복원한다. JWT 키를 바꾸면 기존 세션은 다시 로그인해야 하며, 이전 키로 되돌려 탈취 토큰이 살아나게 하지 않는다.
