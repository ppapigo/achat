import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { CheckCircle2, CircleAlert, X } from 'lucide-react';

const ToastContext = createContext(null);
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Set());
  const notify = useCallback((message, type = 'success') => {
    const id = crypto.randomUUID();
    setToasts((previous) => [...previous.slice(-2), { id, message, type }]);
    const timer = setTimeout(() => { setToasts((previous) => previous.filter((toast) => toast.id !== id)); timers.current.delete(timer); }, 5000);
    timers.current.add(timer);
  }, []);
  useEffect(() => { const current = timers.current; return () => current.forEach(clearTimeout); }, []);
  return <ToastContext.Provider value={notify}>{children}<div className="toast-stack" aria-live="polite">{toasts.map((toast) => <div key={toast.id} className={`toast ${toast.type}`}>{toast.type === 'error' ? <CircleAlert size={20} /> : <CheckCircle2 size={20} />}<span>{toast.message}</span><button className="icon-button" aria-label="알림 닫기" onClick={() => setToasts((previous) => previous.filter((item) => item.id !== toast.id))}><X size={16} /></button></div>)}</div></ToastContext.Provider>;
}
export const useToast = () => useContext(ToastContext);
