import { chromium } from 'playwright';
import assert from 'node:assert/strict';

// Run against a normal Vite dev server or production preview. API calls are mocked.
const baseUrl = process.env.AUTH_UI_BASE_URL || 'http://127.0.0.1:5176';
const browser = await chromium.launch({
  headless: true,
  channel: process.env.UI_BROWSER_CHANNEL || (process.platform === 'win32' ? 'msedge' : undefined),
});
const issues = [];

try {
  for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }]) {
    const context = await browser.newContext({ viewport });
    const page = await context.newPage();
    page.on('pageerror', (error) => issues.push(error.message));
    const calls = [];
    let rejectRegistration = true;
    const email = 'signup@example.com';
    const password = 'password123!';
    const user = { id: 'signup-user', email, username: '가입테스트', coins: 1000, level: 1, tier: 'UNRANKED' };
    await page.route('**/backend/**', async (route) => {
      const request = route.request();
      const path = new URL(request.url()).pathname.replace(/^\/backend/, '');
      calls.push(path);
      const ok = (data) => route.fulfill({ json: { success: true, data } });
      if (path === '/auth/register') {
        assert.equal(request.method(), 'POST');
        assert.deepEqual(request.postDataJSON(), { email, password });
        if (rejectRegistration) {
          return route.fulfill({ status: 400, json: { success: false, error: '이미 등록된 이메일입니다' } });
        }
        return ok({ userId: user.id, email, message: '회원가입이 완료되었습니다' });
      }
      if (path === '/auth/login') {
        assert.deepEqual(request.postDataJSON(), { email, password });
        return ok({ userId: user.id, email, accessToken: 'auth-ui-access', refreshToken: 'auth-ui-refresh' });
      }
      if (path === '/auth/me') {
        assert.equal(request.headers().authorization, 'Bearer auth-ui-access');
        return ok(user);
      }
      if (path === '/api/feed') return ok({ posts: [] });
      if (['/rooms', '/api/friends'].includes(path)) return ok([]);
      issues.push(`Unexpected API request: ${path}`);
      return route.fulfill({ status: 404, json: { success: false, error: 'Unexpected API' } });
    });

    await page.goto(`${baseUrl}/register`);
    await page.getByRole('heading', { name: '첫 대화를 시작해볼까요?' }).waitFor();
    assert.equal(await page.getByText(/이메일 인증|인증 코드|인증 완료/).count(), 0);
    await page.getByLabel('이메일', { exact: true }).fill(email);
    await page.getByLabel(/^비밀번호/).fill(password);
    await page.getByRole('button', { name: '함께 시작하기' }).click();
    await page.getByRole('alert').filter({ hasText: '이미 등록된 이메일입니다' }).waitFor();
    assert.equal(new URL(page.url()).pathname, '/register');
    assert.equal(await page.getByLabel(/^비밀번호/).inputValue(), password);
    assert.equal(await page.getByRole('button', { name: '함께 시작하기' }).isEnabled(), true);

    rejectRegistration = false;
    await page.getByRole('button', { name: '함께 시작하기' }).click();
    await page.waitForURL(`${baseUrl}/login`);
    await page.getByText('회원가입이 완료됐어요. 로그인해주세요.', { exact: true }).waitFor();
    assert.equal(await page.getByText(/이메일 인증|인증 코드|인증 완료/).count(), 0);
    assert.equal(await page.getByLabel(/^비밀번호/).inputValue(), '');
    assert.equal(await page.evaluate(() => sessionStorage.getItem('achat.session')), null);
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1), false);
    assert.deepEqual(calls, ['/auth/register', '/auth/register']);

    await page.getByLabel('이메일', { exact: true }).fill(email);
    await page.getByLabel(/^비밀번호/).fill(password);
    await page.getByRole('button', { name: '로그인', exact: true }).click();
    await page.waitForURL(`${baseUrl}/`);
    await page.getByRole('heading', { name: '가입테스트님, 반가워요.' }).waitFor();
    const session = await page.evaluate(() => JSON.parse(sessionStorage.getItem('achat.session')));
    assert.equal(session.accessToken, 'auth-ui-access');
    assert.deepEqual(calls.filter((path) => path.startsWith('/auth/')),
      ['/auth/register', '/auth/register', '/auth/login', '/auth/me']);
    await context.close();
  }
  assert.deepEqual(issues, []);
  console.log('Auth UI passed on desktop and mobile: signup errors, signup → login, session creation, no verification UI or requests.');
} finally {
  await browser.close();
}
