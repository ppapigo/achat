# A.chat 프론트엔드

React, Vite, React Router, STOMP, Lucide를 사용하는 JavaScript 웹 앱입니다. TypeScript, React Native, 별도 모바일 프로젝트를 사용하지 않습니다. 모바일 화면도 같은 웹 앱의 반응형 CSS로 제공합니다.

## 실행과 환경 설정

Node.js 22.12 이상에서 저장소 루트 기준으로 실행합니다.

```powershell
npm ci
npm run dev
```

주소는 `http://127.0.0.1:5173`입니다. 프론트엔드 폴더에서 직접 `npm run dev`를 실행해도 됩니다. 백엔드는 별도 터미널에서 MySQL 접속 정보를 지정해 실행합니다. [루트 문서](../README.md)에 DB 생성과 Spring Boot 실행 방법을 정리했습니다.

설정이 필요하면 이 폴더의 `.env.example`을 `.env.local`로 복사합니다. 변경 후 개발 서버를 다시 실행해주세요.

| 변수 | 기본값 | 용도 |
| --- | --- | --- |
| `BACKEND_URL` | `http://localhost:8080` | 개발 서버가 요청을 전달할 백엔드 주소 |
| `VITE_API_BASE_URL` | `/backend` | 브라우저가 요청할 REST API 기본 주소 |
| `VITE_WS_URL` | REST 주소로부터 계산 | 직접 지정할 WebSocket 주소 |

개발에서는 `VITE_API_BASE_URL=/backend`를 유지하고 `BACKEND_URL`만 수정하면 됩니다. Vite가 `/backend` 접두사를 제거해 백엔드로 전달하고 WebSocket 업그레이드도 처리합니다. Vite 환경 변수 중 `VITE_`로 시작하는 값은 브라우저 번들에 포함됩니다. DB 비밀번호와 서버 비밀키는 백엔드 환경 변수로 설정합니다.

이메일과 비밀번호로 회원가입하면 바로 로그인할 수 있습니다. 가입 완료 후 로그인 화면으로 이동하며, 별도의 이메일 인증이나 인증 코드 입력은 필요하지 않습니다. 기존 미인증 계정도 로그인할 수 있습니다.

## 화면과 연결 API

### 백엔드 없이 화면 확인

저장소 루트에서 `npm run dev:ui`를 실행한 뒤 `http://127.0.0.1:5173`에서 **로그인 없이 화면 둘러보기**를 선택합니다. 로그인·회원가입 화면도 그대로 볼 수 있습니다.

`ui-preview` 개발 모드에서는 `src/preview/data.js`가 샘플 데이터를 반환하고 채팅도 브라우저 내부에서 처리합니다. 실제 REST·WebSocket 서버에 연결하지 않습니다. 화면에 미리보기 표시가 나타나며 정상 로그인과 별도의 세션 키를 사용합니다. 새로고침하면 샘플 데이터의 변경 사항이 초기화됩니다. 운영 빌드에서는 미리보기 모드가 비활성화되며 샘플 데이터 파일도 번들에 포함되지 않습니다.

로그인 후 경로에 `?ui-state=empty`를 붙이면 빈 목록, `?ui-state=error`를 붙이면 오류 상태를 볼 수 있습니다. 기본 `npm run dev`는 실제 백엔드 연결 모드입니다.

| 경로 | 기능 | 주요 API |
| --- | --- | --- |
| `/login`, `/register` | 로그인, 가입 | `/auth/login`, `/auth/register` |
| `/` | 최근 피드 3개와 전체 보기, 채팅방 미리보기, 내 성장 | `/api/feed`, `/api/me/posts`, `/rooms` |
| `/feed` | 개인 피드 작성·공감, 댓글 조회·작성·본인 댓글 삭제, 이전 소식 더보기 | `/api/feed`, `/api/me/posts`, `/api/posts/:id/like`, `/api/feed/:id/comments` |
| `/rooms` | 채팅방 검색, 생성 | `/rooms` |
| `/rooms/:roomId` | 실시간 채팅·답장·공감, 최근 기록, 라운드·증강 | `/rooms/:id/join`, `/messages`, `/round`, `/augments/*`, STOMP |
| `/boards` | 카테고리, 검색, 페이지 이동, 게시글 작성 | `/api/boards` |
| `/boards/:boardId` | 게시글 수정·삭제·공감, 댓글·답글 관리 | `/api/boards/:id`, `/comments`, `/like` |
| `/friends` | 닉네임 검색, 친구 요청·수락·거절·삭제, 초대 수락 | `/api/friends`, `/search`, `/requests/*`, `/invites/*` |
| `/party` | 친구를 선택해 파티 생성, 파티 입장·탈퇴 | `/api/party/create`, `/my`, `/:id/leave` |
| `/match` | 6·8·10·12인 매칭 신청·취소, 자동 입장 | `/match/join`, `/leave`, `/status` |
| `/seasons` | 시즌 기간·진행률, 내 랭크·배치 진행, 리더보드, 지난 시즌 보상 | `/api/seasons/current`, `/api/seasons`, `/api/seasons/my-history`, `/rank/me`, `/rank/leaderboard` |
| `/cards` | 하루 한 번 프로필 카드 뽑기, 친구 요청 | `/api/cards/status`, `/draw`, `/my` |
| `/shop` | 상품 분류, 구매, 보관함, 장착·해제, 소모품 사용 | `/api/shop/items`, `/purchase`, `/inventory`, `/equip`, `/unequip`, `/use` |
| `/profile` | 닉네임·상태 메시지·테마, 직업, 최근 전적·통계 | `/auth/profile`, `/api/me/home`, `/jobs`, `/history/summary` |

일반 실행 모드의 화면 데이터는 서버 응답을 사용합니다. 빈 목록, 서버 오류, 로딩 상태는 각각 표시하며 임의의 회원·게시글·상품 데이터를 채우지 않습니다. 상점 상품과 직업 정의는 백엔드 카탈로그입니다. 관리자 전용 화면, 가구 배치 편집기, 신고·제재 관리 UI는 이 웹 앱에 포함하지 않습니다.

홈의 **이번 시즌** 카드와 메뉴의 **시즌**에서 시즌 화면으로 이동합니다. 현재 시즌이 없으면 안내를 표시하고, 랭크·리더보드·지난 기록은 각각 조회합니다. 지난 시즌 기록은 최신 시즌부터 표시하며 지급된 코인은 서버 기록을 사용합니다. 시즌 목록 조회에 실패해도 시즌 번호와 개인 기록은 볼 수 있습니다. 새로고침 버튼으로 전체 정보를 다시 조회하거나 실패한 영역만 재시도할 수 있습니다.

피드는 커뮤니티(`/boards`)와 별도의 메뉴·화면·데이터를 사용합니다. 홈의 **피드 전체 보기**는 `/feed`로 이동합니다. 개인 피드에는 본인·친구·최근 7일간 상호작용한 사용자의 글이 표시됩니다. 글 작성은 500자, 댓글은 2000자까지이며, 글 작성에는 기존 서버의 3분 간격·하루 10개 제한이 적용됩니다.

`GET /api/feed?limit=20`은 `posts`, `hasMore`, `nextCursor`를 반환합니다. 더보기는 `cursor=<nextCursor>`로 다음 글을 조회합니다. 작성 시각과 ID의 내림차순으로 이어서 읽으므로 같은 시각의 글이나 조회 중 새로 올라온 글 때문에 기존 글이 중복되거나 누락되지 않습니다. 댓글 삭제는 서버에서도 본인 여부와 해당 글의 댓글인지 확인합니다. 프론트와 백엔드 변경을 함께 반영해야 더보기를 사용할 수 있습니다.

## 소스 구조

```text
achat-front/
├─ public/favicon.svg
├─ src/
│  ├─ components/       # 공용 UI, 레이아웃, 게시글 편집기
│  ├─ contexts/         # 인증 상태, 알림
│  ├─ hooks/            # 취소 가능한 REST 조회, STOMP 채팅
│  ├─ lib/              # API 클라이언트, 메시지 병합, 포맷 함수와 테스트
│  ├─ pages/            # 라우트별 화면
│  ├─ preview/          # 개발 전용 샘플 데이터와 화면 검증 모드
│  ├─ App.jsx           # 인증 보호와 라우트
│  ├─ main.jsx          # 앱 진입점
│  └─ styles.css        # 공용 스타일과 반응형 화면
├─ .env.example
├─ eslint.config.js
├─ vite.config.js
├─ index.html
└─ package.json
```

루트 `package.json`은 npm workspace와 공통 실행 명령을 관리합니다. `package-lock.json`도 루트에 하나만 유지합니다. 새 기능의 화면은 `pages/`, 재사용 UI는 `components/`에 추가하고 `App.jsx`와 `Shell.jsx`에 경로·메뉴를 등록합니다.

## 인증과 요청 처리

`lib/api.js`가 JSON 직렬화, `Authorization: Bearer ...` 헤더, 오류 메시지와 응답 형식을 처리합니다. `{ success, data, error }`, 피드의 `{ data }`, 일부 API의 원본 객체 응답을 지원합니다. HTTP 200이어도 `success: false`면 사용자에게 오류를 보여줍니다.

토큰은 `sessionStorage`의 `achat.session`에 보관하며 탭을 닫으면 제거됩니다. 페이지 새로고침 시 `/auth/me`를 조회해 사용자 정보를 복원합니다. 인증 오류는 refresh token으로 한 번 갱신하고 재요청합니다. 동시 요청의 갱신을 공유하며, 이미 갱신된 토큰의 늦은 오류와 로그아웃 중 갱신 완료도 처리합니다. 네트워크 오류 시에는 세션을 유지하고 재시도를 제공합니다.

`useResource`는 화면 이동 시 이전 요청을 취소하고 다른 경로의 데이터를 표시하지 않습니다. 작업 버튼은 전송 중 중복 클릭을 막습니다. React가 사용자 텍스트를 이스케이프하며 HTML 문자열을 직접 렌더링하지 않습니다.

## 채팅

`useChat`은 방 입장 완료 후 `/ws`에 STOMP로 연결합니다. CONNECT 헤더에 JWT를 전달하며 재연결 전에 만료 임박 토큰을 갱신합니다. 연결 실패 시 3초 간격으로 재연결합니다.

- 발행: `/app/rooms/:id/join`, `/chat`, `/like`
- 구독: `/topic/rooms/:id/messages`, `/message-effects`, `/likes`, `/events`, `/round`, `/round/end`, `/round/highlights`, `/augments`, `/quest`
- 개인 알림: `/user/queue/cooldown`, `/error`, `/quests`

구독을 먼저 등록한 다음 최근 메시지를 조회해, 로딩 도중 수신한 메시지를 기록과 병합합니다. ID로 중복을 제거하고 오래된 기록이 최신 공감 상태를 덮어쓰지 않게 합니다. Enter로 보내기, Shift+Enter로 줄바꿈을 지원하며 한국어 입력 조합 중 Enter는 전송하지 않습니다.

`GET /rooms/:roomId/messages?limit=100`은 최근 1~100개를 시간순으로 반환하고 방 멤버에게만 허용합니다. 기록은 JPA/MySQL에 저장한 메시지입니다. 서버가 닉네임과 구조화된 효과를 반환하며 익명 메시지는 식별자를 제거합니다. 이벤트와 기록은 revision으로 병합합니다.

`components/augments/`에서 증강 27종의 JSX·CSS와 공유 서버 시간 타이머를 관리합니다. 각 선택·대화 구간마다 상대에게 하나를 보내고 하나만 받을 수 있으며, 대상 선택 후 카드와 실제 효과 미리보기를 확인하고 확정합니다. 점유된 대상은 비활성화하고 409 오류 시 목록을 갱신합니다. 상세 규칙·보상 상한·접근성·브라우저 검사 방법은 [증강 시스템 문서](../docs/augment-system.md)를 참고하세요.

화면 이동과 방 탈퇴는 다릅니다. 다른 메뉴로 이동해도 방 멤버로 남으며, 채팅방 나가기 버튼이 REST 탈퇴를 요청합니다. 매칭 상태는 3초 간격으로 조회하고 매칭 완료 시 자동 이동합니다. 라운드·매칭 대기열·접속 상태는 백엔드의 임시 메모리 상태입니다.

## 빌드와 운영 연결

```powershell
npm run lint
npm run test
npm run build
npm run preview
```

빌드 결과는 `dist/`에 생성됩니다. 운영 웹 서버에서 해당 폴더를 제공하고 브라우저 라우트(`/rooms/:id` 등)는 `index.html`로 fallback하도록 구성합니다. `/backend/*`는 접두사를 제거해 Spring Boot로 전달하고 `/backend/ws`의 WebSocket 업그레이드를 허용해야 합니다. `vite preview`는 운영 프록시를 대신하지 않습니다.

API를 별도 도메인에 제공한다면 빌드 전에 `VITE_API_BASE_URL`과 `VITE_WS_URL`을 지정하고 백엔드에서 해당 프론트엔드 origin의 HTTP CORS 및 WebSocket 연결을 허용합니다. HTTPS 화면에서는 HTTPS API와 WSS를 사용합니다. 환경 값을 바꾸면 다시 빌드해야 합니다.

## 검증

Vitest는 API 응답·오류 처리, 동시 토큰 갱신, 늦게 도착한 인증 오류, 로그아웃 중 갱신, 네트워크 장애, 요청 취소, 채팅 기록 병합 및 WebSocket 주소를 검증합니다. 백엔드 통합 테스트는 멤버 권한, 기록 개수 제한과 시간순 반환도 확인합니다.

가입·로그인 흐름은 PC와 모바일에서 별도로 검사합니다. 중복 가입 오류, 가입 완료 후 로그인 화면 이동, 로그인 성공과 세션 저장, 인증 코드 화면·요청 제거를 확인합니다. API 응답을 가로채므로 실제 계정이나 DB는 변경하지 않습니다.

```powershell
# 터미널 1: 일반 개발 서버
npm run dev --workspace achat-front -- --port 5176

# 터미널 2: 가입·로그인 검사
npm run test:auth
```

같은 일반 개발 서버에서 `npm run test:feed`로 PC·모바일 피드 검사를 실행합니다. 홈/피드/커뮤니티 이동, 글 작성, 좋아요, 20개 이후 더보기, 댓글 조회·작성·삭제, 오류 재시도와 빈 상태를 확인하며 실제 DB 대신 API 응답을 모의합니다. 스크린샷은 `achat-front/test-results/feed/`에 저장합니다. 백엔드의 커서 정렬·피드 대상·댓글 삭제 권한은 `FeedTests`에서 H2로 검증합니다.

별도의 MySQL 테스트 DB에서 Vite 프록시를 통한 주요 화면 API 17개, 로그인·토큰 갱신, STOMP 메시지 송수신·공감, 기록 조회, 라운드·증강 선택, 게시글·댓글·답글, 피드, 구매·장착, 프로필·직업과 매칭 신청·취소를 확인했습니다.

백엔드 없이 Playwright와 Edge로 PC(1440×1000), 모바일(390×844)의 주요 화면 13개씩, 총 26개 조합을 검증합니다. 로그인·회원가입, 방·파티·게시글 모달, 메뉴 이동, 샘플 채팅 입력, 상품 분류·보관함, 빈 목록·오류 화면도 확인합니다. 가로 넘침, 검색 버튼 줄바꿈, JavaScript 오류, 실제 백엔드 REST·WebSocket 요청을 검사하며 스크린샷을 저장합니다. 스크린샷 검토에서 발견한 모바일 홈 장식 겹침, 검색 버튼 줄바꿈, 게시글 제목 공간 부족과 미리보기 표시의 내용 가림을 수정했습니다.

재실행하려면 저장소 루트의 두 터미널에서 다음 명령을 각각 실행합니다.

```powershell
# 터미널 1: 화면 미리보기 서버
npm run dev:ui

# 터미널 2: 브라우저 검사
npm run test:ui
```

검사 스크립트는 루트의 `scripts/check-ui.mjs`이고 결과는 `achat-front/test-results/ui/`의 PNG와 `report.json`입니다. 결과 폴더는 Git에서 제외합니다. Windows에서는 설치된 Edge를 사용합니다. Chrome을 사용하려면 `$env:UI_BROWSER_CHANNEL = 'chrome'`을 지정합니다. 다른 OS에서는 `npx playwright install chromium`으로 테스트 브라우저를 준비합니다.

이번 화면 검사는 샘플 데이터에서의 렌더링과 조작을 확인합니다. 실제 저장·권한·다중 사용자 채팅 검증은 위의 백엔드 통합 검사 범위입니다. Vite의 개발용 HMR 연결은 화면 검사 중에도 사용합니다.

시즌 화면은 운영 빌드에서도 별도로 검증할 수 있습니다. 브라우저에서 API 요청을 가로채 실제 서버와 같은 원본 시즌 객체·배열 및 랭크 응답 형식을 반환합니다. PC·모바일에서 기록 정렬, 보상, 배치 중·최고 티어, 시즌 없음(404), 마지막 날·종료 대기, 일부 API 실패와 재시도, 전체 새로고침, 홈·메뉴 이동을 확인합니다. 실제 서버나 DB는 변경하지 않습니다.

```powershell
# 터미널 1
npm run build
npm run preview --workspace achat-front -- --port 5175

# 터미널 2
npm run test:seasons
```

스크립트는 `scripts/check-seasons-ui.mjs`이며 결과는 `achat-front/test-results/seasons/`에 저장합니다. 다른 주소에서는 `SEASON_UI_BASE_URL`을 지정합니다. `ui-preview` 모드 대신 일반 개발 서버 또는 운영 빌드 미리보기를 사용해야 합니다.

## 기존 프로젝트 정리

기존 `achat-front`, `achat-front-backup-web`, `achat-mobile`은 삭제했습니다. 기존 웹 폴더의 gitlink도 제거해 별도 저장소 초기화 없이 새 프론트엔드 소스를 받을 수 있습니다. 루트의 오래된 `test.html`과 React Native 의존성도 제거했습니다. 기존 MySQL/JPA 백엔드 변경은 유지했습니다.
