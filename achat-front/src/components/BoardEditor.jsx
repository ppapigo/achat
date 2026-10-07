import { useState } from 'react';
import { api } from '../lib/api.js';
import { Field, Modal } from './UI.jsx';
import { useToast } from '../contexts/ToastContext.jsx';

export const categories = [['FREE', '자유'], ['QUESTION', '질문'], ['GUIDE', '공략'], ['TRADE', '거래'], ['NOTICE', '공지']];
export default function BoardEditor({ initial, onClose, onSaved }) {
  const notify = useToast(); const [busy, setBusy] = useState(false);
  async function submit(event) { event.preventDefault(); const form = new FormData(event.currentTarget); setBusy(true); try { const result = await api.request(initial ? `/api/boards/${initial.id}` : '/api/boards', { method: initial ? 'PUT' : 'POST', body: { category: form.get('category'), title: form.get('title').trim(), content: form.get('content').trim() } }); notify(initial ? '이야기를 수정했어요.' : '새 이야기를 올렸어요.'); onSaved(result); } catch (error) { notify(error.message, 'error'); } finally { setBusy(false); } }
  return <Modal title={initial ? '이야기 수정하기' : '새 이야기 쓰기'} onClose={() => !busy && onClose()}><form className="form-stack" onSubmit={submit}><Field label="카테고리"><select name="category" defaultValue={initial?.category || 'FREE'}>{categories.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></Field><Field label="제목"><input name="title" defaultValue={initial?.title || ''} placeholder="어떤 이야기를 나누고 싶나요?" required minLength={2} maxLength={100} autoFocus /></Field><Field label="내용" hint="10자 이상, 10,000자 이하로 작성해주세요."><textarea name="content" rows={9} defaultValue={initial?.content || ''} placeholder="당신의 이야기를 자유롭게 들려주세요." required minLength={10} maxLength={10000} /></Field><button className="button wide" disabled={busy}>{busy ? '저장 중…' : initial ? '수정 내용 저장' : '이야기 올리기'}</button></form></Modal>;
}
