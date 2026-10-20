export function mergeMessages(current, incoming) {
  const messages = new Map(current.map((message) => [message.id, message]));
  incoming.forEach((message) => {
    const previous = messages.get(message.id);
    const versioned = message.revision != null && previous?.revision != null;
    const newer = versioned && (message.revision > previous.revision
      || message.revision === previous.revision && Date.parse(message.serverTime) > Date.parse(previous.serverTime));
    messages.set(message.id, newer ? { ...previous, ...message } : { ...message, ...previous });
  });
  return [...messages.values()].sort((left, right) => new Date(left.createdAt) - new Date(right.createdAt));
}
export function websocketUrl(apiBase, origin) {
  const url = new URL(`${apiBase.replace(/\/$/, '')}/ws`, origin);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  return url.toString();
}
export function mergeLike(message, like) {
  if (message.id !== like.messageId || like.revision != null && message.revision != null && like.revision < message.revision) return message;
  const users = new Set(message.likedByUserIds || []);
  const userId = like.userId || like.odUserId;
  if (userId) { if (like.liked) users.add(userId); else users.delete(userId); }
  return { ...message, likeCount: like.likeCount, likedByUserIds: [...users], revision: like.revision ?? message.revision };
}
export function mergeRound(current, incoming) {
  if (current && Date.parse(incoming.serverTime) < Date.parse(current.serverTime)) return current;
  return incoming;
}
