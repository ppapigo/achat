import { chromium } from 'playwright';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';

// Use a normal Vite server. Mocked APIs keep all test writes out of the real DB.
const baseUrl = process.env.FEED_UI_BASE_URL || 'http://127.0.0.1:5176';
const output = 'achat-front/test-results/feed';
await fs.mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true, channel: process.env.UI_BROWSER_CHANNEL || (process.platform === 'win32' ? 'msedge' : undefined) });
const issues = [];
let currentPage;
try {
  for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }]) {
    const context = await browser.newContext({ viewport });
    await context.addInitScript(() => sessionStorage.setItem('achat.session', JSON.stringify({ accessToken: 'feed-test', refreshToken: 'feed-refresh' })));
    const page = await context.newPage();
    currentPage = page;
    page.on('pageerror', (error) => issues.push(error.message));
    const calls = [];
    const user = { id: 'me', username: '피드테스트', email: 'feed@example.test', coins: 1000, level: 1, tier: 'UNRANKED' };
    const posts = Array.from({ length: 24 }, (_, index) => ({ id: `post-${index + 1}`, authorId: 'friend', authorUsername: '대화친구', type: 'FREE', content: `피드 소식 ${index + 1}`, tags: [], likeCount: 0, commentCount: index === 0 ? 1 : 0, liked: false, createdAt: new Date(Date.now() - index * 60000).toISOString() }));
    let comments = [{ id: 'other-comment', authorId: 'friend', authorUsername: '대화친구', content: '친구의 댓글', createdAt: new Date().toISOString() }];
    let rejectPost = true, rejectMore = true, rejectComments = true, rejectComment = true, rejectFeed = false, empty = false;
    await page.route('**/backend/**', async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      const path = url.pathname.replace(/^\/backend/, '');
      const method = request.method();
      calls.push({ path, method, cursor: url.searchParams.get('cursor') });
      const ok = (data) => route.fulfill({ json: { data } });
      const error = (message) => route.fulfill({ status: 503, json: { message } });
      if (path === '/auth/me') return ok(user);
      if (path === '/rooms' || path === '/api/friends') return ok([]);
      if (path === '/api/boards') return ok({ boards: [{ id: 1, title: '커뮤니티 게시글', category: 'FREE', categoryName: '자유', authorNickname: '게시판친구', likeCount: 0, commentCount: 0, createdAt: new Date().toISOString() }], hasNext: false });
      if (path === '/api/feed') {
        if (rejectFeed) return error('피드 조회 실패');
        const cursor = url.searchParams.get('cursor');
        if (cursor && rejectMore) { rejectMore = false; return error('추가 조회 실패'); }
        const start = cursor ? posts.findIndex((post) => post.id === cursor) + 1 : 0;
        const limit = Number(url.searchParams.get('limit'));
        const list = empty ? [] : posts.slice(start, start + limit);
        const hasMore = !empty && start + limit < posts.length;
        return ok({ posts: list, hasMore, nextCursor: hasMore ? list.at(-1).id : null });
      }
      if (path === '/api/me/posts' && method === 'POST') {
        assert.deepEqual(request.postDataJSON(), { type: 'FREE', content: '내 새 피드', tags: [] });
        if (rejectPost) { rejectPost = false; return error('작성 실패'); }
        const post = { ...posts[0], id: 'new-post', authorId: user.id, authorUsername: user.username, content: '내 새 피드', commentCount: 0 };
        posts.unshift(post); return ok(post);
      }
      if (path === '/api/posts/post-1/like') {
        const post = posts.find((item) => item.id === 'post-1');
        post.liked = !post.liked; post.likeCount = post.liked ? 1 : 0;
        return ok({ liked: post.liked, likeCount: post.likeCount });
      }
      if (path === '/api/feed/post-1/comments') {
        if (method === 'GET') {
          if (rejectComments) return error('댓글 조회 실패');
          return ok(comments);
        }
        assert.equal(request.postDataJSON().content, '내 피드 댓글');
        if (rejectComment) { rejectComment = false; return error('댓글 작성 실패'); }
        const comment = { id: 'my-comment', authorId: user.id, authorUsername: user.username, content: '내 피드 댓글', createdAt: new Date().toISOString() };
        comments.push(comment); posts.find((post) => post.id === 'post-1').commentCount = comments.length;
        return ok(comment);
      }
      if (path === '/api/feed/post-1/comments/my-comment' && method === 'DELETE') {
        comments = comments.filter((comment) => comment.id !== 'my-comment');
        posts.find((post) => post.id === 'post-1').commentCount = comments.length;
        return ok({ message: '삭제됨' });
      }
      issues.push(`Unexpected request: ${method} ${path}`);
      return route.fulfill({ status: 404, json: { message: 'Unexpected API' } });
    });
    const navigate = async (label) => {
      if (viewport.width < 760) await page.getByRole('button', { name: '메뉴 열기' }).click();
      await page.getByRole('navigation').getByRole('link', { name: label, exact: true }).click();
      if (viewport.width < 760) await page.waitForFunction(() => document.querySelector('.sidebar').getBoundingClientRect().right <= 0);
    };
    await page.goto(baseUrl);
    await page.getByText('피드 소식 3', { exact: true }).waitFor();
    assert.equal(await page.locator('.feed-card').count(), 3);
    assert.equal(await page.getByRole('link', { name: '피드 전체 보기' }).getAttribute('href'), '/feed');
    await page.getByRole('link', { name: '피드 전체 보기' }).click();
    await page.getByRole('heading', { level: 1, name: '피드', exact: true }).waitFor();
    await page.getByText('피드 소식 20', { exact: true }).waitFor();
    assert.equal(await page.locator('.feed-card').count(), 20);
    assert.equal(calls.some((call) => call.path.startsWith('/api/boards')), false);
    assert.equal(await page.getByRole('button', { name: '피드 올리기' }).isEnabled(), false);
    await page.getByLabel('피드 내용', { exact: true }).fill('   ');
    assert.equal(await page.getByRole('button', { name: '피드 올리기' }).isEnabled(), false);
    await page.getByLabel('피드 내용', { exact: true }).fill('내 새 피드');
    await page.getByRole('button', { name: '피드 올리기' }).click();
    await page.getByRole('alert').filter({ hasText: '작성 실패' }).waitFor();
    assert.equal(await page.getByLabel('피드 내용', { exact: true }).inputValue(), '내 새 피드');
    await page.getByRole('button', { name: '피드 올리기' }).click();
    await page.locator('.feed-card').getByText('내 새 피드', { exact: true }).waitFor();
    assert.equal(await page.getByLabel('피드 내용', { exact: true }).inputValue(), '');
    await page.getByRole('button', { name: '이전 소식 더 보기' }).click();
    await page.getByRole('alert').filter({ hasText: '추가 조회 실패' }).waitFor();
    assert.equal(await page.locator('.feed-card').count(), 20);
    await page.getByRole('button', { name: '다시 시도', exact: true }).click();
    await page.getByText('피드 소식 24', { exact: true }).waitFor();
    assert.equal(await page.locator('.feed-card').count(), 25);
    assert.equal(await page.getByRole('button', { name: '이전 소식 더 보기' }).count(), 0);
    assert.equal(calls.filter((call) => call.cursor).length, 2);
    const card = page.locator('.feed-card').filter({ has: page.getByText('피드 소식 1', { exact: true }) });
    await card.getByRole('button', { name: '좋아요 0개' }).click();
    await card.getByRole('button', { name: '좋아요 1개', pressed: true }).waitFor();
    assert.equal(await page.locator('.feed-card').count(), 25, 'Liking must preserve loaded pages');
    await card.getByRole('button', { name: '좋아요 1개' }).click();
    await card.getByRole('button', { name: '좋아요 0개', pressed: false }).waitFor();
    await card.getByRole('button', { name: '댓글 1', exact: true }).click();
    await card.getByRole('alert').filter({ hasText: '댓글 조회 실패' }).waitFor();
    rejectComments = false;
    await card.getByRole('button', { name: '다시 시도' }).click();
    await card.getByText('친구의 댓글', { exact: true }).waitFor();
    assert.equal(await card.getByRole('button', { name: '댓글 삭제' }).count(), 0);
    await card.getByLabel('피드 댓글 내용').fill('내 피드 댓글');
    await card.getByRole('button', { name: '댓글 남기기' }).click();
    await card.getByRole('alert').filter({ hasText: '댓글 작성 실패' }).waitFor();
    assert.equal(await card.getByLabel('피드 댓글 내용').inputValue(), '내 피드 댓글');
    await card.getByRole('button', { name: '댓글 남기기' }).click();
    await card.getByText('내 피드 댓글', { exact: true }).waitFor();
    await card.getByRole('button', { name: '댓글 2', exact: true }).waitFor();
    assert.equal(await card.getByRole('button', { name: '댓글 삭제' }).count(), 1);
    await card.scrollIntoViewIfNeeded();
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false);
    await page.screenshot({ path: `${output}/${viewport.width}-comments.png` });
    page.once('dialog', (dialog) => dialog.dismiss());
    await card.getByRole('button', { name: '댓글 삭제' }).click();
    assert.equal(await card.getByText('내 피드 댓글', { exact: true }).count(), 1);
    page.once('dialog', (dialog) => dialog.accept());
    await card.getByRole('button', { name: '댓글 삭제' }).click();
    await card.getByText('내 피드 댓글', { exact: true }).waitFor({ state: 'detached' });
    await card.getByRole('button', { name: '댓글 1', exact: true }).waitFor();
    await navigate('커뮤니티');
    await page.getByText('커뮤니티 게시글', { exact: true }).waitFor();
    assert.equal(new URL(page.url()).pathname, '/boards');
    assert.equal(await page.locator('.feed-card').count(), 0);
    await navigate('피드');
    await page.locator('.feed-card').getByText('내 새 피드', { exact: true }).waitFor();
    assert.equal(new URL(page.url()).pathname, '/feed');
    await page.screenshot({ path: `${output}/${viewport.width}-feed.png` });
    empty = true;
    await page.getByRole('button', { name: '새로고침' }).click();
    await page.getByText('아직 올라온 소식이 없어요', { exact: true }).waitFor();
    rejectFeed = true;
    await page.reload();
    await page.getByRole('alert').filter({ hasText: '피드 조회 실패' }).waitFor();
    rejectFeed = false; empty = false;
    await page.getByRole('button', { name: '다시 시도' }).click();
    await page.locator('.feed-card').getByText('내 새 피드', { exact: true }).waitFor();
    assert.equal(calls.some((call) => call.path.startsWith('/api/boards') && call.method !== 'GET'), false);
    await context.close();
    console.log(`Feed checks passed at ${viewport.width}px: separate navigation, posting, cursor paging/retry, likes, comments/ownership, empty/error states.`);
  }
  assert.deepEqual(issues, []);
} catch (error) {
  if (currentPage && !currentPage.isClosed()) await currentPage.screenshot({ path: `${output}/failure.png`, fullPage: true });
  throw error;
} finally { await browser.close(); }
