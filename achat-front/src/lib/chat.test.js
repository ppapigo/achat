import { expect, it } from 'vitest';
import { mergeMessages, websocketUrl } from './chat.js';

it('merges late history with live messages without losing new messages or newer likes', () => {
  const current = [{ id: 'b', createdAt: '2026-01-01T00:00:02Z', likeCount: 2 }, { id: 'c', createdAt: '2026-01-01T00:00:03Z' }];
  const history = [{ id: 'a', createdAt: '2026-01-01T00:00:01Z' }, { id: 'b', createdAt: '2026-01-01T00:00:02Z', content: 'hello', likeCount: 0 }];
  const merged = mergeMessages(current, history);
  expect(merged.map((message) => message.id)).toEqual(['a', 'b', 'c']);
  expect(merged[1]).toMatchObject({ content: 'hello', likeCount: 2 });
});

it('uses the same proxy path for REST and WebSocket and supports secure production URLs', () => {
  expect(websocketUrl('/backend', 'http://localhost:5173')).toBe('ws://localhost:5173/backend/ws');
  expect(websocketUrl('https://api.example.com/', 'https://app.example.com')).toBe('wss://api.example.com/ws');
});
