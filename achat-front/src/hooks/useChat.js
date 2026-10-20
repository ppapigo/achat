import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { api, API_BASE } from '../lib/api.js';
import { mergeLike, mergeMessages, mergeRound, websocketUrl } from '../lib/chat.js';
import { useToast } from '../contexts/ToastContext.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';
import { UI_PREVIEW } from '../preview/mode.js';
import { syncServerTime } from '../components/augments/clock.js';

export function useChat(roomId, enabled, reloadRoom) {
  const [messages, setMessages] = useState([]); const [connected, setConnected] = useState(false); const [error, setError] = useState(null); const [round, setRound] = useState(null); const [cooldown, setCooldown] = useState(0);
  const clientRef = useRef(null); const notify = useToast(); const { reloadUser } = useAuth();
  const [augmentRevision, setAugmentRevision] = useState(0);
  const [highlights, setHighlights] = useState([]);
  const activeRoom = useRef(null);
  const refreshMessage = useCallback(async (id) => {
    try {
      const message = await api.request(`/rooms/${roomId}/messages/${encodeURIComponent(id)}`);
      if (activeRoom.current !== roomId) return;
      syncServerTime(message.serverTime);
      setMessages((current) => mergeMessages(current, [message]));
    } catch (error) { if (activeRoom.current === roomId) notify(error.message, 'error'); }
  }, [roomId, notify]);
  useEffect(() => {
    setMessages([]); setRound(null); setError(null); setConnected(false); setHighlights([]);
    activeRoom.current = enabled ? roomId : null;
    if (!enabled) return;
    let alive = true; const controller = new AbortController();
    if (UI_PREVIEW) {
      const load = async () => { try { const [history, value] = await Promise.all([api.request(`/rooms/${roomId}/messages`, { signal: controller.signal }), api.request(`/rooms/${roomId}/round`, { signal: controller.signal })]); if (alive) { setMessages(history); setRound(value); setConnected(true); } } catch (error) { if (alive) setError(error); } };
      const changed = (event) => { if (event.detail.roomId === roomId) load(); };
      window.addEventListener('achat:preview-chat', changed); load();
      return () => { alive = false; controller.abort(); window.removeEventListener('achat:preview-chat', changed); };
    }
    const client = new Client({ brokerURL: import.meta.env.VITE_WS_URL || websocketUrl(API_BASE, window.location.origin), reconnectDelay: 3000, heartbeatIncoming: 10000, heartbeatOutgoing: 10000 });
    let renewTimer;
    client.beforeConnect = async () => { try { client.connectHeaders = { Authorization: `Bearer ${await api.ensureAccessToken()}` }; } catch (error) { if (alive) setError(error); await client.deactivate(); } };
    client.onConnect = () => {
      if (!alive) return;
      setConnected(true); setError(null);
      clearTimeout(renewTimer);
      try {
        const token = client.connectHeaders.Authorization.slice(7);
        const expires = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).exp * 1000;
        renewTimer = setTimeout(() => client.forceDisconnect(), Math.max(1000, expires - Date.now() - 20000));
      } catch { /* Server validates every frame regardless of this renewal timer. */ }
      const subscribe = (destination, callback) => client.subscribe(destination, (frame) => { if (!alive) return; try { callback(JSON.parse(frame.body)); } catch { notify('실시간 알림을 읽지 못했습니다.', 'error'); } });
      const receiveMessage = (message) => { syncServerTime(message.serverTime); setMessages((current) => mergeMessages(current, [message])); };
      subscribe(`/topic/rooms/${roomId}/messages`, receiveMessage);
      subscribe(`/topic/rooms/${roomId}/message-effects`, receiveMessage);
      subscribe(`/topic/rooms/${roomId}/augments`, (event) => { syncServerTime(event.serverTime); setAugmentRevision((value) => value + 1); });
      subscribe(`/topic/rooms/${roomId}/quest`, (event) => { if (event.messageId) refreshMessage(event.messageId); if (event.type === 'complete') { notify('채팅 미션 완료!'); reloadUser(); } });
      subscribe(`/topic/rooms/${roomId}/likes`, (like) => setMessages((current) => current.map((message) => mergeLike(message, like))));
      subscribe(`/topic/rooms/${roomId}/events`, reloadRoom);
      subscribe(`/topic/rooms/${roomId}/round`, (value) => { syncServerTime(value.serverTime); setRound((current) => mergeRound(current, value)); });
      subscribe(`/topic/rooms/${roomId}/round/end`, (value) => { setHighlights(value.highlights || []); notify('라운드가 끝났어요. 프로필에서 보상을 확인해보세요.'); reloadUser(); });
      subscribe(`/topic/rooms/${roomId}/round/highlights`, (value) => setHighlights(value.highlights || []));
      subscribe('/user/queue/cooldown', (notice) => { setCooldown(Date.now() + (notice.remainingMs || notice.remainingMillis || 3000)); notify(notice.message || '잠시 쉬었다가 다시 보내주세요.', 'error'); });
      subscribe('/user/queue/error', (notice) => notify(notice.message || '메시지를 전송하지 못했습니다.', 'error'));
      subscribe('/user/queue/quests', () => notify('새 채팅 미션 알림이 도착했어요.'));
      client.publish({ destination: `/app/rooms/${roomId}/join`, body: '{}' });
      // Subscribe first, then merge history so messages arriving during the request are retained.
      api.request(`/rooms/${roomId}/messages?limit=100`, { signal: controller.signal }).then((history) => { if (alive) { history.forEach((message) => syncServerTime(message.serverTime)); setMessages((current) => mergeMessages(current, history)); } }).catch((error) => { if (alive) setError(error); });
      api.request(`/rooms/${roomId}/round`, { signal: controller.signal }).then((value) => { if (alive) { syncServerTime(value.serverTime); setRound((current) => mergeRound(current, value)); } }).catch((error) => { if (alive) notify(error.message, 'error'); });
      api.request(`/rooms/${roomId}/messages/highlights`, { signal: controller.signal }).then((value) => { if (alive) setHighlights((current) => current.length ? current : value); }).catch((error) => { if (alive) notify(error.message, 'error'); });
    };
    client.onStompError = (frame) => { if (alive) setError(new Error(frame.headers.message || '채팅 연결을 확인해주세요.')); };
    client.onWebSocketClose = () => { clearTimeout(renewTimer); if (alive) setConnected(false); };
    client.onWebSocketError = () => { if (alive) setError(new Error('채팅 서버에 연결하지 못했습니다. 자동으로 다시 연결합니다.')); };
    clientRef.current = client; client.activate();
    return () => { alive = false; clearTimeout(renewTimer); activeRoom.current = null; controller.abort(); clientRef.current = null; client.deactivate(); };
  }, [roomId, enabled, notify, reloadRoom, reloadUser, refreshMessage]);
  function publish(action, body) { if (UI_PREVIEW) { api.request(`/rooms/${roomId}/preview-chat/${action}`, { method: 'POST', body }).catch((error) => notify(error.message, 'error')); return; } if (!clientRef.current?.connected) throw new Error('채팅 연결을 기다려주세요.'); clientRef.current.publish({ destination: `/app/rooms/${roomId}/${action}`, body: JSON.stringify(body) }); }
  return { messages, connected, error, round, cooldown, publish, augmentRevision, highlights, refreshMessage };
}
