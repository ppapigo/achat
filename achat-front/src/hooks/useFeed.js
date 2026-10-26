import { useCallback, useEffect, useRef, useState } from 'react';
import { api, query } from '../lib/api.js';

export function useFeed(limit = 20) {
  const [state, setState] = useState({ posts: [], nextCursor: null, hasMore: false, loading: true, loadingMore: false, error: null, moreError: null });
  const request = useRef(null);
  const fetchPage = useCallback(async (cursor = null) => {
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    const append = Boolean(cursor);
    setState((previous) => ({ ...previous, loading: !append, loadingMore: append, error: null, moreError: null }));
    try {
      const data = await api.request(`/api/feed${query({ limit, cursor })}`, { signal: controller.signal });
      if (controller.signal.aborted) return;
      setState((previous) => {
        const posts = append ? [...new Map([...previous.posts, ...data.posts].map((post) => [post.id, post])).values()] : data.posts;
        return { posts, nextCursor: data.nextCursor, hasMore: data.hasMore, loading: false, loadingMore: false, error: null, moreError: null };
      });
    } catch (error) {
      if (!controller.signal.aborted) setState((previous) => ({ ...previous, loading: false, loadingMore: false, [append ? 'moreError' : 'error']: error }));
    }
  }, [limit]);
  useEffect(() => { fetchPage(); return () => request.current?.abort(); }, [fetchPage]);
  const updatePost = useCallback((id, changes) => {
    setState((previous) => ({ ...previous, posts: previous.posts.map((post) => post.id === id ? { ...post, ...changes } : post) }));
  }, []);
  return { ...state, reload: () => fetchPage(), loadMore: () => fetchPage(state.nextCursor), updatePost };
}
