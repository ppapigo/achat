import { chromium } from 'playwright';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
const baseUrl = process.env.UI_BASE_URL || 'http://127.0.0.1:5173';
const output = new URL('../achat-front/test-results/ui/', import.meta.url);
const screenshotPath = (name) => fileURLToPath(new URL(name, output));
await fs.mkdir(output, { recursive: true });
const channel = process.env.UI_BROWSER_CHANNEL || (process.platform === 'win32' ? 'msedge' : undefined);
const browser = await chromium.launch({ headless: true, channel });
const issues = [];
const results = [];
const sizes = [{ name: 'desktop', width: 1440, height: 1000 }, { name: 'mobile', width: 390, height: 844 }];
const routes = [['/', '홈'], ['/feed', '피드'], ['/rooms', '채팅방'], ['/rooms/preview-room', '채팅'], ['/boards', '커뮤니티'], ['/boards/1', '게시글'], ['/friends', '친구'], ['/party', '파티'], ['/match', '매칭'], ['/seasons', '시즌'], ['/cards', '카드'], ['/shop', '상점'], ['/profile', '프로필']];
try {
  for (const size of sizes) {
    const context = await browser.newContext({ viewport: { width: size.width, height: size.height }, deviceScaleFactor: 1 });
    const page = await context.newPage();
    page.on('pageerror', (error) => issues.push(`${size.name}: ${error.message}`));
    page.on('request', (request) => { if (request.url().includes('/backend') || /^https?:/.test(request.url()) && new URL(request.url()).origin !== baseUrl) issues.push(`Unexpected network request: ${request.url()}`); });
    page.on('websocket', (socket) => {
      const url = new URL(socket.url());
      if (url.host !== new URL(baseUrl).host || url.pathname !== '/') issues.push(`Unexpected WebSocket: ${socket.url()}`);
    });
    page.on('requestfailed', (request) => issues.push(`Failed request: ${request.url()}`));
    for (const authRoute of ['/login', '/register']) {
      await page.goto(`${baseUrl}${authRoute}`);
      await page.getByRole('heading', { level: 2 }).waitFor();
      await page.screenshot({ path: screenshotPath(`${size.name}-${authRoute.slice(1)}.png`), fullPage: true });
    }
    await page.goto(`${baseUrl}/login`);
    await page.getByRole('button', { name: '로그인 없이 화면 둘러보기' }).click();
    await page.waitForURL(`${baseUrl}/`);
    for (const [route, title] of routes) {
      await page.goto(`${baseUrl}${route}`);
      await page.locator('main h1').waitFor();
      await page.waitForFunction(() => document.querySelectorAll('.loading').length === 0);
      await page.locator('.form-error, .error-state, .inline-error').waitFor({ state: 'detached' });
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
      assert.equal(overflow, false, `${size.name} ${route}: horizontal overflow`);
      if (route === '/boards' || route === '/friends') {
        const searchButton = page.getByRole('button', { name: '검색', exact: true });
        const singleLine = await searchButton.evaluate((button) => {
          const text = document.createRange();
          text.selectNodeContents(button);
          return text.getBoundingClientRect().height < 25;
        });
        assert.ok(singleLine, `${size.name} ${route}: search label wraps`);
      }
      await page.screenshot({ path: screenshotPath(`${size.name}-${route === '/' ? 'home' : route.slice(1).replaceAll('/', '-')}.png`), fullPage: true });
      results.push(`${size.name}: ${title}`);
    }
    await page.goto(`${baseUrl}/rooms`);
    await page.getByRole('button', { name: '채팅방 만들기', exact: true }).click();
    await page.getByRole('dialog').waitFor();
    assert.ok(await page.getByRole('dialog').isVisible());
    await page.screenshot({ path: screenshotPath(`${size.name}-room-modal.png`), fullPage: false });
    await page.keyboard.press('Escape');
    await page.getByRole('dialog').waitFor({ state: 'detached' });
    await page.goto(`${baseUrl}/party`);
    await page.getByRole('button', { name: '파티 만들기', exact: true }).click();
    await page.getByRole('dialog').waitFor();
    await page.getByRole('checkbox').first().check();
    assert.ok(await page.getByRole('button', { name: '친구 1명과 시작하기' }).isEnabled());
    await page.screenshot({ path: screenshotPath(`${size.name}-party-modal.png`), fullPage: false });
    await page.keyboard.press('Escape');
    await page.goto(`${baseUrl}/boards`);
    await page.getByRole('button', { name: '이야기 쓰기', exact: true }).click();
    await page.getByRole('dialog').waitFor();
    await page.screenshot({ path: screenshotPath(`${size.name}-board-modal.png`), fullPage: false });
    await page.keyboard.press('Escape');
    await page.goto(`${baseUrl}/rooms/preview-room`);
    await page.getByRole('textbox', { name: '채팅 메시지' }).fill('화면 검증용 메시지');
    await page.getByRole('button', { name: '메시지 전송' }).click();
    await page.getByText('화면 검증용 메시지', { exact: true }).waitFor();
    await page.getByRole('button', { name: '10분 라운드 시작' }).click();
    const augmentTarget = page.getByRole('combobox', { name: '증강을 받을 상대' });
    await augmentTarget.waitFor();
    assert.equal(await augmentTarget.locator('option').count(), 4, 'Only the three opponents can be selected');
    assert.equal(await page.locator('.augment-option').first().isEnabled(), false, 'Choose a target before an augment');
    await augmentTarget.selectOption({ label: '작은책방' });
    await page.locator('.augment-option').first().click();
    await page.getByRole('button', { name: '증강 보내기 확정' }).click();
    await page.getByText('적용 대상: 작은책방', { exact: true }).waitFor();
    assert.equal(await page.locator('.augment-option').count(), 0, 'Completed picks cannot be changed');
    await page.getByRole('link', { name: '채팅방 둘러보기', exact: true }).click();
    await page.getByRole('link', { name: /퇴근 후, 가벼운 수다/ }).click();
    await page.getByText('적용 대상: 작은책방', { exact: true }).waitFor();
    await page.screenshot({ path: screenshotPath(`${size.name}-augment-target.png`), fullPage: true });
    await page.goto(`${baseUrl}/shop`);
    await page.getByRole('button', { name: '칭호', exact: true }).click();
    assert.equal(await page.locator('.shop-card').count(), 1);
    await page.getByRole('button', { name: '내 보관함', exact: true }).click();
    await page.getByRole('button', { name: '장착 해제', exact: true }).waitFor();
    for (const state of ['empty', 'error']) {
      await page.goto(`${baseUrl}/rooms?ui-state=${state}`);
      await page.locator(state === 'empty' ? '.empty' : '.error-state').waitFor();
      await page.screenshot({ path: screenshotPath(`${size.name}-${state}.png`), fullPage: true });
    }
    if (size.name === 'mobile') {
      await page.goto(`${baseUrl}/`);
      await page.getByRole('button', { name: '메뉴 열기', exact: true }).click();
      assert.ok(await page.locator('.sidebar.open').isVisible());
      await page.waitForFunction(() => {
        const bounds = document.querySelector('.sidebar.open')?.getBoundingClientRect();
        return bounds && Math.abs(bounds.x) < 1 && bounds.width >= 230;
      });
      await page.screenshot({ path: screenshotPath('mobile-navigation.png'), fullPage: false });
      await page.getByRole('navigation').getByRole('link', { name: '친구', exact: true }).click();
      await page.waitForURL(`${baseUrl}/friends`);
      assert.equal(await page.locator('.sidebar.open').count(), 0);
    }
    await context.close();
  }
  assert.deepEqual(issues, [], `Browser errors: ${JSON.stringify(issues)}`);
  await fs.writeFile(new URL('report.json', output), JSON.stringify({ checkedAt: new Date().toISOString(), viewports: sizes, checked: results, errors: issues, networkMode: 'frontend-only' }, null, 2));
  console.log(`UI checks passed: ${results.length} route/device combinations, auth, modals, chat, shop, navigation, empty/error states. No backend requests.`);
} finally { await browser.close(); }
