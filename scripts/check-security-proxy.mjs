// Run only against an isolated local test deployment. Creates disposable test accounts/room.
// Usage: node scripts/check-security-proxy.mjs http://127.0.0.1:PORT
import assert from 'node:assert/strict';
import http from 'node:http';
import { randomBytes, randomUUID } from 'node:crypto';
import { setTimeout as delay } from 'node:timers/promises';

const base = new URL(process.argv[2] || '');
assert(['127.0.0.1', 'localhost', '[::1]'].includes(base.hostname) && base.protocol === 'http:', 'Local isolated deployment required');
const origin = 'https://achat.grooverapp.com';
async function api(path, { token, body, method = body ? 'POST' : 'GET', expected = 200 } = {}) {
  const response = await fetch(new URL(`/backend${path}`, base), {
    method, headers: { Origin: origin, ...(token && { Authorization: `Bearer ${token}` }), ...(body && { 'Content-Type': 'application/json' }) },
    body: body && JSON.stringify(body),
  });
  assert.equal(response.status, expected, `${method} ${path} status`);
  const payload = await response.json();
  return payload.data ?? payload;
}
async function account() {
  const email = `proxy-${randomUUID()}@test.invalid`;
  const password = 'isolated-proxy-test-password';
  await api('/auth/register', { body: { email, password } });
  return api('/auth/login', { body: { email, password } });
}
function maskedFrame(text) {
  const data = Buffer.from(text), mask = randomBytes(4);
  const header = Buffer.alloc(data.length < 126 ? 2 : 4);
  header[0] = 0x81; header[1] = 0x80 | (data.length < 126 ? data.length : 126);
  if (data.length >= 126) header.writeUInt16BE(data.length, 2);
  return Buffer.concat([header, mask, Buffer.from(data.map((byte, index) => byte ^ mask[index % 4]))]);
}
async function connect(path, token, allowedOrigin = origin) {
  return new Promise((resolve, reject) => {
    const request = http.request(new URL(path, base), { headers: {
      Origin: allowedOrigin, Connection: 'Upgrade', Upgrade: 'websocket',
      'Sec-WebSocket-Version': '13', 'Sec-WebSocket-Key': randomBytes(16).toString('base64'),
      'Sec-WebSocket-Protocol': 'v12.stomp',
    } });
    const timer = setTimeout(() => { request.destroy(); reject(new Error('WebSocket timeout')); }, 6000);
    request.on('response', (response) => { clearTimeout(timer); response.resume(); reject(new Error(`Upgrade HTTP ${response.statusCode}`)); });
    request.on('error', (error) => { clearTimeout(timer); reject(error); });
    request.on('upgrade', (response, socket, head) => {
      let data = head;
      const waiting = [];
      const frames = [];
      const consume = (chunk) => {
        data = Buffer.concat([data, chunk]);
        while (data.length >= 2) {
          let size = data[1] & 127, offset = 2;
          if (size === 126) { if (data.length < 4) return; size = data.readUInt16BE(2); offset = 4; }
          if (size === 127) { socket.destroy(); return reject(new Error('Unexpected oversized server frame')); }
          if (data.length < offset + size) return;
          const text = data.subarray(offset, offset + size).toString();
          data = data.subarray(offset + size);
          if (text.startsWith('CONNECTED')) {
            clearTimeout(timer);
            resolve({
              send: (command, headers, body = '') => socket.write(maskedFrame(`${command}\n${Object.entries(headers).map(([key, value]) => `${key}:${value}`).join('\n')}\n\n${body}\0`)),
              next: () => frames.length ? Promise.resolve(frames.shift()) : new Promise((resolveNext) => waiting.push(resolveNext)),
              close: () => socket.destroy(),
            });
          } else if (text.startsWith('MESSAGE') || text.startsWith('ERROR') || text.startsWith('RECEIPT')) {
            if (waiting.length) waiting.shift()(text); else frames.push(text);
          }
        }
      };
      socket.on('data', consume);
      socket.on('error', reject);
      socket.write(maskedFrame(`CONNECT\naccept-version:1.2\nhost:achat.grooverapp.com\nAuthorization:Bearer ${token}\n\n\0`));
    });
    request.end();
  });
}
const timeout = (promise) => Promise.race([promise, new Promise((_, reject) => {
  const timer = setTimeout(() => reject(new Error('STOMP response timeout')), 6000); timer.unref();
})]);
const page = await fetch(base);
assert.equal(page.status, 200);
assert.match(page.headers.get('content-security-policy'), /frame-ancestors 'none'/);
const first = await account(), second = await account();
const room = await api('/rooms', { token: first.accessToken, body: { name: 'proxy validation', maxMembers: 8, userId: second.userId } });
assert.equal(room.ownerId, first.userId);
await api(`/rooms/${room.id}/join`, { token: second.accessToken, body: {} });
await api('/api/admin/reports', { token: second.accessToken, expected: 403 });
await api('/auth/me', { token: first.refreshToken, expected: 401 });
await assert.rejects(connect('/backend/ws', first.accessToken, 'https://attacker.invalid'), /403/);
const reader = await connect('/backend/ws', first.accessToken);
const writer = await connect('/ws', second.accessToken);
try {
  reader.send('SUBSCRIBE', { id: 'messages', destination: `/topic/rooms/${room.id}/messages` });
  // Spring's simple broker has no subscription receipts; allow its async subscription to settle.
  // The integration suite separately checks registration via SimpUserRegistry.
  await delay(200);
  writer.send('SEND', { destination: `/app/rooms/${room.id}/chat`, 'content-type': 'application/json' },
    JSON.stringify({ content: 'isolated proxy test message', clientMessageId: randomUUID(), userId: first.userId }));
  const frame = await timeout(reader.next());
  assert.match(frame, /^MESSAGE/);
  const message = JSON.parse(frame.slice(frame.indexOf('\n\n') + 2).replace(/\0.*$/s, ''));
  assert.equal(message.senderId, second.userId);
  await api('/auth/logout', { body: { refreshToken: first.refreshToken } });
  await api('/auth/me', { token: first.accessToken, expected: 401 });
} finally { reader.close(); writer.close(); }
console.log('PASS: static/CSP, signup/login, principal ownership, admin/access-token restrictions, foreign Origin, both WebSocket proxy paths, real chat delivery, logout revocation');
