# 이메일 인증 제거 후 인증 보안 점검

> 이 문서는 **보안 수정 전의 감사 기록**이다. 후속 코드 수정과 현재 제약은 [보안 강화 및 배포 안내](security-deployment.md)를 참조한다. 아래 재현 결과와 라인 번호는 당시 상태이며, `authSecurityAudit` 태스크는 이제 수정된 동작을 검증하는 회귀 테스트로 변경되었다.

점검일: 2026-10-20. 현재 작업 트리의 가입·로그인·복구 경로를 정적 검토하고, 별도 H2 메모리 DB에서 Spring Security 필터를 포함한 MockMvc 감사 테스트를 실행했다.

**결론: 가입 시 이메일 인증을 생략하는 정책은 반영되어 있으나, 이를 보완할 자동화 방지·로그인 시도 제한·계정 복구 수단은 아직 없다. 고정 자격증명의 관리자 자동 생성과 토큰 폐기 공백도 확인했다. 이번 작업은 점검이며 아래 문제를 수정한 상태가 아니다.**

운영 서버·실제 MySQL·외부 이메일에 요청하지 않았고 실제 계정·데이터·배포 상태를 변경하지 않았다. 저장소에 없는 WAF/리버스 프록시의 제한 설정은 확인하지 못했으므로, 결과는 애플리케이션 계층 기준이다.

## 발견 사항

### A-01 — 긴급: 고정된 자격증명의 관리자 계정 자동 생성

- 근거: [UserStore.java:18](../src/main/java/com/achat/user/UserStore.java#L18), [AdminController.java:33](../src/main/java/com/achat/admin/AdminController.java#L33).
- 서버 시작 시 해당 이메일이 없으면 코드에 고정된 비밀번호로 `isAdmin=true`인 계정을 만든다. 프로필/환경 조건이 없다.
- 격리된 신규 DB에서 코드의 기본 자격증명으로 `/auth/login` HTTP 200 및 access token 발급을 재현했다. 생성 계정의 관리자 플래그도 확인했다. 보고서에는 비밀번호를 복제하지 않았다.
- 영향: 동일 초기화 코드가 실행되는 환경에서 기본 계정이 남아 있으면 관리자 계정 탈취로 이어질 수 있다. 운영 계정의 실제 비밀번호는 확인하지 않았다.
- 조치: 기본 자동 생성 제거 또는 개발 전용 명시적 opt-in으로 제한. 이미 배포된 기본 계정의 자격증명 교체와 기존 세션 폐기를 별도로 처리해야 한다. 코드를 고치는 것만으로 기존 DB의 계정은 바뀌지 않는다.
- 이메일 인증 제거 이전부터 존재하는 문제이며, 이번 제거로 새로 생긴 문제라고 보지 않는다.

### A-02 — 높음: 계정 대량 생성 방어 없음

- 근거: [AuthService.java:29](../src/main/java/com/achat/auth/AuthService.java#L29), [SecurityConfig.java:35](../src/main/java/com/achat/security/SecurityConfig.java#L35), [User.java:73](../src/main/java/com/achat/user/User.java#L73).
- 가입은 이메일 중복 여부, 요청 DTO 검증, BCrypt 해싱, 저장으로 끝난다. IP/시간창별 가입 한도, CAPTCHA 검증, 위험도 기반 제한이 없다.
- 같은 테스트 IP에서 서로 다른 가상 이메일 12개를 연속 가입시켰고 **12/12 HTTP 200**, 계정마다 초기 1,000코인이 지급됐다. `Retry-After`는 없었다.
- 이메일 형식 검증과 동일 이메일 유일 제약은 다른 이메일을 사용하는 계정 대량 생성을 막지 않는다. 프론트의 전송 중 버튼 비활성화도 직접 API 호출을 제한하지 않는다.
- 영향: 스팸·제재 후 재가입·다계정 공감/보상 악용의 비용이 낮다. 보상 이전·현금화 여부는 이번 점검에서 확인하지 않았다.
- 조치: 서버 가입 요청 제한과 가입 성공 수 제한, 과도한 요청에 429/Retry-After, 위험도에 따른 CAPTCHA, 신규 계정 활동/보상 제한을 함께 설계한다. 다중 인스턴스에서는 공유 저장소와 원자적 카운터가 필요하다. IP는 신뢰한 프록시 경로에서만 추출하고 임의의 X-Forwarded-For를 신뢰하지 않아야 한다.

### A-03 — 높음: 로그인 실패 횟수 제한 없음

- 근거: [AuthService.java:41](../src/main/java/com/achat/auth/AuthService.java#L41), [SecurityConfig.java:44](../src/main/java/com/achat/security/SecurityConfig.java#L44).
- 계정별 실패 횟수, IP별 요청 수, 점진적 지연, 임시 차단 상태를 저장하거나 확인하는 코드가 없다. 기존 제재(BAN)는 로그인 추측 방어와 별도다.
- 한 계정에 잘못된 비밀번호로 **20회 연속 요청 모두 HTTP 400**, 직후 올바른 비밀번호는 **HTTP 200**이었다. 존재하지 않는 계정도 **30회 모두 HTTP 400**으로 처리됐다. 테스트 범위 내 429/Retry-After는 없었다.
- 영향: 비밀번호 추측·유출 자격증명 대입을 애플리케이션이 제한하지 않는다. 기존 계정 시도는 매번 BCrypt 연산을 수행한다.
- 조치: 정규화한 계정 식별자와 IP 기준 제한을 함께 적용하고, 원자적 실패 카운터/TTL/점진적 지연/429를 구현한다. 영구 잠금으로 타인의 계정을 못 쓰게 만드는 공격을 피하도록 정책을 설계한다. 제한 수치는 실제 트래픽·공유 IP 환경에 맞춰 설정해야 한다. [OWASP 로그인 제한 지침](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#login-throttling).

### A-04 — 중간: 비밀번호 재설정·변경 수단 미구현

- 근거: [AuthController.java:25](../src/main/java/com/achat/auth/AuthController.java#L25), [AuthPage.jsx:24](../achat-front/src/pages/AuthPage.jsx#L24).
- 런타임 인증 매핑은 register, login, refresh, me, profile 다섯 개뿐이다. profile은 닉네임만 변경한다.
- 저장소의 전체 경로 검색에서도 forgot/reset/change-password, 복구 토큰 저장·메일 발송 서비스, 복구코드/패스키 등록 흐름을 찾지 못했다. 프론트에 비밀번호 찾기 화면도 없다.
- 영향: 비밀번호 분실 또는 노출 시 사용자가 스스로 복구·변경할 수 없다. 기존 `email_verified`는 호환 컬럼일 뿐 신뢰할 수 있는 복구 이메일의 증거가 아니다.
- 조치: **가입 이메일 인증은 계속 선택 사항으로 두고**, 로그인 후 현재 비밀번호 재확인을 거쳐 복구 이메일 소유권 확인 또는 복구코드/패스키 등록을 제공할 수 있다. 복구 수단이 없는 기존 계정에 대한 지원 절차도 별도 정의해야 한다.
- 복구 토큰은 충분한 난수, 짧은 만료, 서버 해시 저장, 단일 사용·원자적 소비를 적용한다. 존재 여부와 무관한 동일 안내, 요청 제한, 완료 통지 및 세션 폐기도 필요하다. 미검증 가입 이메일 문자열만으로 신원을 확인한 것으로 처리해서는 안 된다. [OWASP 비밀번호 복구 지침](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).

### A-05 — 높음: 비밀번호 변경에 따른 기존 토큰 폐기 장치 없음

- 근거: [AuthService.java:110](../src/main/java/com/achat/auth/AuthService.java#L110), [JwtProvider.java:39](../src/main/java/com/achat/auth/JwtProvider.java#L39), [AuthContext.jsx:16](../achat-front/src/contexts/AuthContext.jsx#L16).
- refresh는 JWT 유효성·종류·사용자·제재 상태만 검사한다. 저장된 refresh 세션, token version, 사용 완료 기록이나 폐기 목록이 없다. 프론트 로그아웃은 브라우저 저장값만 지운다.
- H2 계정의 비밀번호 해시를 변경한 뒤 **변경 전 refresh token을 두 번 재사용했고 모두 HTTP 200**으로 새 access token을 받았다. 이는 실제 재설정 API가 아닌, 운영자가 비밀번호를 변경했을 때의 모델 검증이다.
- 영향: 현재 구조에서 복구 API만 추가하면 비밀번호를 바꿔도 탈취된 세션이 남을 수 있다.
- 조치: 사용자 token version 또는 서버 세션 상태를 access/refresh 검증에 반영하고 비밀번호 변경/복구 완료 시 세션을 폐기한다. refresh rotation과 재사용 탐지를 함께 구현한다. 복구 후 기존 세션 무효화는 [OWASP 복구 후 처리](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html#user-resets-password)도 다룬다.

### A-06 — 중간: 이메일 등록 여부 노출

- 근거: [AuthService.java:30](../src/main/java/com/achat/auth/AuthService.java#L30), [AuthService.java:42](../src/main/java/com/achat/auth/AuthService.java#L42).
- 새 이메일은 가입 200, 기존 이메일은 400과 명시적인 중복 안내를 반환하는 것을 재현했다.
- 로그인 오류 문구는 통일되어 있지만, 존재하지 않는 계정은 BCrypt 검사 전에 반환한다. 시간 차이 가능성은 코드상 확인했으며 네트워크 타이밍 공격의 성공률은 측정하지 않았다.
- 조치: 계정 존재 여부 노출 정책을 정하고 가입/로그인 제한을 우선 적용한다. 로그인에는 미존재 계정도 고정된 더미 해시를 검사하는 방식을 검토한다. 비밀번호 복구는 반드시 동일 응답을 사용한다. [OWASP 인증 오류 지침](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#authentication-and-error-messages).

## 유지된 방어와 검증 범위

- 서버에서 이메일 형식·가입 비밀번호 길이를 검사한다.
- 비밀번호는 BCrypt로 해시 저장한다.
- 이메일을 소문자로 정규화하고 DB 유일 제약으로 같은 이메일 중복을 막는다.
- 잘못된 비밀번호와 미존재 계정의 로그인 문구를 통일한다.
- 로그인 및 refresh에서 정지 계정을 검사한다.

이 장치들은 자동화 방지나 계정 복구를 대신하지 않는다. 가입 요청 제한과 로그인 제한 부재는 이전부터 있었고, 이메일 소유 확인 없이 즉시 활동할 수 있어 대량 가입 후 활동의 장벽이 낮아졌다.

## 재현 결과와 실행법

`authSecurityAudit` **7개 현상 재현 성공**. 테스트 성공은 **취약점이 재현되었다는 의미이며 보안 적합 판정이 아니다**. 정상 동작을 요구하는 기존 회귀 테스트와 분리해 opt-in 작업으로 만들었다. 보완 후에는 현재 취약 동작을 기대하는 감사 테스트를 수정하거나 폐기하고, 제한·만료·경쟁·우회 방지를 기대하는 회귀 테스트로 교체해야 한다.

```powershell
.\gradlew.bat --init-script scripts/security-audit/init.gradle authSecurityAudit --console=plain
```

감사 작업은 시스템 속성으로 테스트 전용 H2 URL/드라이버/자격증명을 강제하여 실제 DB 설정을 사용하지 않는다. 기본 자격증명과 발급 토큰을 출력하지 않는다. HTTP 서버 포트를 열지 않는 MockMvc 방식으로 검사하며 외부 네트워크 부하 테스트는 수행하지 않는다.

- 재현 소스: [AuthSecurityAuditTests.java](../scripts/security-audit/java/com/achat/AuthSecurityAuditTests.java)
- 전용 실행 설정: [init.gradle](../scripts/security-audit/init.gradle)
- 실행 결과: `build/reports/tests/authSecurityAudit/index.html`
- 원시 결과: `build/test-results/authSecurityAudit/TEST-com.achat.AuthSecurityAuditTests.xml`

이번 작업에서 추가한 파일은 이 문서와 위 두 감사 실행 파일이며, README에 문서 링크를 추가했다. 프로덕션 Java/React 코드는 변경하지 않았다. 우선순위는 **기본 관리자 처리 → 가입/로그인 제한 → 복구 수단 및 세션 폐기 구현 → 존재 여부 노출 정책 보완**이다.
