import { useState } from 'react';
import AugmentRenderer from './AugmentRenderer.jsx';

export default function AugmentPreview({ type }) {
  const [opened, setOpened] = useState(false);
  const [startedAt, setStartedAt] = useState(0);
  const effect = { type, status: 'ACTIVE', value: type === 'FATE_DICE' ? 6 : 7, points: 0,
    detail: type === 'FORBIDDEN_WORD' ? '무지개' : 'REPLY_QUEST', progress: 1, target: 5 };
  if (type === 'JACKPOT_MESSENGER') effect.status = 'WON';
  if (['TIME_CAPSULE', 'DOPPELGANGER'].includes(type)) effect.revealAt = new Date(startedAt + 2000).toISOString();
  if (['TIME_BOMB', 'MAIN_CHARACTER'].includes(type)) effect.expiresAt = new Date(startedAt + 6000).toISOString();
  return <div className="augment-preview">
    <button type="button" className="text-button" aria-expanded={opened} onClick={() => { setOpened(!opened); setStartedAt(Date.now()); }}>효과 {opened ? '접기' : '미리보기'}</button>
    {opened && <AugmentRenderer key={startedAt} preview message={{ id: `preview-${type}`, senderName: '대화 친구', content: '반가워요!\n오늘의 이야기를 들려주세요.',
      createdAt: new Date(startedAt).toISOString(), likeCount: 5, augmentEffects: [effect] }} />}
  </div>;
}
