import { useEffect, useState, useCallback, useRef } from 'react';
import wsService from '../services/websocket';

// WebSocket 연결 상태 훅
export function useWebSocket() {
  const [isConnected, setIsConnected] = useState(wsService.isConnected);

  useEffect(() => {
    const onConnected = () => setIsConnected(true);
    const onDisconnected = () => setIsConnected(false);

    const cleanupConnected = wsService.on('connected', onConnected);
    const cleanupDisconnected = wsService.on('disconnected', onDisconnected);

    // 초기 연결
    if (!wsService.isConnected) {
      wsService.connect().catch(console.error);
    }

    return () => {
      cleanupConnected();
      cleanupDisconnected();
    };
  }, []);

  const connect = useCallback(() => wsService.connect(), []);
  const disconnect = useCallback(() => wsService.disconnect(), []);

  return { isConnected, connect, disconnect };
}

// 채팅방 훅
export function useChatRoom(roomId) {
  const [messages, setMessages] = useState([]);
  const [participants, setParticipants] = useState([]);
  const [isJoined, setIsJoined] = useState(false);
  const [cooldown, setCooldown] = useState(null);
  const [error, setError] = useState(null);
  
  const cleanupRef = useRef([]);

  useEffect(() => {
    if (!roomId || !wsService.isConnected) return;

    // 메시지 수신
    const cleanupMessage = wsService.on('message', (data) => {
      if (data.roomId === roomId) {
        setMessages((prev) => [...prev, {
          id: data.id,
          type: 'user',
          senderId: data.senderId,
          nickname: data.senderUsername || data.senderId?.substring(0, 8),
          senderTitle: data.senderTitle,
          content: data.content,
          appliedContent: data.appliedContent,
          augmentType: data.augmentType,
          likeCount: data.likeCount || 0,
          replyToId: data.replyToId,
          replyToPreview: data.replyToPreview,
          createdAt: data.createdAt,
        }]);
      }
    });

    // 방 이벤트 (입장/퇴장)
    const cleanupEvent = wsService.on('roomEvent', (data) => {
      if (data.roomId === roomId) {
        if (data.type === 'join') {
          setParticipants((prev) => {
            if (prev.find((p) => p.userId === data.userId)) return prev;
            return [...prev, { userId: data.userId, username: data.username }];
          });
          // 시스템 메시지 추가
          setMessages((prev) => [...prev, {
            id: `system-${Date.now()}`,
            type: 'system',
            content: `${data.username}님이 입장했습니다`,
          }]);
        } else if (data.type === 'leave') {
          setParticipants((prev) => prev.filter((p) => p.userId !== data.userId));
          setMessages((prev) => [...prev, {
            id: `system-${Date.now()}`,
            type: 'system',
            content: `${data.username}님이 퇴장했습니다`,
          }]);
        }
      }
    });

    // 좋아요 이벤트
    const cleanupLike = wsService.on('like', (data) => {
      if (data.roomId === roomId) {
        setMessages((prev) => prev.map((msg) =>
          msg.id === data.messageId
            ? { ...msg, likeCount: data.likeCount }
            : msg
        ));
      }
    });

    // 쿨타임 알림
    const cleanupCooldown = wsService.on('cooldown', (data) => {
      setCooldown(data);
      // 일정 시간 후 초기화
      setTimeout(() => setCooldown(null), data.remainingMs || 3000);
    });

    // 에러 알림
    const cleanupError = wsService.on('error', (data) => {
      setError(data);
      setTimeout(() => setError(null), 5000);
    });

    // 방 입장
    wsService.joinRoom(roomId);
    setIsJoined(true);

    cleanupRef.current = [
      cleanupMessage,
      cleanupEvent,
      cleanupLike,
      cleanupCooldown,
      cleanupError,
    ];

    return () => {
      cleanupRef.current.forEach((cleanup) => cleanup());
      wsService.leaveRoom(roomId);
      setIsJoined(false);
    };
  }, [roomId, wsService.isConnected]);

  // 메시지 전송
  const sendMessage = useCallback((content, replyToId = null) => {
    return wsService.sendMessage(roomId, content, replyToId);
  }, [roomId]);

  // 좋아요 토글
  const toggleLike = useCallback((messageId) => {
    return wsService.toggleLike(roomId, messageId);
  }, [roomId]);

  // 시스템 메시지 추가 (로컬)
  const addSystemMessage = useCallback((content) => {
    setMessages((prev) => [...prev, {
      id: `system-${Date.now()}`,
      type: 'system',
      content,
    }]);
  }, []);

  return {
    messages,
    participants,
    isJoined,
    cooldown,
    error,
    sendMessage,
    toggleLike,
    addSystemMessage,
  };
}
