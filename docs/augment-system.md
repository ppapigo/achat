# 증강 시스템 구현·검증 기록

2026-10-20. Spring Boot / Java 21 / React / MySQL / STOMP.

## 선택 구간과 서버 권한

기존 **20초 선택 → 대화**를 3회 반복하는 흐름을 유지한다. 사용자가 확인한 범위에 따라 각 선택·대화 구간마다 새 `roundId`를 만들며, 한 게임의 세 구간은 같은 `gameId`를 사용한다.

- 같은 방의 자신을 제외한 한 사람에게 제시된 증강 하나를 보낸다.
- 구간마다 보내기 최대 1회, 받기 최대 1회이며 확정 후 변경할 수 없다.
- 상대가 퇴장해도 확정 기록을 되돌리지 않으므로 재입장으로 기회를 재사용할 수 없다.
- 옵션 조회를 반복해도 같은 구간의 옵션은 바뀌지 않는다. 프리랜서는 기존대로 4개, 나머지는 3개를 받는다.
- 선택 마감 때 자동 선택은 수동 선택과 같은 검증·예약 경로를 사용한다. 비어 있는 다른 상대가 없으면 건너뛴다.
- 다음 선택 구간에서 이전 예약을 초기화한다. 이전 `roundId`, 만료된 선택, 이미 선택한 사람, 이미 점유된 대상은 409이다. 자기 자신·다른 방·제시되지 않은 증강은 400이다.
- 확률·점수·시각·금칙어·주사위 결과를 클라이언트가 전송하거나 결정하지 않는다.

## 신규 12종

| 증강 | 서버·저장 동작 | JSX·CSS 표시 |
|---|---|---|
| 비밀 봉투 `SEALED_MESSAGE` | 메시지 하나와 효과 종류 저장. 시각적 숨김이며 보안 기능이 아니다. | 메시지마다 독립적인 봉투 개봉. Enter/Space·클릭·터치 지원 |
| 타임 캡슐 `TIME_CAPSULE` | 생성 시각 +10초를 저장. 공개 전 REST·WS의 본문을 null로 투영하고 답장·공감을 차단. 만료 작업이 공개 이벤트 발송 | 서버 시간에 맞춘 카운트다운, 공개 애니메이션, 이벤트 누락 시 단건 조회 |
| 시한폭탄 `TIME_BOMB` | +15초까지 같은 방·같은 구간의 다른 사람이 답장하면 성공. 점수 인정 메시지 작성자 +10점, 구간 최대 +50점. 중복 지급 불가. 만료는 EXPLODED, 조기 구간 종료는 CANCELLED | 폭탄·남은 시간·서버 성공/폭발 결과 |
| 잭팟 메신저 `JACKPOT_MESSENGER` | 서버 10% 추첨, +10점. 점수 인정·내용 중복 제외·5초 간격·구간 최대 10회 추첨/3회 당첨 | 슬롯 회전, 당첨 파티클, 실제 지급 점수/제한 표시 |
| 금칙어 저주 `FORBIDDEN_WORD` | 안전한 목록에서 한 단어 저장. 다음 5개 수락 메시지를 NFKC·소문자·공백/형식문자 제거 후 검사. 위반당 -3점, 총 -15점까지. 모두 피하면 +15점 한 번 | 선택 패널에 받은 금칙어, 메시지에 5개 진행도·실제 판정 |
| 유령 메시지 `GHOST_MESSAGE` | 시각 효과 메타데이터 저장 | 반투명 텍스트·유령 배경·잔잔한 흔들림. hover/focus/터치로 선명하게 |
| 무중력 채팅 `ZERO_GRAVITY` | 시각 효과 메타데이터 저장 | transform으로 부드럽게 부유, hover/focus 정지. 문서 흐름 유지 |
| 도플갱어 `DOPPELGANGER` | 생성 시각 +2초 기록. 복제 메시지는 생성하지 않음 | 같은 말풍선 안에 반투명 '잔상'. 별도 액션·공감·점수 없음 |
| 공감 폭주 `LIKE_OVERDRIVE` | 서로 다른 5명의 공감을 처음 달성한 점수 인정 메시지 +10점. 메시지별 1회, 구간 최대 3회. 공감 취소·재클릭은 재지급하지 않음 | 1개 하트, 3개 강조, 5개 특별 애니메이션 |
| 운명의 주사위 `FATE_DICE` | 서버 1~6 결정·범위 검증·저장. 5는 +5점, 6은 +10점. 보상은 잭팟과 같은 점수 인정/중복/간격/10회 제한, 구간 총 +50점 | 1 일반, 2 색상, 3 강조, 4 파티클, 5 보너스 표시, 6 잭팟 연출 |
| 주인공 모드 `MAIN_CHARACTER` | 첫 수락 메시지에만 +6초 시각 저장. 후속 메시지는 SPENT. 실제 메시지는 한 개 | 닉네임과 메시지를 금색 카드로 6초 강조한 뒤 일반 표시 |
| 얼음 봉인 `ICE_SEAL` | 메시지 하나와 시각 효과 저장. 실제 전송·수신은 정상 | 결정 덮개·파편·공개. Enter/Space·터치 지원 |

보상 숫자는 `AugmentPolicy`에서 관리한다. 점수 인정 조건은 기존 `ScorePolicy`의 길이·5초 간격·중복·연속 발언·게임당 개수 제한을 따른다. `clientMessageId`가 같은 재전송은 저장된 원본을 반환하므로 메시지 수, 추첨, 미션, 점수를 다시 처리하지 않는다. ID가 달라도 보상 추첨에는 정규화한 내용 해시와 간격·횟수 상한을 적용한다. 총점은 0점 미만으로 내려가지 않는다.

## 기존 15종 점검

| 증강 | 유지·개선 결과 |
|---|---|
| 리버스 `REVERSE_TEXT` | 기존 서버 문자열 역순 변환 유지, 변환 결과 저장 |
| 캡스락 `CAPS_LOCK` | 대문자 변환 유지, Locale.ROOT 적용 |
| 속삭임 `WHISPER` | 소문자 변환 유지, Locale.ROOT 적용 |
| 터보 캐스터 `TURBO_CASTER` | 실제 쿨타임 배율 0.6 유지, 설명과 일치 |
| 에코 버스트 `ECHO_BURST` | 문자열 장식 제거, 첫 줄을 실제 `<strong>` 구조로 강조 |
| 비주얼 스펠 `VISUAL_SPELL` | CSS 파티클 구현 |
| 쓰레드 위버 `THREAD_WEAVER` | 답장·원본 DOM 위치 사이 SVG 연결선, 리사이즈 반영 |
| 그림자 화자 `SHADOW_SPEAKER` | REST·WS·답장 미리보기에서 senderId/칭호 제거 및 익명 표시. 아바타도 익명 처리 |
| 스팟라이트 `SPOTLIGHT` | 강조 말풍선 및 채팅 상단 링크 고정 |
| 링크 브리지 `LINK_BRIDGE` | 인용 카드·SVG 연결선, 원본으로 이동하고 키보드 포커스 |
| 하모니 오라 `HARMONY_AURA` | 기존 단어 치환·반복 표현 완화 유지. 모든 공격적 표현을 인식하는 분류기는 아님 |
| 카오스 룰렛 `CHAOS_ROULETTE` | 기존 서버 무작위 텍스트 변형 유지. 저장된 결과 재사용, 파티클 추가 |
| 퀘스트 기버 `QUEST_GIVER` | 표시와 서버 미션 종류 일치, 답장/공감 실제 진행. 퀘스트·참여자·완료 여부 영속화. 같은 참여자는 한 번, 완료 보상도 한 번 |
| 타임 시프터 `TIME_SHIFTER` | 공감 받은 메시지를 상단 표시. 설명과 무관했던 쿨타임 변경 제거 |
| 인사이트 드롭 `INSIGHT_DROP` | 구간·게임 종료 때 공감순/시간순 최대 3개 하이라이트 발송, 기록 저장·재조회 |

상단 고정 영역은 현재 화면에 있는 메시지 중 해당 구간의 최신 2개까지 표시한다. 연결선은 현재 로드된 원본과 답장이 모두 있을 때 그린다. 최근 메시지 조회는 기존대로 최대 100개다.

## 동시성·트랜잭션

기존 `ConcurrentHashMap`과 사용자별 `synchronized select()`는 A와 B가 동시에 C의 비어 있는 상태를 검사하는 작업을 묶지 못했다. 두 사용자의 서로 다른 락 아래에서 검사와 선택이 모두 성공할 수 있었다.

`RoomExecution`은 방별 재진입 락을 사용한다. **멤버·구간·기한 확인 → 옵션 확인 → 발신자/대상 확인 → DB 진행 상태 저장 → 발신자/대상 예약 → 커밋** 전체를 같은 임계 구역에서 실행한다. 기존 트랜잭션에 참여해도 락은 DB 커밋/롤백까지 유지한다.

- 옵션 생성, 수동/자동 확정, 라운드 전환, 입퇴장, 메시지·공감·시간 만료 처리를 같은 방 락으로 직렬화한다.
- A/B→C 경합에서 하나만 성공하고 나머지는 409로 실패한다. 실패한 사람의 선택은 소모하지 않는다.
- 선택 예약은 DB 오류 시 복원한다. 라운드 단계·점수·스팸 방지 기록도 트랜잭션별 최초 스냅샷으로 복원한다.
- 방송과 다음 단계 예약은 커밋 이후에 실행한다. 라운드 스케줄러는 캡처한 구간 ID/단계/마감 시각을 현재 값과 비교하여 오래된 작업을 무시한다. 처리 오류 시 1초 뒤 같은 조건으로 재시도한다.
- 보상 영수증 ID는 `messageId:판정종류`이며, 보상 상태와 메시지 결과를 같은 트랜잭션에 저장한다.
- 기존 `/likes` 이벤트를 유지하면서 revision과 userId 별칭을 추가했다. 프론트는 늦은 공감·기록 이벤트가 최신 효과를 덮지 못하도록 revision을 비교한다.

## API·WebSocket

기존 경로와 `appliedContent`, `appliedAugment` 등 기존 메시지 필드를 유지했다. 선택 POST에는 오래된 요청 차단을 위해 `roundId`가 필수다. 이전 클라이언트는 이 필드를 추가해야 한다.

```json
{
  "type": "TIME_BOMB",
  "targetUserId": "다른-참가자-ID",
  "roundId": "옵션-응답에서-받은-구간-ID"
}
```

- `GET /rooms/{roomId}/augments/options`: 기존 옵션과 선택값 + roundId, selectionEndsAt, serverTime, canSelect, targets[].available, receivedAugment, forbiddenWord.
- `POST /rooms/{roomId}/augments/pick`: 위 요청, 409면 옵션을 갱신하고 다른 대상을 선택할 수 있다.
- `GET /rooms/{roomId}/messages?limit=100`: 구조화된 효과와 최신 공감/퀘스트 상태를 포함한다.
- `GET /rooms/{roomId}/messages/{messageId}`: 같은 방 멤버에게 단건 상태 조회.
- `GET /rooms/{roomId}/messages/highlights`: 저장된 최근 하이라이트 최대 3개.
- 메시지 DTO 추가: `senderName`, `anonymous`, `augmentEffects[]`, `revision`, `serverTime`, `roundId`.
- 메시지 전송 추가: 선택적 `clientMessageId` (영문/숫자/_/- 1~64자). 새 프론트는 UUID를 사용한다.
- 기존 `/topic/rooms/{id}/messages`, `/likes`, `/events`, `/round`, `/round/end` 유지.
- 추가: `/message-effects` 전체 메시지 상태, `/augments` 옵션 갱신 신호, `/round/highlights`. 기존 `/quest`도 구독한다.
- STOMP는 멤버만 방 토픽에 접근한다. 클라이언트가 `/topic`으로 SEND하여 서버 결과를 위조하지 못하도록 허용 목적지를 제한한다.

모든 조회·방송은 같은 `MessagePresenter`를 사용한다. HTML 문자열을 생성하거나 실행하지 않고 React 텍스트/JSX로 렌더링한다. 시간 효과는 저장된 서버 UTC 시각과 응답의 serverTime을 사용하며, 브라우저의 공유 타이머는 표현만 담당한다. 봉투·얼음의 개인 열림 상태는 해당 메시지 컴포넌트의 로컬 상태라 새로고침하면 다시 닫힌다.

## 저장과 적용

이번 작업에서 실제 MySQL 스키마 변경, 데이터 초기화·삭제, 운영 배포, Git push는 실행하지 않았다.

JPA 변경은 추가 중심이다. 배포 담당자는 별도 환경에서 아래 추가 항목을 검토해야 한다. 기존 개발 설정의 `DB_DDL_AUTO=update`는 애플리케이션을 시작할 때 이 매핑을 반영할 수 있다.

- `chat_messages`: game_id, round_id, client_message_id, augment_effects(TEXT JSON), revision, next_effect_at, highlighted_at, reward_eligible.
- `(room_id,sender_id,client_message_id)` 유일 제약, next_effect_at 조회 인덱스. 기존 메시지의 새 컬럼은 null 허용이며 변환기가 빈 효과/0 revision으로 처리한다.
- `augment_progress`, `augment_reward_content`: 구간·수신자별 효과, 미션/추첨 횟수, 정규화 내용 해시.
- `augment_awards`: 메시지별 보상 영수증 및 gameId/roundId/사용자/점수.
- `chat_quests`, `chat_quest_participants`: 기존 퀘스트 진행과 완료 영수증.

효과 메타데이터·추첨 결과·시각·보상 영수증·퀘스트는 DB에서 복원한다. 타임 캡슐 공개 작업은 재시작 후에도 next_effect_at를 읽어 진행한다. 진행 중 게임 자체의 재개 범위는 아래 제약을 따른다.

## 자동 검증과 수동 시나리오

실제 서비스 DB 대신 명시적으로 격리한 H2 MySQL 호환 메모리 DB 및 재시작 검사용 임시 파일 DB에서 실행했다.

최종 실행 결과:

| 검증 | 결과 |
|---|---|
| 백엔드 전체 테스트 | 91개, 실패 0·오류 0 |
| Java 21 `bootJar` | 성공, `build/libs/achat-0.0.1-SNAPSHOT.jar` |
| 프론트 Vitest | 15개 통과 |
| ESLint / Vite 운영 빌드 | 모두 성공 |
| 증강 브라우저 검사 | PC·모바일 각각 27종, 오류 0 |
| 기존 화면 검사 | 24개 화면·기기 조합 통과 |
| 가입·로그인 회귀 | PC·모바일 통과, 이메일 인증 UI/요청 없음 |
| 실제 STOMP 통합 | JWT 연결·메시지·답장·효과 갱신·DB 일치 통과 |
| 변경 파일 공백 검사 | `git diff --check` 통과 |

서로 다른 Spring 테스트 컨텍스트가 같은 H2 스키마를 지우지 않도록 기본 DB 이름에 `${random.uuid}`를 포함했다. 선택 경쟁 외에 동일 요청 ID의 동시 재전송, 5번째·6번째 공감 동시 도착도 각각 보상 한 번만 지급되는지 검증한다. 프론트는 늦게 도착한 이전 라운드 조회 응답과 이전 방의 단건 조회 응답을 무시한다.

```powershell
$env:TEST_DB_URL = 'jdbc:h2:mem:achat-final-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=-1'
$env:TEST_DB_USERNAME = 'sa'
$env:TEST_DB_PASSWORD = ''
$env:TEST_DB_DRIVER = 'org.h2.Driver'
.\gradlew.bat test bootJar
npm test
npm run lint
npm run build
```

백엔드 검증은 자기 자신/다른 방/중복/동시 대상 선택, 실패 후 재시도, 동시 옵션 조회, 수동·자동 경쟁, 라운드 초기화 경쟁, 오래된 요청·스케줄러, 만료·대상 없음, 트랜잭션 롤백을 포함한다. 신규 12종과 기존 15종을 각각 검증하고, 시간 판정·재전송·추첨 제한·금칙어·공감 취소 재클릭·퀘스트 완료 보상·게임 종료 보상·기록 복원도 확인한다. 별도 테스트는 실제 서버 임의 포트에서 JWT/STOMP 클라이언트 두 개로 시한폭탄 전송→답장→성공 이벤트 및 DB 일치를 검증한다.

브라우저 효과 검사는 일반 Vite 서버를 사용한다. REST·STOMP 응답을 테스트에서 제어하므로 실제 DB/계정은 변경하지 않는다.

```powershell
# 터미널 1
npm run dev --workspace achat-front -- --port 5177
# 터미널 2
npm run test:augments
```

PC 1440×1000 / 모바일 390×844에서 27종 렌더링, XSS 비실행, 키보드 개봉, 터치, SVG 연결, 상단 표시, 409 재시도, 실제 STOMP 형식의 갱신, 새로고침, reduced-motion, 가로 넘침을 검사한다. `achat-front/test-results/augments/`에 report.json과 PNG가 생성된다. 브라우저 모의 서버 검사와 실제 백엔드 STOMP 검사는 별도로 수행한다.

수동 재현:

1. 세 계정을 같은 방에 입장시키고 방장이 게임을 시작한다. 각자 옵션과 대상 목록을 확인한다.
2. A와 B가 C를 동시에 확정한다. 한 명만 성공하고 다른 사람은 409 안내 뒤 다른 상대에게 보낼 수 있어야 한다.
3. 마감까지 선택하지 않은 계정의 자동 선택을 확인한다. 두 번째 선택 구간에서 새 옵션/대상을 확인하고 이전 roundId 요청은 거절되는지 확인한다.
4. 효과를 받은 계정이 메시지를 보내고, 받지 않은 계정은 일반 메시지를 보낸다. 봉투/얼음은 키보드·터치로 열고 잔상이 실제 메시지 수를 늘리지 않는지 확인한다.
5. 캡슐 전송 후 다른 브라우저를 새로고침한다. 동일 공개 시각과 공개 전 빈 본문을 확인한다. 폭탄은 15초 안/밖 답장과 구간 종료를 각각 확인한다.
6. 최소 6계정으로 공감 5개를 달성한 뒤 취소·재클릭한다. 이미 지급된 보상은 다시 늘어나지 않아야 한다.
7. 금칙어의 대문자·공백 변형과 5개 이후 메시지를 확인한다. 잭팟/주사위는 재전송 ID와 반복 내용으로 추가 지급되지 않는지 확인한다.
8. 그림자 메시지의 REST·WS JSON과 답장 미리보기에 원래 senderId가 없는지 확인한다. 마지막 구간 종료 후 하이라이트·보상·기록을 새로 조회한다.

## 남은 제약

- **단일 서버 기준**이다. 방 락·진행 중 라운드·선택 옵션·쿨타임은 JVM 메모리이며 다중 인스턴스 사이에 공유되지 않는다. 여러 서버로 확장하려면 분산 락 또는 DB 구간/발신자/수신자 예약 테이블과 원자적 갱신, 공유 라운드 상태·브로커가 필요하다.
- 서버 재시작 시 저장된 메시지 효과·보상 영수증은 유지되지만 진행 중 게임/옵션은 재개하지 않는다. 이전 구간 답장으로 새 보상을 얻지 못한다. 완료되지 않은 폭탄은 만료 작업으로 정리된다.
- 익명 메시지 DTO에서 직접 식별자를 제거했으나, 방 참가자 목록·점수판 변화·발언 시점·내용·증강을 보낸 사람의 지식으로 작성자를 추론할 수 있다. **추적 불가능한 완전 익명성은 제공하지 않는다.**
- 방 락 객체는 같은 방에 서로 다른 락이 생기는 문제를 막기 위해 프로세스 동안 유지한다. 방 수가 매우 많은 장기 운영에서는 참조 횟수를 고려한 수명 관리가 필요하다.
- 실제 MySQL에서 이번 스키마의 DDL/잠금 동작, 여러 JVM, 실기기 Safari/Firefox, 대규모 부하 테스트는 수행하지 않았다. H2와 Edge의 데스크톱/모바일 뷰포트 검증 범위다.

## 증강 작업 변경 파일

앞선 이메일 인증 제거 변경은 별도이며 그대로 유지했다. 다음은 이번 증강 작업의 파일 목록이다.

| 영역 | 수정·추가 파일 |
|---|---|
| 선택·상태 | `src/main/java/com/achat/augment/{AugmentDefinition,AugmentService,AugmentStore,AugmentType,UserAugmentState,AugmentConflictException,AugmentPolicy,AugmentRandom,AugmentProgress,AugmentProgressRepository,AugmentAward,AugmentAwardRepository}.java` |
| 선택 DTO | `src/main/java/com/achat/augment/dto/{AugmentOptionResponse,PickAugmentRequest}.java` |
| 효과 기반·신규 | `src/main/java/com/achat/augment/effect/{AugmentEffectRegistry,MessageEffect,MessageEffectHandler,MessageEffectService,MessageEffectsConverter,EffectRewards,PresentationEffect,TimedMessageEffects,RandomRewardEffects,ForbiddenWordEffect}.java` |
| 기존 효과 | `src/main/java/com/achat/augment/effect/{CapsLockEffect,WhisperEffect,EchoBurstEffect,InsightDropEffect,LinkBridgeEffect,QuestGiverEffect,ShadowSpeakerEffect,SpotlightEffect,ThreadWeaverEffect,TimeShifterEffect,TurboCasterEffect,VisualSpellEffect}.java` |
| 공통·방 | `src/main/java/com/achat/common/{RoomExecution,GlobalExceptionHandler}.java`, `src/main/java/com/achat/room/RoomService.java` |
| 채팅·저장 | `src/main/java/com/achat/message/{Message,MessageRepository,MessagePresenter,MessageHistoryController,ChatMessageService}.java` |
| 메시지 DTO | `src/main/java/com/achat/message/dto/{MessageResponse,LikeResponse,SendMessageRequest}.java` |
| 퀘스트 | `src/main/java/com/achat/quest/{Quest,QuestRepository,QuestStore,QuestService}.java` |
| 라운드 | `src/main/java/com/achat/round/{Round,RoundStore,RoundService,RoundScheduler,RoundScore,RoundScoreTracker}.java`, `src/main/java/com/achat/round/dto/{RoundResponse,RoundEndResponse}.java` |
| 실시간 | `src/main/java/com/achat/websocket/{ChatController,WebSocketAuthInterceptor}.java` |
| 화면·상태 | `achat-front/src/components/AugmentPanel.jsx`, `achat-front/src/pages/ChatPage.jsx`, `achat-front/src/hooks/useChat.js`, `achat-front/src/lib/{chat.js,chat.test.js}`, `achat-front/src/preview/data.js` |
| 효과 UI | `achat-front/src/components/augments/{AugmentRenderer.jsx,AugmentPreview.jsx,ThreadConnections.jsx,AugmentEffects.css,effects.js,effects.test.js,clock.js}` |
| 백엔드 테스트 | `src/test/java/com/achat/{AugmentTestSupport,AugmentTargetingTests,AugmentEffectsTests,AugmentRewardPolicyTests,AugmentApiTests,AugmentWebSocketTests,JpaRestartPersistenceTests}.java` |
| 빌드·UI 검사·문서 | `build.gradle`, `package.json`, `scripts/{check-augments-ui.mjs,check-ui.mjs}`, `src/test/resources/application.yml`, `README.md`, `achat-front/README.md`, `docs/augment-system.md` |
