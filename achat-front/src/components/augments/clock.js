import { useSyncExternalStore } from 'react';

let offset = 0;
let latestServerTime = 0;
let snapshot = Date.now();
let timer;
const listeners = new Set();
function tick() { snapshot = Date.now() + offset; listeners.forEach((listener) => listener()); }
export function syncServerTime(value) {
  const time = Date.parse(value);
  if (!Number.isFinite(time) || time < latestServerTime) return;
  latestServerTime = time; offset = time - Date.now(); tick();
}
function subscribe(listener) {
  listeners.add(listener);
  if (!timer) timer = setInterval(tick, 500);
  tick();
  return () => { listeners.delete(listener); if (!listeners.size) { clearInterval(timer); timer = undefined; } };
}
const idleSubscribe = () => () => {};
export function useEffectClock(deadline) {
  const active = Date.parse(deadline) > snapshot;
  return useSyncExternalStore(active ? subscribe : idleSubscribe, () => snapshot, () => snapshot);
}
