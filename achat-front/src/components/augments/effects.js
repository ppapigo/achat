export const AUGMENT_NAMES = {
  REVERSE_TEXT: '리버스', CAPS_LOCK: '캡스락', WHISPER: '속삭임', TURBO_CASTER: '터보 캐스터',
  ECHO_BURST: '에코 버스트', VISUAL_SPELL: '비주얼 스펠', THREAD_WEAVER: '쓰레드 위버',
  SHADOW_SPEAKER: '그림자 화자', SPOTLIGHT: '스팟라이트', LINK_BRIDGE: '링크 브리지',
  HARMONY_AURA: '하모니 오라', CHAOS_ROULETTE: '카오스 룰렛', QUEST_GIVER: '퀘스트 기버',
  TIME_SHIFTER: '타임 시프터', INSIGHT_DROP: '인사이트 드롭', SEALED_MESSAGE: '비밀 봉투',
  TIME_CAPSULE: '타임 캡슐', TIME_BOMB: '시한폭탄', JACKPOT_MESSENGER: '잭팟 메신저',
  FORBIDDEN_WORD: '금칙어 저주', GHOST_MESSAGE: '유령 메시지', ZERO_GRAVITY: '무중력 채팅',
  DOPPELGANGER: '도플갱어', LIKE_OVERDRIVE: '공감 폭주', FATE_DICE: '운명의 주사위',
  MAIN_CHARACTER: '주인공 모드', ICE_SEAL: '얼음 봉인',
};
export const AUGMENT_ICONS = {
  REVERSE_TEXT: '↔', CAPS_LOCK: 'Aa', WHISPER: 'aa', TURBO_CASTER: 'ϟ', ECHO_BURST: '◖',
  VISUAL_SPELL: '✦', THREAD_WEAVER: '⌁', SHADOW_SPEAKER: '◒', SPOTLIGHT: '☀', LINK_BRIDGE: '↗',
  HARMONY_AURA: '❀', CHAOS_ROULETTE: '◈', QUEST_GIVER: '⚑', TIME_SHIFTER: '◷', INSIGHT_DROP: '✧',
  SEALED_MESSAGE: '✉', TIME_CAPSULE: '⌛', TIME_BOMB: '◉', JACKPOT_MESSENGER: '777',
  FORBIDDEN_WORD: '⊘', GHOST_MESSAGE: '♧', ZERO_GRAVITY: '↑', DOPPELGANGER: '◌',
  LIKE_OVERDRIVE: '♥', FATE_DICE: '⚄', MAIN_CHARACTER: '♛', ICE_SEAL: '❄',
};
export function messageEffects(message) {
  if (message.augmentEffects?.length) return message.augmentEffects;
  return (message.appliedAugment || '').split(',').filter((type) => AUGMENT_NAMES[type])
    .map((type) => ({ type, status: 'ACTIVE' }));
}
export function messageText(message) {
  return message.appliedContent ?? message.content ?? '';
}
export function isAnonymous(message) {
  return message.anonymous || messageEffects(message).some((effect) => effect.type === 'SHADOW_SPEAKER');
}
export function likeStage(count) { return count >= 5 ? 5 : count >= 3 ? 3 : count >= 1 ? 1 : 0; }
export function secondsRemaining(deadline, now) {
  const end = Date.parse(deadline);
  return Number.isFinite(end) ? Math.max(0, Math.ceil((end - now) / 1000)) : 0;
}
