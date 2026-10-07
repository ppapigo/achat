export function mergeMessages(current, incoming) {
  const messages = new Map(current.map((message) => [message.id, message]));
  incoming.forEach((message) => messages.set(message.id, { ...message, ...messages.get(message.id) }));
  return [...messages.values()].sort((left, right) => new Date(left.createdAt) - new Date(right.createdAt));
}
export function websocketUrl(apiBase, origin) {
  const url = new URL(`${apiBase.replace(/\/$/, '')}/ws`, origin);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  return url.toString();
}
