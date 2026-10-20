import { memo, useEffect, useRef, useState } from 'react';
import { AUGMENT_NAMES, AUGMENT_ICONS, likeStage, messageEffects, messageText, secondsRemaining } from './effects.js';
import { useEffectClock } from './clock.js';
import './AugmentEffects.css';

export function Particles({ hearts = false }) {
  return <span className={`effect-particles ${hearts ? 'hearts' : ''}`} aria-hidden="true">
    {Array.from({ length: 7 }, (_, index) => <i key={index} style={{ '--particle': index }}>{hearts ? '♥' : '✦'}</i>)}
  </span>;
}

function OpenableMessage({ type, text, id }) {
  const [opened, setOpened] = useState(false);
  const ice = type === 'ICE_SEAL';
  return <div className={`openable-message ${ice ? 'ice-seal' : 'sealed-message'} ${opened ? 'opened' : ''}`}>
    <button type="button" className="effect-cover" aria-expanded={opened} aria-controls={`revealed-${id}`}
      aria-label={`${AUGMENT_NAMES[type]} ${opened ? '열림' : '열기'}`} onClick={() => setOpened(true)} disabled={opened}>
      <span className="seal-art" aria-hidden="true">{ice ? '❄' : '✉'}</span>
      <span>{opened ? '메시지가 열렸어요' : ice ? '터치해서 얼음을 깨보세요' : '봉투를 열어보세요'}</span>
      {ice && <span className="ice-shards" aria-hidden="true">{Array.from({ length: 6 }, (_, i) => <i key={i} style={{ '--particle': i }} />)}</span>}
    </button>
    <div id={`revealed-${id}`} className="revealed-content" hidden={!opened}>{text}</div>
  </div>;
}

function TimedMessage({ effect, message, onReveal }) {
  const deadline = effect.revealAt || effect.expiresAt;
  const now = useEffectClock(deadline);
  const seconds = secondsRemaining(deadline, now);
  const requested = useRef(false);
  const text = messageText(message);
  useEffect(() => {
    if (effect.type === 'TIME_CAPSULE' && !seconds && message.content == null && onReveal && !requested.current) {
      requested.current = true; onReveal(message.id);
    }
  }, [effect.type, seconds, message.id, message.content, onReveal]);
  if (effect.type === 'TIME_CAPSULE') {
    const revealed = message.content != null && (effect.status === 'REVEALED' || !seconds);
    return <div className={`time-capsule ${revealed ? 'revealed' : ''}`}>
      {revealed ? <><span className="effect-caption">⌛ 타임 캡슐 공개</span><div>{text}</div></>
        : <><span className="capsule-lock" aria-hidden="true">⌛</span><strong>{seconds ? `${seconds}초 후 공개` : '공개 확인 중…'}</strong>
          {!seconds && onReveal && <button type="button" className="text-button" onClick={() => onReveal(message.id)}>다시 확인</button>}</>}
    </div>;
  }
  if (effect.type === 'TIME_BOMB') {
    const label = { SUCCESS: `답장 성공${effect.points ? ` · +${effect.points}점` : ''}`, EXPLODED: '시간 초과 · 폭발',
      CANCELLED: '구간 종료', ACTIVE: seconds ? `${seconds}초 안에 답장해주세요` : '서버 판정 대기 중' }[effect.status];
    return <div className={`time-bomb ${effect.status.toLowerCase()}`}><span className="effect-caption"><span className="bomb-fuse" aria-hidden="true">◉</span> {label}</span><div>{text}</div>{effect.status === 'EXPLODED' && <Particles />}</div>;
  }
  if (effect.type === 'DOPPELGANGER') return <div className="doppelganger"><div>{text}</div>{!seconds && <div className="message-afterimage" aria-hidden="true"><small>잔상</small>{text}</div>}</div>;
  return <div className={`main-character ${effect.status === 'ACTIVE' && seconds ? 'in-spotlight' : ''}`}>
    {effect.status === 'ACTIVE' && seconds > 0 && <><span className="effect-caption">♛ 오늘의 주인공</span><strong className="character-name">{message.senderName || '참여자'}</strong><Particles /></>}
    <div>{text}</div>
  </div>;
}

function GhostMessage({ text }) {
  const [clear, setClear] = useState(false);
  return <button type="button" className={`ghost-message ${clear ? 'clear' : ''}`} aria-label="유령 메시지 선명하게 보기"
    aria-pressed={clear} onClick={() => setClear(!clear)}><span className="ghost-aura" aria-hidden="true">♧</span><span className="ghost-text">{text}</span></button>;
}

function EffectContent({ effect, message, onReveal }) {
  const text = messageText(message);
  if (['SEALED_MESSAGE', 'ICE_SEAL'].includes(effect.type)) return <OpenableMessage type={effect.type} text={text} id={message.id} />;
  if (['TIME_CAPSULE', 'TIME_BOMB', 'DOPPELGANGER', 'MAIN_CHARACTER'].includes(effect.type)) return <TimedMessage effect={effect} message={message} onReveal={onReveal} />;
  if (effect.type === 'GHOST_MESSAGE') return <GhostMessage text={text} />;
  if (effect.type === 'ECHO_BURST') {
    const [first, ...rest] = text.split('\n');
    return <><strong className="echo-first-line">{first}</strong>{rest.length > 0 && <div>{rest.join('\n')}</div>}</>;
  }
  if (effect.type === 'JACKPOT_MESSENGER') return <div className={`jackpot-message ${effect.status === 'WON' ? 'won' : ''}`}>
    <div className="slot-machine" aria-label={effect.status === 'WON' ? '잭팟 당첨' : effect.status === 'LIMIT' ? '보상 추첨 제한' : '당첨되지 않음'}>
      {[0, 1, 2].map((i) => <span key={i} style={{ '--reel': i }}>{effect.status === 'WON' ? 7 : ((effect.value || 0) + i * 3) % 10}</span>)}
      <small>{effect.points ? `+${effect.points}점` : effect.status === 'LIMIT' ? '추첨 제한' : '다음 기회에!'}</small>
    </div><div>{text}</div>{effect.status === 'WON' && <Particles />}</div>;
  if (effect.type === 'FATE_DICE') return <div className={`dice-message dice-${effect.value}`}>
    <div className="dice-result"><span className="dice-face" aria-label={`주사위 ${effect.value}`}>{String.fromCodePoint(0x267f + Math.max(1, Math.min(6, effect.value || 1)))}</span>
      <span>{['', '평범한 하루', '새로운 색', '한마디의 무게', '반짝이는 대화', '행운의 한마디', '특별한 잭팟'][effect.value]}
        {effect.points > 0 && <strong> +{effect.points}점</strong>}{effect.status === 'NO_REWARD' && <small> · 보상 제한</small>}</span></div>
    <div>{text}</div>{[4, 6].includes(effect.value) && <Particles />}</div>;
  if (effect.type === 'FORBIDDEN_WORD') return <><div className={`forbidden-status ${effect.status.toLowerCase()}`}>금칙어: <strong>{effect.detail}</strong> · {effect.progress}/5
    {effect.points !== 0 && effect.points != null && <b> · {effect.points > 0 ? '+' : ''}{effect.points}점</b>}
    {effect.status === 'SUCCESS' && ' · 미션 성공'}{effect.status === 'FAILED' && ' · 미션 종료'}</div><div>{text}</div></>;
  if (effect.type === 'LIKE_OVERDRIVE') return <div className={`like-overdrive like-stage-${likeStage(message.likeCount || 0)}`}>
    <div>{text}</div>{message.likeCount >= 1 && <Particles hearts />}<span className="effect-caption">♥ {message.likeCount || 0}/5 {effect.points > 0 && `· +${effect.points}점 지급 완료`}</span></div>;
  if (effect.type === 'QUEST_GIVER') return <><div>{text}</div><div className="quest-card"><strong>⚑ {effect.detail === 'LIKE_QUEST' ? '공감 미션' : '답장 미션'}</strong>
    <span>{effect.progress || 0}/{effect.target || 0}명 · 작성자 {effect.value || 0}코인, 참여자 {Math.floor((effect.value || 0) / 2)}코인</span>
    <small>{effect.status === 'COMPLETE' ? '완료 · 보상 지급' : effect.status === 'CANCELLED' ? '구간 종료' : '서로 다른 참여자의 참여가 필요해요'}</small></div></>;
  return <div className={`presentation-effect effect-${effect.type.toLowerCase()}`}><div>{text}</div>
    {['VISUAL_SPELL', 'SPOTLIGHT', 'CHAOS_ROULETTE'].includes(effect.type) && <Particles />}</div>;
}

const AugmentRenderer = memo(function AugmentRenderer({ message, onReveal, preview = false }) {
  const effect = messageEffects(message)[0];
  return <div className={`message-bubble augmented-bubble ${effect ? `augment-${effect.type.toLowerCase()}` : ''} ${preview ? 'effect-preview' : ''}`} data-augment={effect?.type}>
    {effect && <span className="augment-label"><span aria-hidden="true">{AUGMENT_ICONS[effect.type]}</span> {AUGMENT_NAMES[effect.type]}</span>}
    {effect ? <EffectContent effect={effect} message={message} onReveal={onReveal} /> : messageText(message)}
  </div>;
});
export default AugmentRenderer;
