import { expect, it } from 'vitest';
import { mergeLike, mergeMessages, mergeRound, websocketUrl } from './chat.js';

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

it('ignores out-of-order like events and accepts the legacy voter field', () => {
  const message = { id: 'a', revision: 5, likeCount: 5, likedByUserIds: [] };
  expect(mergeLike(message, { messageId: 'a', revision: 4, likeCount: 4, liked: true, userId: 'voter' })).toBe(message);
  expect(mergeLike(message, { messageId: 'a', revision: 6, likeCount: 6, liked: true, odUserId: 'voter' }))
    .toMatchObject({ likeCount: 6, likedByUserIds: ['voter'], revision: 6 });
});

it('does not restore an old picking phase when a delayed REST response follows a live update', () => {
  const picking = { roundId: 'first', phase: 'PICKING', serverTime: '2026-10-20T12:00:00Z' };
  const playing = { ...picking, phase: 'PLAYING', serverTime: '2026-10-20T12:00:20Z' };
  const next = { roundId: 'second', phase: 'PICKING', serverTime: '2026-10-20T12:03:20Z' };
  expect(mergeRound(playing, picking)).toBe(playing);
  expect(mergeRound(playing, next)).toBe(next);
  expect(mergeRound(null, picking)).toBe(picking);
});
