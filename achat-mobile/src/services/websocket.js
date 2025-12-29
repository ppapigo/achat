import { Client } from '@stomp/stompjs';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Android 에뮬레이터: 10.0.2.2, iOS: localhost, 실기기: 컴퓨터 IP
const WS_URL = 'ws://10.0.2.2:8080/ws';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
    this.isConnected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.listeners = new Map();
  }

  // 연결
  async connect() {
    if (this.client?.connected) {
      console.log('WebSocket already connected');
      return;
    }

    const token = await AsyncStorage.getItem('accessToken');
    if (!token) {
      console.error('No access token for WebSocket');
      return;
    }

    return new Promise((resolve, reject) => {
      this.client = new Client({
        brokerURL: WS_URL,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        debug: (str) => {
          if (__DEV__) console.log('[STOMP]', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,

        onConnect: () => {
          console.log('✅ WebSocket connected');
          this.isConnected = true;
          this.reconnectAttempts = 0;
          
          // 유저 전용 채널 구독
          this.subscribeUserChannels();
          
          this.emit('connected');
          resolve();
        },

        onDisconnect: () => {
          console.log('❌ WebSocket disconnected');
          this.isConnected = false;
          this.emit('disconnected');
        },

        onStompError: (frame) => {
          console.error('STOMP error:', frame.headers['message']);
          this.emit('error', frame);
          reject(new Error(frame.headers['message']));
        },

        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          this.emit('error', event);
        },
      });

      this.client.activate();
    });
  }

  // 연결 해제
  disconnect() {
    if (this.client) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      this.client = null;
      this.isConnected = false;
    }
  }

  // 유저 전용 채널 구독 (쿨타임, 에러)
  subscribeUserChannels() {
    // 쿨타임 알림
    this.subscribe('/user/queue/cooldown', (message) => {
      const data = JSON.parse(message.body);
      this.emit('cooldown', data);
    });

    // 에러 알림
    this.subscribe('/user/queue/error', (message) => {
      const data = JSON.parse(message.body);
      this.emit('error', data);
    });
  }

  // 방 입장 (구독 시작)
  joinRoom(roomId) {
    if (!this.isConnected) {
      console.error('WebSocket not connected');
      return;
    }

    // 방 이벤트 구독 (입장/퇴장)
    this.subscribe(`/topic/rooms/${roomId}/events`, (message) => {
      const data = JSON.parse(message.body);
      this.emit('roomEvent', { roomId, ...data });
    });

    // 메시지 구독
    this.subscribe(`/topic/rooms/${roomId}/messages`, (message) => {
      const data = JSON.parse(message.body);
      this.emit('message', { roomId, ...data });
    });

    // 좋아요 구독
    this.subscribe(`/topic/rooms/${roomId}/likes`, (message) => {
      const data = JSON.parse(message.body);
      this.emit('like', { roomId, ...data });
    });

    // 서버에 입장 알림
    this.send(`/app/rooms/${roomId}/join`, {});
  }

  // 방 퇴장 (구독 해제)
  leaveRoom(roomId) {
    if (!this.isConnected) return;

    // 서버에 퇴장 알림
    this.send(`/app/rooms/${roomId}/leave`, {});

    // 구독 해제
    this.unsubscribe(`/topic/rooms/${roomId}/events`);
    this.unsubscribe(`/topic/rooms/${roomId}/messages`);
    this.unsubscribe(`/topic/rooms/${roomId}/likes`);
  }

  // 메시지 전송
  sendMessage(roomId, content, replyToId = null) {
    if (!this.isConnected) {
      console.error('WebSocket not connected');
      return false;
    }

    this.send(`/app/rooms/${roomId}/chat`, {
      content,
      replyToId,
    });
    return true;
  }

  // 좋아요 토글
  toggleLike(roomId, messageId) {
    if (!this.isConnected) return false;

    this.send(`/app/rooms/${roomId}/like`, {
      messageId,
    });
    return true;
  }

  // === 내부 메서드 ===

  // 구독
  subscribe(destination, callback) {
    if (!this.client?.connected) return null;

    if (this.subscriptions.has(destination)) {
      return this.subscriptions.get(destination);
    }

    const subscription = this.client.subscribe(destination, callback);
    this.subscriptions.set(destination, subscription);
    return subscription;
  }

  // 구독 해제
  unsubscribe(destination) {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
    }
  }

  // 메시지 전송
  send(destination, body) {
    if (!this.client?.connected) return;

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  // 이벤트 리스너 등록
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event).add(callback);
    
    // cleanup 함수 반환
    return () => {
      this.listeners.get(event)?.delete(callback);
    };
  }

  // 이벤트 발생
  emit(event, data) {
    this.listeners.get(event)?.forEach((callback) => {
      try {
        callback(data);
      } catch (e) {
        console.error('Event listener error:', e);
      }
    });
  }
}

// 싱글톤 인스턴스
export const wsService = new WebSocketService();
export default wsService;
