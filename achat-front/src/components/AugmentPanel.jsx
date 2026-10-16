import { useEffect, useState } from 'react';
import { api } from '../lib/api.js';
import { shortId } from '../lib/format.js';
import { useResource } from '../hooks/useResource.js';
import { ActionButton, Field, ResourceState } from './UI.jsx';

export default function AugmentPanel({ roomId, enabled, room, phase }) {
  const augments = useResource(enabled ? `/rooms/${roomId}/augments/options` : null);
  const [targetUserId, setTargetUserId] = useState('');
  const reload = augments.reload;
  useEffect(() => { reload(); }, [room, phase, reload]);

  const { options = [], selected, targets = [], targetUserId: selectedTargetId } = augments.data || {};
  const target = targets.find((entry) => entry.userId === targetUserId);
  const selectedTarget = targets.find((entry) => entry.userId === selectedTargetId);
  const selectedName = options.find((option) => option.type === selected)?.name || selected;

  return <div className="panel augment-panel">
    <h2>상대에게 보낼 증강</h2>
    <p className="muted">상대 한 명을 선택한 뒤 증강을 골라주세요. 선택한 상대에게만 적용돼요.</p>
    <ResourceState resource={augments}>
      {selected ? <>
        <span className="badge soft">{selectedName}</span>
        <p className="muted">적용 대상: {selectedTarget?.username || `참여자 ${shortId(selectedTargetId)}`} {!selectedTarget && '(퇴장)'}</p>
      </> : <>
        <Field label="증강을 받을 상대">
          <select value={target?.userId || ''} onChange={(event) => setTargetUserId(event.target.value)} disabled={!targets.length}>
            <option value="">{targets.length ? '상대를 선택해주세요' : '선택할 상대가 없습니다'}</option>
            {targets.map((entry) => <option key={entry.userId} value={entry.userId}>{entry.username}</option>)}
          </select>
        </Field>
        {!targets.length && <p className="muted">다른 참여자가 들어오면 증강을 보낼 수 있어요.</p>}
        {options.map((option) => <ActionButton key={option.type} className="augment-option" disabled={!target || augments.loading}
          action={async () => {
            try {
              return await api.request(`/rooms/${roomId}/augments/pick`, { method: 'POST', body: { type: option.type, targetUserId: target.userId } });
            } finally {
              reload();
            }
          }}>
          <strong>{option.name}</strong><span>{option.description}</span>
        </ActionButton>)}
      </>}
    </ResourceState>
  </div>;
}
