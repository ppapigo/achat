// Synthetic data for the explicit ui-preview development mode. No network requests.
const timestamp = (minutes = 0) => new Date(Date.now() - minutes * 60000).toISOString();
const copy = (value) => JSON.parse(JSON.stringify(value));
const user = { id: 'preview-me', email: 'preview@example.test', username: '초록빛하루', coins: 3600, exp: 160, expToNextLevel: 240, level: 4, rp: 720, tier: 'GOLD', placementCompleted: true, admin: false };
let rooms = [
  { id: 'preview-room', name: '퇴근 후, 가벼운 수다', ownerId: user.id, maxMembers: 10, currentMembers: 4 },
  { id: 'room-books', name: '책 한 권, 이야기 한 잔', ownerId: 'preview-friend', maxMembers: 8, currentMembers: 3 },
  { id: 'room-weekend', name: '이번 주말에는 뭐 할까요?', ownerId: 'preview-friend', maxMembers: 6, currentMembers: 2 },
  { id: 'room-music', name: '지금 듣는 노래를 나눠요', ownerId: user.id, maxMembers: 10, currentMembers: 5 },
];
let friends = [{ id: 'preview-friend', username: '느긋한오후', online: true, currentRoomId: 'preview-room' }, { id: 'preview-friend-2', username: '작은책방', online: false, currentRoomId: null }];
let requests = [{ id: 'request-1', from: { id: 'preview-neighbor', username: '노을산책' }, createdAt: timestamp(25) }];
let invites = [{ id: 'invite-1', from: friends[0], room: { id: 'preview-room', name: rooms[0].name }, expiresAt: timestamp(-10) }];
let parties = [{ id: 'preview-party', name: '오늘의 작은 모임', memberCount: 3, memberNames: [user.username, ...friends.map((friend) => friend.username)] }];
const labels = { FREE: '자유', QUESTION: '질문', GUIDE: '공략', TRADE: '거래', NOTICE: '공지' };
let boards = [
  { id: 1, authorId: user.id, authorNickname: user.username, category: 'FREE', categoryName: '자유', title: '오늘의 작은 행복은 무엇이었나요?', content: '퇴근길에 예쁜 하늘을 봤어요.\n\n바쁜 하루였지만 잠깐 멈춰서 바라보니 기분이 좋아지더라고요. 여러분의 하루에는 어떤 작은 행복이 있었나요?', viewCount: 24, likeCount: 8, liked: false, commentCount: 2, createdAt: timestamp(50), comments: [{ id: 1, authorId: 'preview-friend', authorNickname: '느긋한오후', content: '따뜻한 커피 한 잔이요. 이런 이야기 좋네요!', createdAt: timestamp(20), replies: [{ id: 2, authorId: user.id, authorNickname: user.username, content: '커피 한 잔의 여유, 저도 좋아해요 ☕', createdAt: timestamp(10), replies: [] }] }] },
  { id: 2, authorId: 'preview-friend', authorNickname: '느긋한오후', category: 'QUESTION', categoryName: '질문', title: '처음 만난 사람과 자연스럽게 대화하는 팁', content: '새로운 사람과 이야기할 때 어떤 질문으로 시작하시나요? 편하게 대화를 이어가는 방법이 궁금해요.', viewCount: 17, likeCount: 3, liked: false, commentCount: 0, comments: [], createdAt: timestamp(120) },
  { id: 3, authorId: 'preview-friend-2', authorNickname: '작은책방', category: 'GUIDE', categoryName: '공략', title: '증강을 고를 때 알아두면 좋은 것들', content: '나의 대화 스타일에 맞는 증강을 찾아보세요. 여러 가지를 선택하며 가장 편한 조합을 발견할 수 있어요.', viewCount: 39, likeCount: 12, liked: false, commentCount: 0, comments: [], createdAt: timestamp(240) },
];
let posts = [{ id: 'post-1', authorId: user.id, authorUsername: user.username, content: '오늘은 평소보다 조금 일찍 퇴근했어요. 여러분은 어떤 하루를 보내셨나요? ☘', type: 'FREE', likeCount: 4, commentCount: 2, liked: false, tags: [], createdAt: timestamp(12) }, { id: 'post-2', authorId: 'preview-friend', authorUsername: '느긋한오후', content: '오랜만에 읽던 책을 마저 읽었어요. 작은 목표를 하나 완성한 기분이네요.', type: 'FREE', likeCount: 7, commentCount: 1, liked: false, tags: [], createdAt: timestamp(40) }];
const cards = [{ cardId: 1, targetId: 'preview-friend', username: '느긋한오후', level: 5, tier: 'GOLD', rp: 860, isOnline: true, isFriend: true, drawnDate: timestamp(1440).slice(0, 10) }, { cardId: 2, targetId: 'preview-neighbor', username: '노을산책', level: 3, tier: 'SILVER', rp: 430, isOnline: false, isFriend: false, drawnDate: timestamp(2880).slice(0, 10) }];
let drawnToday = false;
const items = [
  { id: 'title-newbie', name: '대화의 새싹', description: '처음 시작하는 나에게 작은 응원을.', category: 'TITLE', type: 'ENTITLEMENT', price: 100, rarity: 'COMMON', equipSlot: 'TITLE', owned: true },
  { id: 'emoji-leaf', name: '초록 잎 이모지', description: '대화에 싱그러운 마음을 더해요.', category: 'EMOJI', type: 'ENTITLEMENT', price: 200, rarity: 'UNCOMMON', equipSlot: 'EMOJI', owned: false },
  { id: 'border-star', name: '별빛 테두리', description: '내 프로필에 은은하게 빛나는 별.', category: 'BORDER', type: 'ENTITLEMENT', price: 800, rarity: 'RARE', equipSlot: 'BORDER', owned: false },
  { id: 'background-forest', name: '작은 숲의 오후', description: '고요한 숲의 분위기를 담은 배경.', category: 'BACKGROUND', type: 'ENTITLEMENT', price: 500, rarity: 'RARE', equipSlot: 'BACKGROUND', owned: false },
  { id: 'furniture-plant', name: '창가의 작은 화분', description: '우리의 공간에 초록빛을 더해요.', category: 'FURNITURE', type: 'ENTITLEMENT', price: 300, rarity: 'COMMON', equipSlot: null, owned: false },
  { id: 'reroll-ticket', name: '증강 리롤권', description: '새로운 가능성을 한 번 더 만나보세요.', category: 'REROLL', type: 'CONSUMABLE', price: 50, rarity: 'COMMON', equipSlot: null, owned: false },
];
const inventory = [{ ...items[0], itemId: items[0].id, quantity: 1, acquiredAt: timestamp(1440) }];
const equipment = { TITLE: { itemId: items[0].id, name: items[0].name } };
const home = { ownerId: user.id, nickname: user.username, statusMessage: '작은 대화가 모여 즐거운 하루가 되길.', backgroundTheme: 'forest', visitCount: 12 };
const jobs = [{ type: 'FREELANCER', name: '프리랜서', description: '다양한 대화를 자유롭게 탐험해요.', passiveDescription: '새로운 스타일을 찾아보세요.' }, { type: 'GUARDIAN', name: '가디언', description: '따뜻한 분위기로 대화의 중심을 지켜요.', passiveDescription: '꾸준한 참여에 보너스가 더해져요.' }, { type: 'CONNECTOR', name: '커넥터', description: '사람과 이야기를 자연스럽게 이어줘요.', passiveDescription: '새로운 연결에서 힘을 얻어요.' }];
let job = { jobType: 'GUARDIAN', jobName: '가디언', level: 2, exp: 40, expForNextLevel: 100 };
const history = { stats: { totalGames: 12, winRate: 58.3, bestScore: 280 }, recentGames: [{ id: 'game-1', roomName: '편안한 오후의 대화', isWin: true, rank: 1, totalPoints: 280, rpChange: 20, playedAt: timestamp(1440) }, { id: 'game-2', roomName: '매치 #초록', isWin: false, rank: 3, totalPoints: 160, rpChange: -5, playedAt: timestamp(2880) }] };
const messages = {};
const rounds = {};
const selections = {};
const options = [{ type: 'HARMONY_AURA', name: '하모니 오라', description: '대화에 따뜻한 분위기를 더해요.' }, { type: 'THREAD_WEAVER', name: '쓰레드 위버', description: '답장이 시각적으로 연결됩니다.' }, { type: 'TURBO_CASTER', name: '터보 캐스터', description: '메시지 쿨타임을 줄여줘요.' }];
let waiting = false;
let sequence = 100;
const nextId = () => ++sequence;
const changed = (roomId) => { if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('achat:preview-chat', { detail: { roomId } })); };
function roomMessages(roomId) { return messages[roomId] ||= [{ id: `hello-${roomId}`, roomId, senderId: 'preview-friend', content: '어서 오세요! 오늘 하루는 어땠나요?', appliedContent: '어서 오세요! 오늘 하루는 어땠나요?', createdAt: timestamp(8), likeCount: 2, likedByUserIds: [] }, { id: `mine-${roomId}`, roomId, senderId: user.id, content: '반가워요. 잠깐 쉬어가며 이야기하고 싶었어요 ☘', createdAt: timestamp(6), likeCount: 1, likedByUserIds: ['preview-friend'] }, { id: `next-${roomId}`, roomId, senderId: 'preview-friend', content: '좋아요. 편하게 이야기해요!', createdAt: timestamp(5), likeCount: 0, likedByUserIds: [] }]; }
function result(data, status = 200) { return new Response(JSON.stringify(status === 200 ? { success: true, data: copy(data) } : { success: false, error: data }), { status, headers: { 'Content-Type': 'application/json' } }); }

export async function previewFetch(input, { method = 'GET', body, signal } = {}) {
  if (signal?.aborted) throw new DOMException('취소됨', 'AbortError');
  const url = new URL(input, 'http://preview.local'); const path = url.pathname.replace(/^\/backend/, ''); const payload = body ? JSON.parse(body) : {};
  const viewState = typeof window === 'undefined' ? '' : new URLSearchParams(window.location.search).get('ui-state');
  if (viewState === 'error' && !path.startsWith('/auth/')) return result('화면 검증용 오류 상태입니다. 정상 화면은 주소에서 ui-state를 제거해주세요.', 503);
  if (path === '/auth/me') return result(user);
  if (['/auth/login', '/auth/refresh'].includes(path)) return result({ accessToken: 'ui-preview-only', refreshToken: 'ui-preview-refresh', userId: user.id, email: user.email });
  if (['/auth/register', '/auth/verify', '/auth/resend-code'].includes(path)) return result({ userId: user.id, email: payload.email, verified: path === '/auth/verify' });
  if (path === '/auth/profile') { user.username = payload.username; return result({ username: user.username }); }
  if (path === '/rooms' && method === 'POST') { const room = { ...payload, id: `room-${nextId()}`, ownerId: user.id, currentMembers: 1 }; rooms.push(room); return result(room); }
  if (path === '/rooms') return result(viewState === 'empty' ? [] : rooms);
  const roomMatch = path.match(/^\/rooms\/([^/]+)(?:\/(.+))?$/);
  if (roomMatch) {
    const [, roomId, action] = roomMatch; const room = rooms.find((item) => item.id === roomId) || { id: roomId, name: '우리의 작은 파티', ownerId: user.id, currentMembers: 3, maxMembers: 10 };
    if (!action || action === 'join') return result(room);
    if (action === 'leave') return result('방을 나갔어요.');
    if (action === 'messages') return result(viewState === 'empty' ? [] : roomMessages(roomId));
    if (action === 'round') return result(rounds[roomId] || { phase: 'WAITING', scores: [] });
    if (action === 'round/start') { rounds[roomId] = { phase: 'PICKING', scores: [{ userId: user.id, rank: 1, totalPoints: 12 }, { userId: 'preview-friend', rank: 2, totalPoints: 8 }] }; changed(roomId); return result(rounds[roomId]); }
    if (action === 'augments/options') return result({ options, selected: selections[roomId] || null });
    if (action === 'augments/pick') { selections[roomId] = payload.type; return result({ options, selected: payload.type }); }
    if (action === 'preview-chat/chat') { roomMessages(roomId).push({ ...payload, id: `message-${nextId()}`, roomId, senderId: user.id, createdAt: timestamp(), likeCount: 0, likedByUserIds: [], replyToPreview: roomMessages(roomId).find((item) => item.id === payload.replyToId)?.content }); changed(roomId); return result(true); }
    if (action === 'preview-chat/like') { const message = roomMessages(roomId).find((item) => item.id === payload.messageId); if (message) { const liked = message.likedByUserIds.includes(user.id); message.likedByUserIds = liked ? message.likedByUserIds.filter((id) => id !== user.id) : [...message.likedByUserIds, user.id]; message.likeCount += liked ? -1 : 1; } changed(roomId); return result(true); }
  }
  if (path === '/api/feed') return result({ posts: viewState === 'empty' ? [] : posts, hasMore: false });
  if (path === '/api/me/posts') { const post = { ...payload, id: `post-${nextId()}`, authorId: user.id, authorUsername: user.username, likeCount: 0, commentCount: 0, liked: false, createdAt: timestamp() }; posts.unshift(post); return result(post); }
  if (path.match(/^\/api\/posts\/[^/]+\/like$/)) { const post = posts.find((item) => item.id === path.split('/')[3]); if (post) { post.liked = !post.liked; post.likeCount += post.liked ? 1 : -1; } return result(post); }
  if (path === '/api/boards' && method === 'POST') { const board = { ...payload, id: nextId(), authorId: user.id, authorNickname: user.username, categoryName: labels[payload.category], comments: [], commentCount: 0, likeCount: 0, viewCount: 0, liked: false, createdAt: timestamp() }; boards.unshift(board); return result(board); }
  if (path === '/api/boards') { const list = boards.filter((board) => (!url.searchParams.get('category') || board.category === url.searchParams.get('category')) && board.title.includes(url.searchParams.get('keyword') || '')); return result({ boards: viewState === 'empty' ? [] : list, hasNext: false, page: 0, total: list.length }); }
  const boardMatch = path.match(/^\/api\/boards\/(\d+)(?:\/(.*))?$/);
  if (boardMatch) {
    const board = boards.find((entry) => entry.id === Number(boardMatch[1])); if (!board) return result('이야기를 찾을 수 없어요.', 404);
    const action = boardMatch[2];
    if (!action) { if (method === 'DELETE') boards = boards.filter((item) => item !== board); else if (method === 'PUT') Object.assign(board, payload, { categoryName: labels[payload.category] }); return result(board); }
    if (action === 'like') { board.liked = !board.liked; board.likeCount += board.liked ? 1 : -1; return result({ likeCount: board.likeCount }); }
    if (action === 'comments') { const comment = { id: nextId(), content: payload.content, authorId: user.id, authorNickname: user.username, createdAt: timestamp(), replies: [] }; const parent = board.comments.find((item) => item.id === payload.parentId); (parent ? parent.replies : board.comments).push(comment); board.commentCount++; return result(comment); }
    if (action.startsWith('comments/')) { const id = Number(action.split('/')[1]); const list = [board.comments, ...board.comments.map((item) => item.replies)].find((group) => group.some((entry) => entry.id === id)); const comment = list?.find((entry) => entry.id === id); if (comment && method === 'PUT') comment.content = payload.content; if (comment && method === 'DELETE') { list.splice(list.indexOf(comment), 1); board.commentCount--; } return result(comment || {}); }
  }
  if (path === '/api/friends') return result(viewState === 'empty' ? [] : friends);
  if (path === '/api/friends/search') return result([...friends.map((friend) => ({ ...friend, isFriend: true })), { id: 'preview-new', username: '새로운이웃', isFriend: false }].filter((person) => person.username.includes(url.searchParams.get('q') || '')));
  if (path === '/api/friends/request') return result({ requestId: `request-${nextId()}` });
  if (path === '/api/friends/requests') return result(viewState === 'empty' ? [] : requests);
  if (path === '/api/friends/invites') return result(viewState === 'empty' ? [] : invites);
  if (path.match(/^\/api\/friends\/requests\/.+\/(accept|reject)$/)) { const id = path.split('/')[4]; const request = requests.find((item) => item.id === id); if (path.endsWith('/accept') && request) friends.push({ ...request.from, online: false }); requests = requests.filter((item) => item.id !== id); return result(true); }
  if (path.match(/^\/api\/friends\/invites\/.+\/(accept|reject)$/)) { invites = invites.filter((item) => item.id !== path.split('/')[4]); return result({ roomId: 'preview-room' }); }
  if (path.startsWith('/api/friends/') && method === 'DELETE') { friends = friends.filter((friend) => friend.id !== path.split('/')[3]); return result(true); }
  if (path === '/api/party/my') return result(viewState === 'empty' ? [] : parties);
  if (path === '/api/party/create') { const id = `party-${nextId()}`; const members = friends.filter((friend) => payload.friendIds.includes(friend.id)); parties.push({ id, name: payload.name || '새로운 작은 모임', memberCount: members.length + 1, memberNames: [user.username, ...members.map((friend) => friend.username)] }); return result({ partyId: id }); }
  if (path.match(/^\/api\/party\/.+\/leave$/)) { parties = parties.filter((party) => party.id !== path.split('/')[3]); return result(true); }
  if (path === '/match/status') return result({ waiting, queueSize: waiting ? 3 : 0, waitTime: waiting ? 9 : 0 });
  if (path === '/match/join' || path === '/match/leave') { waiting = path.endsWith('/join'); return result({ waiting, queueSize: waiting ? 3 : 0 }); }
  if (path === '/api/cards/status') return result({ drawnToday, remainingDraws: drawnToday ? 0 : 1, totalCards: cards.length });
  if (path === '/api/cards/my') return result(viewState === 'empty' ? [] : cards);
  if (path === '/api/cards/draw') { if (drawnToday) return result({ success: false, alreadyDrawnToday: true }); drawnToday = true; const card = { ...cards[1], cardId: nextId(), username: '숲속의별', drawnDate: timestamp().slice(0, 10) }; cards.unshift(card); return result({ success: true, card }); }
  if (path === '/api/shop/items') return result(viewState === 'empty' ? [] : items);
  if (path === '/api/shop/inventory') return result({ items: viewState === 'empty' ? [] : inventory });
  if (path === '/api/shop/equipped') return result({ slots: equipment });
  if (path === '/api/shop/purchase') { const item = items.find((entry) => entry.id === payload.itemId); if (!item || item.price > user.coins) return result('코인이 부족해요.', 400); user.coins -= item.price; item.owned = item.type !== 'CONSUMABLE'; const owned = inventory.find((entry) => entry.itemId === item.id); if (owned) owned.quantity++; else inventory.push({ ...item, itemId: item.id, quantity: 1 }); return result({ remainingCoins: user.coins }); }
  if (path === '/api/shop/equip') { const item = items.find((entry) => entry.id === payload.itemId); if (item) equipment[item.equipSlot] = { itemId: item.id, name: item.name }; return result(true); }
  if (path === '/api/shop/unequip') { delete equipment[payload.slot]; return result(true); }
  if (path === '/api/shop/use') { const item = inventory.find((entry) => entry.itemId === payload.itemId); if (item) item.quantity--; return result(true); }
  if (path === '/api/me/home') { if (method === 'PATCH') Object.assign(home, payload); return result(home); }
  if (path === '/jobs') return result(jobs);
  if (path === '/jobs/me') return result(job);
  if (path === '/jobs/select') { const selected = jobs.find((entry) => entry.type === payload.jobType); job = { ...job, jobType: selected.type, jobName: selected.name }; return result(job); }
  if (path === '/history/summary') return result(viewState === 'empty' ? { ...history, recentGames: [] } : history);
  return result(`미리보기에서 지원하지 않는 요청입니다: ${path}`, 404);
}
