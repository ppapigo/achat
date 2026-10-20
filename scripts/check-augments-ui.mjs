import { chromium } from 'playwright';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { AUGMENT_NAMES } from '../achat-front/src/components/augments/effects.js';

const baseUrl = process.env.AUGMENT_UI_BASE_URL || 'http://127.0.0.1:5177';
const output = new URL('../achat-front/test-results/augments/', import.meta.url);
await fs.mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true, channel: process.env.UI_BROWSER_CHANNEL || (process.platform === 'win32' ? 'msedge' : undefined) });
const errors = [];
const checked = [];
try {
  for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }]) {
    const context = await browser.newContext({ viewport });
    await context.addInitScript(() => sessionStorage.setItem('achat.session', JSON.stringify({ accessToken: 'augment-ui', refreshToken: 'refresh-ui' })));
    const page = await context.newPage();
    page.on('pageerror', (error) => errors.push(error.message));
    const now = Date.now();
    const user = { id: 'me', email: 'ui@example.test', username: '테스트참가자', coins: 1000, level: 1, tier: 'UNRANKED' };
    const round = { roomId: 'effects', roundId: 'cycle-ui', phase: 'PICKING', phaseEndsAt: new Date(now + 360000).toISOString(), scores: [], serverTime: new Date(now).toISOString() };
    const messages = Object.keys(AUGMENT_NAMES).map((type, index) => {
      const effect = { type, status: 'ACTIVE', points: 0, value: type === 'FATE_DICE' ? 6 : 0, detail: null, progress: 0, target: 0 };
      if (type === 'TIME_CAPSULE') { effect.status = 'LOCKED'; effect.revealAt = new Date(now + 360000).toISOString(); }
      if (type === 'TIME_BOMB') effect.expiresAt = new Date(now + 15000).toISOString();
      if (type === 'DOPPELGANGER') effect.revealAt = new Date(now - 1000).toISOString();
      if (type === 'MAIN_CHARACTER') effect.expiresAt = new Date(now + 60000).toISOString();
      if (type === 'JACKPOT_MESSENGER') { effect.status = 'WON'; effect.value = 7; effect.points = 10; }
      if (type === 'FORBIDDEN_WORD') { effect.detail = '무지개'; effect.progress = 2; effect.target = 5; }
      if (type === 'QUEST_GIVER') { effect.detail = 'REPLY_QUEST'; effect.progress = 1; effect.target = 3; effect.value = 10; }
      return { id: `effect-${type}`, roomId: 'effects', roundId: 'cycle-ui', senderId: type === 'SHADOW_SPEAKER' ? null : 'friend',
        senderName: type === 'SHADOW_SPEAKER' ? '익명' : '대화친구', anonymous: type === 'SHADOW_SPEAKER',
        content: type === 'TIME_CAPSULE' ? null : `${AUGMENT_NAMES[type]} 첫 번째 줄\n안전한 JSX <img src=x onerror=alert(1)>`,
        appliedAugment: type, augmentEffects: [effect], createdAt: new Date(now - 60000 + index * 500).toISOString(),
        serverTime: new Date(now).toISOString(), revision: 1, likeCount: type === 'LIKE_OVERDRIVE' ? 4 : type === 'TIME_SHIFTER' ? 1 : 0,
        likedByUserIds: [], replyToId: ['THREAD_WEAVER', 'LINK_BRIDGE'].includes(type) ? 'effect-REVERSE_TEXT' : null,
        replyToPreview: '원본 메시지와 연결됩니다' };
    });
    let socket;
    let sequence = 0;
    const subscriptions = new Map();
    function event(topic, payload) {
      const destination = `/topic/rooms/effects/${topic}`;
      const id = subscriptions.get(destination);
      assert.ok(id, `Subscribed to ${topic}`);
      socket.send(`MESSAGE\nsubscription:${id}\nmessage-id:${++sequence}\ndestination:${destination}\ncontent-type:application/json\n\n${JSON.stringify(payload)}\0`);
    }
    await page.routeWebSocket('**/backend/ws', (ws) => {
      socket = ws;
      ws.onMessage((data) => {
        const frame = String(data);
        const command = frame.split('\n')[0];
        const [header, body = ''] = frame.split('\n\n');
        const headers = Object.fromEntries(header.split('\n').slice(1).map((line) => { const index = line.indexOf(':'); return [line.slice(0, index), line.slice(index + 1)]; }));
        if (command === 'CONNECT') ws.send('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0');
        if (command === 'SUBSCRIBE') subscriptions.set(headers.destination, headers.id);
        if (command === 'SEND' && headers.destination.endsWith('/like')) {
          const payload = JSON.parse(body.replace(/\0.*$/s, ''));
          const message = messages.find((item) => item.id === payload.messageId);
          message.likeCount++; message.revision++; message.likedByUserIds = ['me'];
          if (message.appliedAugment === 'LIKE_OVERDRIVE') message.augmentEffects[0] = { ...message.augmentEffects[0], status: 'REWARDED', points: 10 };
          event('message-effects', { ...message, serverTime: new Date().toISOString() });
        }
      });
    });
    const options = ['SEALED_MESSAGE', 'ICE_SEAL', 'FATE_DICE'].map((type) => ({ type, name: AUGMENT_NAMES[type], description: '서버에서 확정되는 효과를 확인해보세요.' }));
    let selected = null;
    let conflict = true;
    const picks = [];
    await page.route('**/backend/**', async (route) => {
      const request = route.request(); const path = new URL(request.url()).pathname.replace('/backend', '');
      const ok = (data) => route.fulfill({ json: { success: true, data } });
      if (path === '/auth/me') return ok(user);
      if (path === '/rooms/effects' || path === '/rooms/effects/join') return ok({ id: 'effects', name: '27가지 대화의 모습', ownerId: 'me', currentMembers: 4, maxMembers: 10 });
      if (path === '/rooms/effects/round') return ok(round);
      if (path === '/rooms/effects/messages') return ok(messages);
      if (path === '/rooms/effects/messages/highlights') return ok([]);
      if (path.startsWith('/rooms/effects/messages/')) return ok(messages.find((message) => message.id === path.split('/').at(-1)));
      if (path.endsWith('/augments/pick')) {
        const payload = request.postDataJSON(); picks.push(payload);
        assert.equal(payload.roundId, 'cycle-ui'); assert.equal(payload.targetUserId, 'free');
        if (conflict) { conflict = false; return route.fulfill({ status: 409, json: { success: false, error: '이미 증강을 받은 상대입니다. 다른 상대를 선택해주세요' } }); }
        selected = payload.type;
      }
      if (path.endsWith('/augments/options') || path.endsWith('/augments/pick')) return ok({
        options, selected, targetUserId: selected ? 'free' : null, roundId: 'cycle-ui', canSelect: !selected,
        selectionEndsAt: round.phaseEndsAt, serverTime: new Date().toISOString(),
        receivedAugment: 'FORBIDDEN_WORD', forbiddenWord: '무지개',
        targets: [{ userId: 'occupied', username: '이미받은친구', available: false }, { userId: 'free', username: '선택가능친구', available: true }],
      });
      errors.push(`Unexpected request ${path}`); return route.fulfill({ status: 404, json: { success: false, error: path } });
    });

    await page.goto(`${baseUrl}/rooms/effects`);
    await page.locator('.message-row').last().waitFor();
    assert.equal(await page.locator('.message-row').count(), 27);
    assert.equal(await page.locator('.message-bubble img').count(), 0, 'User text must never become HTML');
    for (const type of Object.keys(AUGMENT_NAMES)) assert.equal(await page.locator(`#message-effect-${type} [data-augment="${type}"]`).count(), 1);
    await page.locator('#message-effect-SEALED_MESSAGE .effect-cover').focus();
    await page.keyboard.press('Enter');
    assert.equal(await page.locator('#message-effect-SEALED_MESSAGE .revealed-content').isVisible(), true);
    await page.locator('#message-effect-ICE_SEAL .effect-cover').click();
    assert.equal(await page.locator('#message-effect-ICE_SEAL .revealed-content').isVisible(), true);
    await page.locator('#message-effect-GHOST_MESSAGE .ghost-message').click();
    assert.equal(await page.locator('#message-effect-GHOST_MESSAGE .ghost-message').getAttribute('aria-pressed'), 'true');
    assert.equal(await page.locator('#message-effect-DOPPELGANGER .message-afterimage').count(), 1);
    assert.equal(await page.locator('#message-effect-ECHO_BURST .echo-first-line').innerText(), '에코 버스트 첫 번째 줄');
    assert.equal(await page.locator('#message-effect-SHADOW_SPEAKER .message-meta strong').innerText(), '익명');
    assert.equal(await page.locator('.chat-pins .chat-pin').count(), 2);
    assert.equal(await page.locator('.thread-connections path').count(), 2);
    await page.locator('#message-effect-LINK_BRIDGE .quote-link').click();
    assert.equal(await page.evaluate(() => document.activeElement.id), 'message-effect-REVERSE_TEXT');
    assert.equal(await page.locator('#message-effect-TIME_CAPSULE').innerText().then((text) => text.includes('초 후 공개')), true);
    const capsule = messages.find((message) => message.appliedAugment === 'TIME_CAPSULE');
    capsule.content = '서버가 공개한 타임 캡슐'; capsule.revision++;
    capsule.augmentEffects[0] = { ...capsule.augmentEffects[0], status: 'REVEALED' };
    event('message-effects', capsule);
    await page.getByText('서버가 공개한 타임 캡슐', { exact: true }).waitFor();
    await page.locator('#message-effect-LIKE_OVERDRIVE').getByRole('button', { name: '메시지 공감' }).click();
    await page.locator('#message-effect-LIKE_OVERDRIVE .like-stage-5').waitFor();
    assert.ok((await page.locator('#message-effect-LIKE_OVERDRIVE').innerText()).includes('+10점 지급 완료'));
    assert.equal(await page.locator('.message-row').count(), 27, 'Visual clones and effect updates must not add messages');
    const target = page.getByRole('combobox', { name: '증강을 받을 상대' });
    assert.equal(await target.locator('option[value="occupied"]').evaluate((option) => option.disabled), true);
    await target.selectOption('free');
    await page.locator('.augment-option').first().click();
    await page.getByRole('button', { name: '증강 보내기 확정' }).click();
    await page.getByRole('alert').filter({ hasText: '이미 증강을 받은 상대' }).waitFor();
    await page.getByRole('button', { name: '증강 보내기 확정' }).click();
    await page.getByText('적용 대상: 선택가능친구', { exact: false }).waitFor();
    assert.equal(picks.length, 2);
    assert.equal(await page.locator('.augment-option').count(), 0);
    event('round/highlights', { highlights: [messages.find((message) => message.appliedAugment === 'INSIGHT_DROP')] });
    await page.getByRole('heading', { name: '✧ 대화 하이라이트' }).waitFor();
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1), false);
    await page.locator('#message-effect-MAIN_CHARACTER').scrollIntoViewIfNeeded();
    await page.screenshot({ path: fileURLToPath(new URL(`${viewport.width}-effects.png`, output)), fullPage: true });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    assert.equal(await page.locator('#message-effect-ZERO_GRAVITY .presentation-effect').evaluate((element) => getComputedStyle(element).animationName), 'none');
    await page.reload();
    await page.getByText('서버가 공개한 타임 캡슐', { exact: true }).waitFor();
    assert.equal(await page.locator('#message-effect-FATE_DICE .dice-face').getAttribute('aria-label'), '주사위 6');
    checked.push(`${viewport.width}: 27 effects, keyboard/touch, selection conflict/retry, realtime update, reload, reduced motion`);
    await context.close();
  }
  assert.deepEqual(errors, []);
  await fs.writeFile(new URL('report.json', output), JSON.stringify({ checked, errors }, null, 2));
  console.log(checked.join('\n'));
} finally { await browser.close(); }
