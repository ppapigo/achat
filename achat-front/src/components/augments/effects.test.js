import { describe, expect, it } from 'vitest';
import { AUGMENT_NAMES, isAnonymous, likeStage, messageEffects, messageText, secondsRemaining } from './effects.js';
import { mergeMessages } from '../../lib/chat.js';

describe('structured augment messages', () => {
  it('covers all 27 effects and retains legacy history', () => {
    expect(Object.keys(AUGMENT_NAMES)).toHaveLength(27);
    expect(messageEffects({ appliedAugment: 'ECHO_BURST,UNKNOWN' })).toEqual([{ type: 'ECHO_BURST', status: 'ACTIVE' }]);
    expect(messageText({ content: '<img onerror=alert(1)>', appliedContent: '' })).toBe('');
  });
  it('keeps anonymous identities hidden even for legacy messages', () => {
    expect(isAnonymous({ appliedAugment: 'SHADOW_SPEAKER', senderId: 'legacy-id' })).toBe(true);
    expect(isAnonymous({ anonymous: true })).toBe(true);
  });
  it('calculates time boundaries and like thresholds exactly', () => {
    const end = '2026-10-20T12:00:10Z';
    expect(secondsRemaining(end, Date.parse(end) - 10001)).toBe(11);
    expect(secondsRemaining(end, Date.parse(end))).toBe(0);
    expect(secondsRemaining(end, Date.parse(end) + 1)).toBe(0);
    expect([0, 1, 2, 3, 4, 5, 8].map(likeStage)).toEqual([0, 1, 1, 3, 3, 5, 5]);
  });
  it('applies newer effect updates and rejects late historical snapshots', () => {
    const locked = { id: 'a', revision: 1, createdAt: '2026-10-20T12:00:00Z', content: null };
    const revealed = { ...locked, revision: 2, content: 'revealed', augmentEffects: [{ type: 'TIME_CAPSULE', status: 'REVEALED' }] };
    expect(mergeMessages([locked], [revealed])[0]).toEqual(revealed);
    expect(mergeMessages([revealed], [locked])[0]).toEqual(revealed);
  });
  it('refreshes deadline-derived visibility and quest progress at the same revision', () => {
    const old = { id: 'a', revision: 2, content: null, serverTime: '2026-10-20T12:00:00Z' };
    const next = { ...old, content: 'now visible', serverTime: '2026-10-20T12:00:10Z' };
    expect(mergeMessages([old], [next])[0].content).toBe('now visible');
  });
});
