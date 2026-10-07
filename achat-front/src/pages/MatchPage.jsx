import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Users, LoaderCircle, Trophy } from 'lucide-react';
import { api } from '../lib/api.js';
import { useAuth } from '../contexts/AuthContext.jsx';
import { useToast } from '../contexts/ToastContext.jsx';
import { tiers } from '../lib/format.js';
import { ActionButton, PageHeading } from '../components/UI.jsx';

export default function MatchPage() {
  const { user } = useAuth(); const [count, setCount] = useState(6); const [status, setStatus] = useState(null); const [error, setError] = useState(''); const notify = useToast(); const navigate = useNavigate();
  useEffect(() => { const controller = new AbortController(); let timer; async function poll() { try { const next = await api.request('/match/status', { signal: controller.signal }); if (!controller.signal.aborted) { setStatus(next); setError(''); if (next.matched && next.roomId) { navigate(`/rooms/${next.roomId}`, { replace: true }); return; } } } catch (error) { if (error.name !== 'AbortError') setError(error.message); } if (!controller.signal.aborted) timer = setTimeout(poll, 3000); } poll(); return () => { controller.abort(); clearTimeout(timer); }; }, [navigate]);
  const waiting = status?.waiting;
  return <><PageHeading eyebrow="A CONVERSATION, A CHALLENGE" title="새로운 만남, 새로운 라운드" description="같은 목표를 가진 사람들과 대화하며 성장해요." /><section className="panel match-card"><div className={`match-orbit ${waiting ? 'waiting' : ''}`}><Sparkles size={53} /><span /><span /></div><span className="eyebrow">RANKED CONVERSATION</span><h2>{waiting ? '함께할 사람을 찾고 있어요' : '당신의 이야기가 빛날 차례'}</h2><p className="muted">{waiting ? '인원이 모이면 자동으로 채팅방에 입장합니다.' : '참여 인원을 선택하고 새로운 대화 라운드를 시작하세요.'}</p><div className="match-tier"><Trophy size={19} />{tiers[user.tier] || user.tier}<strong>{user.rp} RP</strong></div>{error && <p className="form-error" role="alert">{error}</p>}{waiting ? <><div className="queue-status"><LoaderCircle className="spin" size={19} /><span>대기 인원 {status.queueSize ?? 0}명 · {status.waitTime ?? 0}초 대기</span></div><ActionButton className="button secondary" action={() => api.request('/match/leave', { method: 'POST' })} onSuccess={() => { setStatus({ waiting: false }); notify('매칭 대기를 취소했어요.'); }}>대기 취소</ActionButton></> : <><div className="player-options">{[6, 8, 10, 12].map((value) => <button key={value} className={count === value ? 'selected' : ''} onClick={() => setCount(value)} aria-pressed={count === value}><Users size={20} /><strong>{value}명</strong></button>)}</div><ActionButton disabled={!status || Boolean(error)} action={() => api.request(`/match/join?playerCount=${count}`, { method: 'POST' })} onSuccess={(result) => setStatus({ waiting: true, queueSize: result.queueSize, waitTime: 0 })}><Sparkles size={18} />새로운 대화 찾기</ActionButton></>}<p className="match-note">라운드에 참여해서 경험치, 코인, 랭크 포인트를 모아보세요.</p></section></>;
}
