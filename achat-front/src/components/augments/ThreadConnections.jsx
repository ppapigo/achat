import { useLayoutEffect, useState } from 'react';
import { messageEffects } from './effects.js';

export default function ThreadConnections({ messages, containerRef }) {
  const [paths, setPaths] = useState([]);
  useLayoutEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const measure = () => {
      const base = container.getBoundingClientRect();
      setPaths(messages.flatMap((message) => {
        if (!message.replyToId || !messageEffects(message).some((effect) => ['THREAD_WEAVER', 'LINK_BRIDGE'].includes(effect.type))) return [];
        const parent = document.getElementById(`message-${message.replyToId}`);
        const child = document.getElementById(`message-${message.id}`);
        if (!parent || !child || !container.contains(parent)) return [];
        const from = parent.getBoundingClientRect(); const to = child.getBoundingClientRect();
        const y1 = from.top - base.top + 15; const y2 = to.top - base.top + 15;
        return [{ id: message.id, d: `M 28 ${y1} C 1 ${y1}, 1 ${y2}, 28 ${y2}` }];
      }));
    };
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(container);
    return () => observer.disconnect();
  }, [messages, containerRef]);
  return <svg className="thread-connections" aria-hidden="true">{paths.map((path) => <path key={path.id} d={path.d} />)}</svg>;
}
