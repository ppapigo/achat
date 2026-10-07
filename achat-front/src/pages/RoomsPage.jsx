import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Search, MessageCircle, Users, ArrowUpRight } from 'lucide-react';
import { useResource } from '../hooks/useResource.js';
import { useToast } from '../contexts/ToastContext.jsx';
import { api } from '../lib/api.js';
import { Empty, Field, Modal, PageHeading, ResourceState } from '../components/UI.jsx';

export default function RoomsPage() {
  const resource = useResource('/rooms'); const notify = useToast(); const navigate = useNavigate();
  const [search, setSearch] = useState(''); const [open, setOpen] = useState(false); const [busy, setBusy] = useState(false);
  const filtered = resource.data?.filter((room) => room.name.toLowerCase().includes(search.toLowerCase())) || [];
  async function create(event) { event.preventDefault(); const form = new FormData(event.currentTarget); setBusy(true); try { const room = await api.request('/rooms', { method: 'POST', body: { name: form.get('name').trim(), maxMembers: Number(form.get('maxMembers')) } }); navigate(`/rooms/${room.id}`); } catch (error) { notify(error.message, 'error'); } finally { setBusy(false); } }
  return <><PageHeading eyebrow="FIND YOUR PEOPLE" title="대화가 되는 공간" description="취향이 통하는 방에 들어가거나, 나만의 작은 공간을 만들어보세요."><button className="button" onClick={() => setOpen(true)}><Plus size={18} />채팅방 만들기</button></PageHeading><div className="toolbar"><label className="search-input"><Search size={18} /><input aria-label="채팅방 검색" placeholder="어떤 대화를 찾고 있나요?" value={search} onChange={(event) => setSearch(event.target.value)} /></label><span className="muted">{filtered.length}개의 공간</span></div><ResourceState resource={resource}>{filtered.length ? <div className="room-grid">{filtered.map((room, index) => <Link to={`/rooms/${room.id}`} key={room.id} className="panel room-card"><div className={`room-cover cover-${index % 4}`}><span className="room-cover-pattern" /><MessageCircle size={44} strokeWidth={1.4} /><span className="room-status"><span className="status-dot" />{room.currentMembers >= room.maxMembers ? '함께하는 중' : '대화에 참여해요'}</span></div><div className="room-card-body"><h2>{room.name}</h2><p>가벼운 인사로 새로운 이야기를 시작해요.</p><div><span><Users size={17} />{room.currentMembers} / {room.maxMembers}명</span><ArrowUpRight size={21} /></div></div></Link>)}</div> : <div className="panel"><Empty title={search ? '검색한 채팅방이 없어요' : '첫 채팅방을 열어볼까요?'} description="관심 있는 주제로 사람들을 초대해보세요."><button className="button secondary" onClick={() => setOpen(true)}>새 채팅방 만들기</button></Empty></div>}</ResourceState>
  {open && <Modal title="나만의 채팅방 만들기" onClose={() => !busy && setOpen(false)}><form className="form-stack" onSubmit={create}><Field label="채팅방 이름"><input name="name" required minLength={2} maxLength={80} placeholder="예: 퇴근 후 가벼운 수다" autoFocus /></Field><Field label="최대 참여 인원"><select name="maxMembers" defaultValue="10">{[2, 4, 6, 8, 10].map((value) => <option key={value} value={value}>{value}명</option>)}</select></Field><button className="button wide" disabled={busy}>채팅방 만들기</button></form></Modal>}</>;
}
