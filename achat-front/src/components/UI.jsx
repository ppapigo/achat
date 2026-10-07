import { useEffect, useId, useRef, useState } from 'react';
import { CircleAlert, LoaderCircle, MessageCircle, X, Inbox, ArrowRight } from 'lucide-react';
import { useToast } from '../contexts/ToastContext.jsx';

export function Brand() { return <span className="brand"><span className="brand-mark"><MessageCircle size={23} fill="currentColor" /></span><span>A.chat<span className="brand-dot">.</span></span></span>; }
export function Avatar({ name = '?', size = '' }) { const color = [...name].reduce((total, char) => total + char.charCodeAt(0), 0) % 5; return <span className={`avatar avatar-${color} ${size}`} aria-hidden="true">{name.slice(0, 1).toUpperCase()}</span>; }
export function Empty({ title, description, children }) { return <div className="empty"><span className="empty-icon"><Inbox size={28} /></span><h3>{title}</h3>{description && <p>{description}</p>}{children}</div>; }
export function Loading() { return <div className="loading" role="status"><LoaderCircle className="spin" size={24} /><span>불러오는 중이에요</span></div>; }
export function ErrorState({ error, retry }) { return <div className="error-state" role="alert"><CircleAlert size={22} /><p>{error?.message || '잠시 후 다시 시도해주세요.'}</p>{retry && <button className="button secondary" onClick={retry}>다시 시도</button>}</div>; }
export function ResourceState({ resource, children }) { if (resource.loading && !resource.data) return <Loading />; if (resource.error) return <ErrorState error={resource.error} retry={resource.reload} />; return children; }
export function PageHeading({ eyebrow, title, description, children }) { return <div className="page-heading"><div>{eyebrow && <p className="eyebrow">{eyebrow}</p>}<h1>{title}</h1>{description && <p className="muted">{description}</p>}</div>{children && <div className="heading-actions">{children}</div>}</div>; }
export function Field({ label, children, hint }) { return <label className="field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>; }
export function Modal({ title, children, onClose }) {
  const id = useId(); const dialog = useRef(null);
  useEffect(() => { const element = dialog.current; element.showModal(); return () => element.close(); }, []);
  return <dialog ref={dialog} className="modal" aria-labelledby={id} onCancel={(event) => { event.preventDefault(); onClose(); }} onClick={(event) => { if (event.target === dialog.current) onClose(); }}><div className="modal-header"><h2 id={id}>{title}</h2><button className="icon-button" aria-label="닫기" onClick={onClose}><X size={20} /></button></div>{children}</dialog>;
}
export function ActionButton({ action, children, className = 'button', disabled, onSuccess, ...props }) {
  const [busy, setBusy] = useState(false); const notify = useToast();
  return <button {...props} className={className} disabled={busy || disabled} onClick={async () => { setBusy(true); try { const result = await action(); onSuccess?.(result); } catch (error) { notify(error.message, 'error'); } finally { setBusy(false); } }}>{busy ? <LoaderCircle size={16} className="spin" /> : null}{children}</button>;
}
export function SectionHeading({ title, children }) { return <div className="section-heading"><h2>{title}</h2>{children}</div>; }
export function TextLink({ children }) { return <span className="text-link">{children}<ArrowRight size={16} /></span>; }
