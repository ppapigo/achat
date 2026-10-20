import { useState } from 'react';
import { api } from '../lib/api.js';
import { useToast } from '../contexts/ToastContext.jsx';
import { Field, SectionHeading } from './UI.jsx';

export function PasswordSettings() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [busy, setBusy] = useState(false);
  const notify = useToast();
  async function submit(event) {
    event.preventDefault();
    if (newPassword !== confirmation) { notify('새 비밀번호 확인이 일치하지 않습니다.', 'error'); return; }
    if (new TextEncoder().encode(newPassword).length > 72) { notify('새 비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.', 'error'); return; }
    setBusy(true);
    try {
      await api.request('/auth/password', { method: 'POST', body: { currentPassword, newPassword }, retry: false });
      api.setSession(null);
      notify('비밀번호를 변경했습니다. 새 비밀번호로 다시 로그인해주세요.');
    } catch (error) { notify(error.message, 'error'); }
    finally { setCurrentPassword(''); setNewPassword(''); setConfirmation(''); setBusy(false); }
  }
  return <section>
    <SectionHeading title="비밀번호 변경" />
    <form className="panel form-stack settings-form" onSubmit={submit}>
      <p className="muted">변경하면 모든 기기에서 로그아웃됩니다.</p>
      <Field label="현재 비밀번호"><input type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} maxLength={100} required disabled={busy} /></Field>
      <Field label="새 비밀번호" hint="8자 이상, UTF-8 기준 72바이트 이하"><input type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={8} maxLength={72} required disabled={busy} /></Field>
      <Field label="새 비밀번호 확인"><input type="password" autoComplete="new-password" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} minLength={8} maxLength={72} required disabled={busy} /></Field>
      <button className="button" disabled={busy}>{busy ? '변경 중…' : '비밀번호 변경'}</button>
    </form>
  </section>;
}
