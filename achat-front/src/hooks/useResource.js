import { useCallback, useEffect, useState } from 'react';
import { api } from '../lib/api.js';

export function useResource(path) {
  const [state, setState] = useState({ path, data: null, loading: Boolean(path), error: null });
  const [revision, setRevision] = useState(0);
  const reload = useCallback(() => setRevision((value) => value + 1), []);
  useEffect(() => {
    const controller = new AbortController();
    if (!path) { setState({ path, data: null, loading: false, error: null }); return () => controller.abort(); }
    setState((previous) => ({ path, data: previous.path === path ? previous.data : null, loading: true, error: null }));
    api.request(path, { signal: controller.signal }).then((data) => {
      if (!controller.signal.aborted) setState({ path, data, loading: false, error: null });
    }).catch((error) => { if (!controller.signal.aborted) setState({ path, data: null, loading: false, error }); });
    return () => controller.abort();
  }, [path, revision]);
  return { ...(state.path === path ? state : { data: null, loading: Boolean(path), error: null }), reload };
}
