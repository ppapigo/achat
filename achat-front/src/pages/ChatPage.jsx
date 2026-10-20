import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Send, Heart, Reply, Sparkles, LogOut, Play, MessageCircle, X } from 'lucide-react';
import { api } from '../lib/api.js';
import { shortId, time } from '../lib/format.js';
import { useResource } from '../hooks/useResource.js';
import { useChat } from '../hooks/useChat.js';
import { useAuth } from '../contexts/AuthContext.jsx';
import { useToast } from '../contexts/ToastContext.jsx';
import { ActionButton, Avatar, Empty, ErrorState, ResourceState } from '../components/UI.jsx';
import AugmentPanel from '../components/AugmentPanel.jsx';
import AugmentRenderer from '../components/augments/AugmentRenderer.jsx';
import ThreadConnections from '../components/augments/ThreadConnections.jsx';
import { isAnonymous, messageEffects, messageText } from '../components/augments/effects.js';

function goToMessage(id) {
  const element = document.getElementById(`message-${id}`);
  if (element) { element.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'center' }); element.focus({ preventScroll: true }); }
}

export default function ChatPage() {
  const { roomId } = useParams(); const room = useResource(`/rooms/${roomId}`); const { user } = useAuth(); const notify = useToast(); const navigate = useNavigate();
  const [joinedRoom, setJoinedRoom] = useState(''); const [joinError, setJoinError] = useState(null);
  const [content, setContent] = useState(''); const [reply, setReply] = useState(null);
  const bottom = useRef(null); const messageList = useRef(null); const lastMessage = useRef(null);
  const reloadRoom = room.reload; const chat = useChat(roomId, joinedRoom === roomId, reloadRoom);
  useEffect(() => { let alive = true; setJoinError(null); api.request(`/rooms/${roomId}/join`, { method: 'POST' }).then(() => { if (alive) { setJoinedRoom(roomId); reloadRoom(); } }).catch((error) => { if (alive) setJoinError(error); }); return () => { alive = false; }; }, [roomId, reloadRoom]);
  useEffect(() => {
    const newest = chat.messages.at(-1)?.id;
    if (newest && newest !== lastMessage.current) bottom.current?.scrollIntoView({ behavior: 'instant', block: 'nearest' });
    lastMessage.current = newest;
  }, [chat.messages]);
  const phase = chat.round?.phase;
  const phaseName = { WAITING: '라운드 대기 중', PICKING: '증강 선택 중', PLAYING: '대화 라운드 진행 중', ENDED: '라운드 종료' }[phase] || '자유롭게 대화해요';
  const pins = chat.messages.filter((message) => (!message.roundId || message.roundId === chat.round?.roundId)
    && messageEffects(message).some((effect) => effect.type === 'SPOTLIGHT' || effect.type === 'TIME_SHIFTER' && message.likeCount > 0)).slice(-2);
  function send(event) {
    event.preventDefault(); if (!content.trim()) return;
    if (chat.cooldown > Date.now()) { notify('잠시 쉬었다가 다시 보내주세요.', 'error'); return; }
    try { chat.publish('chat', { content: content.trim(), replyToId: reply?.id || null, clientMessageId: crypto.randomUUID() }); setContent(''); setReply(null); }
    catch (error) { notify(error.message, 'error'); }
  }
  return <><Link to="/rooms" className="back-link"><ArrowLeft size={17} />채팅방 둘러보기</Link>
    {joinError && <ErrorState error={joinError} retry={() => window.location.reload()} />}
    <ResourceState resource={room}>{room.data && <div className="chat-layout">
      <section className="panel chat-panel">
        <header className="chat-header"><span className="room-symbol symbol-0"><MessageCircle size={24} /></span>
          <div><h1>{room.data.name}</h1><span className="muted">{room.data.currentMembers}명 참여 중 · <span className={chat.connected ? 'connected' : ''}>{chat.connected ? '연결됨' : '연결 대기'}</span></span></div>
          <ActionButton className="icon-button danger" aria-label="채팅방 나가기" action={async () => { await api.request(`/rooms/${roomId}/leave`, { method: 'POST' }); navigate('/rooms'); }}><LogOut size={19} /></ActionButton>
        </header>
        {chat.error && <div className="inline-error" role="alert">{chat.error.message}</div>}
        {!!pins.length && <nav className="chat-pins" aria-label="상단 고정 메시지">{pins.map((message) => <button type="button" className="chat-pin" key={message.id} onClick={() => goToMessage(message.id)}><span>{messageEffects(message).some((effect) => effect.type === 'SPOTLIGHT') ? '스팟라이트' : '관심 메시지'}</span>{messageText(message)}</button>)}</nav>}
        <div className="chat-messages" aria-live="polite"><div className="chat-message-list" ref={messageList}>
          <ThreadConnections messages={chat.messages} containerRef={messageList} />
          {!chat.messages.length && <Empty title="가벼운 인사부터 시작해요" description="이 방의 새로운 이야기를 함께 만들어보세요." />}
          {chat.messages.map((message) => {
            const anonymous = isAnonymous(message); const mine = !anonymous && message.senderId === user.id;
            const name = anonymous ? '익명' : mine ? user.username : message.senderName || `참여자 ${shortId(message.senderId)}`;
            const linked = messageEffects(message).some((effect) => ['LINK_BRIDGE', 'THREAD_WEAVER'].includes(effect.type));
            const locked = message.content == null && messageEffects(message).some((effect) => effect.type === 'TIME_CAPSULE');
            return <article id={`message-${message.id}`} tabIndex={-1} key={message.id} className={`message-row ${mine ? 'mine' : ''}`}>
              <Avatar name={anonymous ? '?' : name} size="small" /><div className="message-body">
                <div className="message-meta"><strong>{name}</strong>{!anonymous && message.senderTitle && <span className="badge">{message.senderTitle}</span>}<span>{time(message.createdAt)}</span></div>
                {message.replyToId && <button type="button" className={`quote-link ${linked ? 'linked' : ''}`} onClick={() => goToMessage(message.replyToId)} aria-label="인용한 원본 메시지로 이동"><Reply size={13} /><span>{message.replyToPreview || '이전 메시지에 답장'}</span></button>}
                <AugmentRenderer message={message} onReveal={chat.refreshMessage} />
                <div className="message-actions"><button type="button" className="text-button" disabled={locked} onClick={() => setReply(message)}><Reply size={13} />답장</button>
                  <button type="button" className={`text-button ${message.likedByUserIds?.includes(user.id) ? 'liked' : ''}`} aria-label="메시지 공감" disabled={mine || locked} onClick={() => { try { chat.publish('like', { messageId: message.id }); } catch (error) { notify(error.message, 'error'); } }}><Heart size={13} />{message.likeCount || ''}</button></div>
              </div></article>;
          })}<div ref={bottom} />
        </div></div>
        <form className="chat-composer" onSubmit={send}>
          {reply && <div className="reply-indicator"><span>{isAnonymous(reply) ? '익명: ' : ''}{messageText(reply).slice(0, 60)}에 답장</span><button type="button" className="icon-button" aria-label="답장 취소" onClick={() => setReply(null)}><X size={15} /></button></div>}
          <div><textarea aria-label="채팅 메시지" placeholder="메시지를 입력해주세요. Enter로 전송, Shift+Enter로 줄바꿈" rows={2} maxLength={2000} value={content} onChange={(event) => setContent(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) { event.preventDefault(); send(event); } }} disabled={!chat.connected || Boolean(joinError)} /><button className="send-button" aria-label="메시지 전송" disabled={!chat.connected || !content.trim() || Boolean(joinError)}><Send size={21} /></button></div>
        </form>
      </section>
      <aside className="chat-aside"><div className="panel round-panel"><span className="eyebrow">PLAY WITH YOUR WORDS</span><h2><Sparkles size={20} />{phaseName}</h2><p>대화에 참여하며 점수와 보상을 모아보세요.</p>
        {room.data.ownerId === user.id && (!phase || phase === 'WAITING' || phase === 'ENDED') && <ActionButton className="button wide" action={() => api.request(`/rooms/${roomId}/round/start`, { method: 'POST', body: { durationMinutes: 10 } })}><Play size={16} />10분 라운드 시작</ActionButton>}
        {chat.round?.scores?.map((score, index) => <div className="score-row" key={score.userId}><span>{index + 1}</span><strong>{score.userId === user.id ? '나' : shortId(score.userId)}</strong><span>{score.totalPoints}점</span></div>)}
        {!!chat.highlights.length && <section className="round-highlights"><h3>✧ 대화 하이라이트</h3>{chat.highlights.map((message) => <AugmentRenderer key={message.id} message={message} />)}</section>}
      </div><AugmentPanel key={roomId} roomId={roomId} enabled={joinedRoom === roomId} room={room.data} phase={phase} roundId={chat.round?.roundId} revision={chat.augmentRevision} /></aside>
    </div>}</ResourceState></>;
}
