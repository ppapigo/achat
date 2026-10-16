import { chromium } from 'playwright';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

// Run against a normal dev server or production preview, with API responses
// intercepted at the browser boundary. No database or real account is used.
const baseUrl = process.env.SEASON_UI_BASE_URL || 'http://127.0.0.1:5175';
const output = new URL('../achat-front/test-results/seasons/', import.meta.url);
await fs.mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true, channel: process.env.UI_BROWSER_CHANNEL || (process.platform === 'win32' ? 'msedge' : undefined) });
const issues = [];
const checked = [];
const user = { id: 'season-test-me', username: '시즌참여자', email: 'season@example.test', coins: 900, level: 4, tier: 'GOLD', rp: 950 };
const current = { seasonId: 3, name: '새로운 시즌', startDate: '2099-01-01T00:00:00Z', endDate: '2099-01-31T00:00:00Z', active: true, remainingDays: 17, progressPercent: 44 };
const rank = { odUserId: user.id, rp: 950, tier: 'GOLD', seasonHighestTier: 'PLATINUM', rpToNextTier: 250, seasonId: 3, rank: 3, placementCompleted: true, placementGamesPlayed: 5, placementGamesRemaining: 0 };
const history = [
  { seasonId: 1, finalRp: 420, finalTier: 'SILVER', highestTier: 'SILVER', rewardCoins: 350, recordedAt: '2098-12-01T00:00:00Z' },
  { seasonId: 2, finalRp: 820, finalTier: 'GOLD', highestTier: 'PLATINUM', rewardCoins: 700, recordedAt: '2099-01-01T00:00:00Z' },
];

try {
  for (const size of [{ name: 'desktop', width: 1440, height: 1000 }, { name: 'mobile', width: 390, height: 844 }]) {
    const context = await browser.newContext({ viewport: { width: size.width, height: size.height } });
    await context.addInitScript(() => sessionStorage.setItem('achat.session', JSON.stringify({ accessToken: 'season-ui-test', refreshToken: 'season-ui-refresh' })));
    const page = await context.newPage();
    page.on('pageerror', (error) => issues.push(error.message));
    let state = 'normal';
    const seen = new Set();
    await page.route('**/backend/**', async (route) => {
      const request = route.request();
      const path = new URL(request.url()).pathname.replace(/^\/backend/, '');
      assert.equal(request.method(), 'GET', 'Season views must only read data');
      seen.add(path);
      const send = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
      if (path === '/auth/me') return send({ success: true, data: user });
      if (path === '/api/seasons/current') {
        if (state === 'empty') return route.fulfill({ status: 404, body: '' });
        if (state === 'current-error') return send({ error: '시즌 조회 실패' }, 503);
        if (state === 'expired') return send({ ...current, endDate: '2000-01-01T00:00:00Z', remainingDays: 0, progressPercent: 100 });
        if (state === 'last-day') return send({ ...current, endDate: new Date(Date.now() + 3600000).toISOString(), remainingDays: 0, progressPercent: 99 });
        return send(current);
      }
      if (path === '/api/seasons') {
        if (state === 'catalog-error') return send({ error: '시즌 목록 조회 실패' }, 503);
        return send(history.map((record) => ({ seasonId: record.seasonId, name: `지난 시즌 ${record.seasonId}`, startDate: '2098-11-01T00:00:00Z', endDate: record.recordedAt, active: false })));
      }
      if (path === '/api/seasons/my-history') {
        if (state === 'history-error') return send({ error: '시즌 기록 조회 실패' }, 503);
        return send(state === 'empty' ? [] : history);
      }
      if (path === '/rank/me') return send({ success: true, data: state === 'empty' ? { ...rank, rp: 0, tier: 'UNRANKED', seasonHighestTier: 'UNRANKED', rank: -1, placementCompleted: false, placementGamesPlayed: 2, placementGamesRemaining: 3 } : state === 'top' ? { ...rank, tier: 'CHALLENGER', rpToNextTier: 0 } : rank });
      if (path === '/rank/leaderboard') return send({ success: true, data: { leaderboard: state === 'empty' ? [] : [{ ...rank, displayName: 'se***@example.test', level: 4 }], myRank: state === 'empty' ? -1 : 3 } });
      if (path === '/api/feed') return send({ data: { posts: [] } });
      if (path === '/api/friends' || path === '/rooms') return send({ success: true, data: [] });
      issues.push(`Unexpected API: ${path}`);
      return send({ error: 'Unexpected request' }, 404);
    });
    const open = async (nextState) => {
      state = nextState;
      await page.goto(`${baseUrl}/seasons`);
      await page.getByRole('heading', { level: 1, name: '시즌', exact: true }).waitFor();
      await page.waitForFunction(() => document.querySelectorAll('.loading').length === 0);
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false, `${size.name}, ${state}: overflow`);
    };
    const screenshot = (name) => page.screenshot({ path: fileURLToPath(new URL(`${size.name}-${name}.png`, output)), fullPage: true });

    await open('normal');
    assert.equal(await page.getByRole('progressbar', { name: '시즌 진행률' }).getAttribute('value'), '44');
    assert.equal(await page.locator('.season-record h3').allTextContents().then((names) => names.join(',')), '지난 시즌 2,지난 시즌 1');
    await page.getByText('다음 티어까지 250 RP', { exact: true }).waitFor();
    await page.getByText('700 코인', { exact: true }).waitFor();
    assert.equal(await page.locator('tr.is-me').count(), 1);
    for (const path of ['/api/seasons/current', '/api/seasons', '/api/seasons/my-history', '/rank/me', '/rank/leaderboard']) assert.ok(seen.has(path), `Missing request: ${path}`);
    await screenshot('normal');

    await open('empty');
    await page.getByText('진행 중인 시즌이 없어요', { exact: true }).waitFor();
    await page.getByText('아직 완료한 시즌이 없어요', { exact: true }).waitFor();
    await page.getByText('첫 랭커를 기다리고 있어요', { exact: true }).waitFor();
    assert.equal(await page.getByRole('progressbar', { name: '배치 진행률' }).getAttribute('value'), '2');
    assert.equal(await page.locator('.error-state').count(), 0, '404 current season is an empty state');
    await screenshot('empty-placement');

    for (const failedState of ['current-error', 'history-error']) {
      await open(failedState);
      assert.equal(await page.getByRole('alert').count(), 1);
      await page.getByText('다음 티어까지 250 RP', { exact: true }).waitFor();
      state = 'normal';
      await page.getByRole('button', { name: '다시 시도', exact: true }).click();
      await page.getByRole('alert').waitFor({ state: 'detached' });
      await page.getByRole('heading', { name: '새로운 시즌', exact: true }).waitFor();
      await page.getByRole('heading', { name: '지난 시즌 2', exact: true }).waitFor();
    }
    await open('catalog-error');
    await page.getByRole('heading', { name: '시즌 2', exact: true }).waitFor();
    await page.getByText('700 코인', { exact: true }).waitFor();
    state = 'normal';
    await page.getByRole('button', { name: '시즌 정보 다시 시도' }).click();
    await page.getByRole('heading', { name: '지난 시즌 2', exact: true }).waitFor();
    await open('top');
    await page.getByText('최고 티어에 도달했어요!', { exact: true }).waitFor();
    await open('last-day');
    await page.getByText('마지막 날', { exact: true }).waitFor();
    await open('expired');
    await page.getByText('종료 집계 대기', { exact: true }).waitFor();
    state = 'normal';
    await page.getByRole('button', { name: '새로고침', exact: true }).click();
    await page.getByText('17일 남음', { exact: true }).waitFor();

    await page.goto(baseUrl);
    await page.getByRole('link', { name: /이번 시즌/ }).click();
    await page.waitForURL(`${baseUrl}/seasons`);
    if (size.name === 'mobile') await page.getByRole('button', { name: '메뉴 열기' }).click();
    await page.getByRole('navigation').getByRole('link', { name: '시즌', exact: true }).click();
    assert.equal(await page.locator('.sidebar.open').count(), 0);
    assert.ok(await page.getByRole('link', { name: '라운드 참여하기' }).getAttribute('href') === '/match');
    checked.push(`${size.name}: API shapes, records, placement, empty states, partial errors/retry, highest tier, season deadline, refresh, navigation`);
    await context.close();
  }
  assert.deepEqual(issues, []);
  await fs.writeFile(new URL('report.json', output), JSON.stringify({ checkedAt: new Date().toISOString(), checked, errors: issues, networkMode: 'mocked-api' }, null, 2));
  console.log(`Season UI checks passed: ${checked.join('; ')}`);
} finally { await browser.close(); }
