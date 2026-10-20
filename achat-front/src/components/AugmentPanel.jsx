import { useEffect, useState } from 'react';
import { api } from '../lib/api.js';
import { shortId } from '../lib/format.js';
import { useResource } from '../hooks/useResource.js';
import { Field, ResourceState } from './UI.jsx';
import AugmentPreview from './augments/AugmentPreview.jsx';
import { AUGMENT_ICONS, AUGMENT_NAMES } from './augments/effects.js';
import { syncServerTime } from './augments/clock.js';

export default function AugmentPanel({ roomId, enabled, room, phase, roundId, revision }) {
  const augments = useResource(enabled ? `/rooms/${roomId}/augments/options` : null);
  const [targetUserId, setTargetUserId] = useState('');
  const [chosenType, setChosenType] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const reload = augments.reload;
  useEffect(() => { reload(); }, [room, phase, roundId, revision, reload]);
  useEffect(() => { setTargetUserId(''); setChosenType(''); setError(''); }, [roundId]);
  useEffect(() => { if (augments.data?.serverTime) syncServerTime(augments.data.serverTime); }, [augments.data]);

  const { options = [], selected, targets = [], targetUserId: selectedTargetId, canSelect = false, receivedAugment, forbiddenWord } = augments.data || {};
  const target = targets.find((entry) => entry.userId === targetUserId && entry.available !== false);
  const selectedTarget = targets.find((entry) => entry.userId === selectedTargetId);
  const selectedName = options.find((option) => option.type === selected)?.name || selected;
  async function confirm() {
    if (!target || !chosenType || !canSelect || busy) return;
    setBusy(true); setError('');
    try {
      await api.request(`/rooms/${roomId}/augments/pick`, { method: 'POST',
        body: { type: chosenType, targetUserId: target.userId, roundId: augments.data.roundId } });
      setChosenType('');
    } catch (error) { setError(error.message); }
    finally { setBusy(false); reload(); }
  }

  return <div className="panel augment-panel">
    <h2>상대에게 보낼 증강</h2>
    <p className="muted">각 구간에서 한 번 보내고, 하나만 받을 수 있어요. 확정한 선택은 바꿀 수 없어요.</p>
    {receivedAugment && <div className="augment-notice">받은 증강: {AUGMENT_NAMES[receivedAugment]}
      {forbiddenWord && <div>금칙어: <strong>{forbiddenWord}</strong> · 다음 5개 메시지에서 피해보세요.</div>}</div>}
    {error && <p role="alert" className="form-error">{error}</p>}
    <ResourceState resource={augments}>
      {selected ? <>
        <span className="badge soft">{selectedName}</span>
        <p className="muted">적용 대상: {selectedTarget?.username || `참여자 ${shortId(selectedTargetId)}`} {!selectedTarget && '(퇴장)'}</p>
      </> : !canSelect ? <p className="muted">{phase === 'PLAYING' ? '선택 시간이 끝났어요. 다음 구간을 기다려주세요.' : '라운드의 증강 선택 시간에 참여할 수 있어요.'}</p> : <>
        <Field label="증강을 받을 상대">
          <select value={target?.userId || ''} onChange={(event) => setTargetUserId(event.target.value)} disabled={busy || !targets.some((entry) => entry.available !== false)}>
            <option value="">{targets.length ? '상대를 선택해주세요' : '선택할 상대가 없습니다'}</option>
            {targets.map((entry) => <option key={entry.userId} value={entry.userId} disabled={entry.available === false}>{entry.username}{entry.available === false ? ' · 이미 증강을 받음' : ''}</option>)}
          </select>
        </Field>
        {!targets.length && <p className="muted">다른 참여자가 들어오면 증강을 보낼 수 있어요.</p>}
        {targets.length > 0 && !targets.some((entry) => entry.available !== false) && <p className="muted">모든 상대가 증강을 받았어요. 이번 구간은 건너뜁니다.</p>}
        {options.map((option) => <div key={option.type} className="augment-card">
          <button type="button" className="augment-option" aria-pressed={chosenType === option.type}
            disabled={!target || busy || augments.loading} onClick={() => setChosenType(option.type)}>
            <span className="augment-option-icon" aria-hidden="true">{AUGMENT_ICONS[option.type]}</span>
            <strong>{option.name}{chosenType === option.type ? ' · 선택됨' : ''}</strong><span>{option.description}</span>
          </button><AugmentPreview type={option.type} />
        </div>)}
        <button type="button" className="button augment-pick-confirm" disabled={!target || !chosenType || busy || augments.loading} onClick={confirm}>{busy ? '확정 중…' : '증강 보내기 확정'}</button>
      </>}
    </ResourceState>
  </div>;
}
