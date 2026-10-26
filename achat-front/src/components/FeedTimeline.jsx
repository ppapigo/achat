import { useEffect, useId, useState } from 'react';
import { Heart, MessageCircle, RefreshCw, Send } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext.jsx';
import { useToast } from '../contexts/ToastContext.jsx';
import { useResource } from '../hooks/useResource.js';
import { useFeed } from '../hooks/useFeed.js';
import { api } from '../lib/api.js';
import { date } from '../lib/format.js';
import { ActionButton, Avatar, Empty, ErrorState, Loading, ResourceState } from './UI.jsx';

const postTypes = { FREE: '일상', GAME_LOG: '라운드 기록', ACHIEVEMENT: '달성 소식', QUESTION: '질문' };

function FeedComposer({ onPublished }) {
  const { user } = useAuth();
  const notify = useToast();
  const [content, setContent] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  async function publish(event) {
    event.preventDefault();
    if (busy || !content.trim()) return;
    setBusy(true); setError(null);
    try {
      await api.request('/api/me/posts', { method: 'POST', body: { type: 'FREE', content: content.trim(), tags: [] } });
      setContent(''); onPublished(); notify('피드에 소식을 남겼어요.');
    } catch (error) { setError(error); } finally { setBusy(false); }
  }
  return <form className="panel composer" onSubmit={publish}>
    <div className="composer-top"><Avatar name={user.username} /><textarea aria-label="피드 내용" placeholder="오늘의 소식을 나눠보세요." maxLength={500} value={content} onChange={(event) => setContent(event.target.value)} disabled={busy} required /></div>
    <div className="composer-bottom"><span>{content.length} / 500</span><button className="button small-button" disabled={busy || !content.trim()}><Send size={15} />{busy ? '올리는 중' : '피드 올리기'}</button></div>
    {error && <p className="form-error" role="alert">{error.message}</p>}
    <p className="feed-quota">글은 3분에 한 번, 하루 최대 10개까지 올릴 수 있어요.</p>
  </form>;
}

function FeedComments({ post, onUpdate, id }) {
  const { user } = useAuth();
  const comments = useResource(`/api/feed/${encodeURIComponent(post.id)}/comments`);
  const [content, setContent] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  useEffect(() => {
    if (comments.data) onUpdate(post.id, { commentCount: comments.data.length });
  }, [comments.data, onUpdate, post.id]);
  async function submit(event) {
    event.preventDefault();
    if (busy || !content.trim()) return;
    setBusy(true); setError(null);
    try {
      await api.request(`/api/feed/${encodeURIComponent(post.id)}/comments`, { method: 'POST', body: { content: content.trim() } });
      setContent(''); onUpdate(post.id, { commentCount: comments.data.length + 1 }); comments.reload();
    } catch (error) { setError(error); } finally { setBusy(false); }
  }
  return <section id={id} className="feed-comments" aria-label="피드 댓글">
    <ResourceState resource={comments}>
      {comments.data?.length ? comments.data.map((comment) => <div className="comment-row" key={comment.id}>
        <Avatar name={comment.authorUsername || '이웃'} size="small" /><div><div className="comment-meta"><strong>{comment.authorUsername || '이웃'}</strong><small>{date(comment.createdAt)}</small></div><p className="post-content">{comment.content}</p>
          {comment.authorId === user.id && <div className="comment-actions"><ActionButton className="text-button danger" disabled={busy || comments.loading} action={async () => {
            if (!window.confirm('이 댓글을 삭제할까요?')) return;
            setBusy(true);
            try {
              await api.request(`/api/feed/${encodeURIComponent(post.id)}/comments/${encodeURIComponent(comment.id)}`, { method: 'DELETE' });
              onUpdate(post.id, { commentCount: Math.max(0, comments.data.length - 1) }); comments.reload();
            } finally { setBusy(false); }
          }}>댓글 삭제</ActionButton></div>}
        </div>
      </div>) : <p className="muted feed-comment-empty">첫 댓글을 남겨보세요.</p>}
      {comments.data && <form className="comment-form" onSubmit={submit}>
        <textarea aria-label="피드 댓글 내용" placeholder="따뜻한 댓글을 남겨주세요." rows={2} maxLength={2000} value={content} onChange={(event) => setContent(event.target.value)} disabled={busy} required />
        {error && <p className="form-error" role="alert">{error.message}</p>}
        <button className="button small-button" disabled={busy || comments.loading || !content.trim()}><Send size={15} />댓글 남기기</button>
      </form>}
    </ResourceState>
  </section>;
}

function FeedCard({ post, onUpdate }) {
  const { user } = useAuth();
  const [expanded, setExpanded] = useState(false);
  const commentsId = useId();
  const author = post.authorUsername || (post.authorId === user.id ? user.username : '이웃');
  return <article className="panel feed-card">
    <div className="feed-author"><Avatar name={author} /><div><strong>{author}</strong><small>{date(post.createdAt)}</small></div><span className="badge">{postTypes[post.type] || '소식'}</span></div>
    <p className="post-content">{post.content}</p>
    {post.tags?.length > 0 && <div className="feed-tags">{post.tags.map((tag, index) => <span className="badge soft" key={`${tag}-${index}`}>#{tag}</span>)}</div>}
    <div className="feed-actions">
      <ActionButton className={`button ghost ${post.liked ? 'liked' : ''}`} aria-label={`좋아요 ${post.likeCount}개`} aria-pressed={post.liked} action={() => api.request(`/api/posts/${encodeURIComponent(post.id)}/like`, { method: 'POST' })} onSuccess={(result) => onUpdate(post.id, { liked: result.liked, likeCount: result.likeCount })}><Heart size={17} fill={post.liked ? 'currentColor' : 'none'} />{post.likeCount}</ActionButton>
      <button className="button ghost" aria-expanded={expanded} aria-controls={commentsId} onClick={() => setExpanded((value) => !value)}><MessageCircle size={17} />댓글 {post.commentCount ?? 0}</button>
    </div>
    {expanded && <FeedComments post={post} onUpdate={onUpdate} id={commentsId} />}
  </article>;
}

export default function FeedTimeline({ preview = false }) {
  const feed = useFeed(preview ? 3 : 20);
  return <div className="feed-timeline">
    <FeedComposer onPublished={feed.reload} />
    {!preview && <div className="feed-toolbar"><span>최신 소식</span><button className="text-button" onClick={feed.reload} disabled={feed.loading || feed.loadingMore}><RefreshCw size={14} />새로고침</button></div>}
    {feed.error && <ErrorState error={feed.error} retry={feed.reload} />}
    {feed.loading && <Loading />}
    {feed.posts.length > 0 ? <div className="feed-list">{feed.posts.map((post) => <FeedCard key={post.id} post={post} onUpdate={feed.updatePost} />)}</div> : !feed.loading && !feed.error && <div className="panel"><Empty title="아직 올라온 소식이 없어요" description="내 소식과 친구들, 최근 대화를 나눈 사람들의 소식이 이곳에 모여요." /></div>}
    {!preview && !feed.loading && !feed.error && feed.hasMore && <div className="feed-more">
      {feed.moreError && <ErrorState error={feed.moreError} retry={feed.loadMore} />}
      {!feed.moreError && <button className="button secondary" onClick={feed.loadMore} disabled={feed.loadingMore}>{feed.loadingMore ? '불러오는 중' : '이전 소식 더 보기'}</button>}
    </div>}
  </div>;
}
