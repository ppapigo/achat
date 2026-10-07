import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, Sparkles, MessageCircle, Users, ShieldCheck, LoaderCircle } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext.jsx';
import { useToast } from '../contexts/ToastContext.jsx';
import { Brand, Field } from '../components/UI.jsx';
import { api } from '../lib/api.js';
import { UI_PREVIEW } from '../preview/mode.js';

export default function AuthPage({ register = false }) {
  const { user, login } = useAuth(); const notify = useToast(); const navigate = useNavigate(); const location = useLocation();
  const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [code, setCode] = useState('');
  const [verification, setVerification] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  if (user) return <Navigate to="/" replace />;
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      if (verification) { await api.request('/auth/verify', { method: 'POST', body: { email, code }, auth: false }); notify('인증이 완료됐어요. 로그인해주세요.'); setVerification(false); navigate('/login'); }
      else if (register) { await api.request('/auth/register', { method: 'POST', body: { email, password }, auth: false }); setVerification(true); }
      else { await login(email, password); const from = location.state?.from; navigate(from?.startsWith('/') && !from.startsWith('//') ? from : '/', { replace: true }); }
    } catch (error) { setError(error.message); } finally { setBusy(false); }
  }
  return <div className="auth-layout"><section className="auth-story"><Link to="/login"><Brand /></Link><div className="story-copy"><span className="story-label"><span className="status-dot" /> A LITTLE CONNECTION</span><h1>대화가 시작되면,<br />새로운 내가 보여요<span>.</span></h1><p>나와 닮은 사람, 나와 다른 이야기.<br />부담 없이 만나고, 함께 즐기는 우리만의 공간.</p><div className="conversation-art" aria-hidden="true"><span className="art-spark"><Sparkles size={34} /></span><div className="art-bubble first"><span className="art-avatar">A</span><span>오늘, 어떤 이야기를 나눌까요?<i>조금 다른 하루의 시작</i></span></div><div className="art-bubble second">좋아요. 같이 이야기해요! <span>☘</span></div><span className="art-caption">YOU + A LITTLE CONVERSATION</span></div></div><div className="story-features"><span><MessageCircle size={18} /> 실시간 대화</span><span><Sparkles size={18} /> 나만의 증강</span><span><Users size={18} /> 새로운 친구</span></div></section>
    <section className="auth-form-panel"><div className="auth-form-wrap"><p className="eyebrow">WELCOME TO A.CHAT</p><h2>{verification ? '거의 다 왔어요!' : register ? '첫 대화를 시작해볼까요?' : '다시 만나서 반가워요'}</h2><p className="muted">{verification ? '계정을 활성화할 인증 코드를 입력해주세요.' : register ? '이메일로 가입하고 당신의 이야기를 들려주세요.' : '오늘도 당신만의 이야기가 기다리고 있어요.'}</p>
    <form onSubmit={submit} className="form-stack"><Field label="이메일"><input type="email" name="email" autoComplete="email" placeholder="hello@example.com" value={email} onChange={(event) => setEmail(event.target.value)} required disabled={busy} maxLength={254} /></Field>{verification ? <Field label="인증 코드"><input inputMode="numeric" autoComplete="one-time-code" value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="6자리 코드" pattern="[0-9]{6}" required /></Field> : <Field label="비밀번호" hint={register ? '8자 이상 입력해주세요.' : undefined}><input type="password" name="password" autoComplete={register ? 'new-password' : 'current-password'} placeholder="비밀번호를 입력해주세요" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} maxLength={100} /></Field>}
      {error && <p className="form-error" role="alert">{error}</p>}<button className="button wide" disabled={busy}>{busy ? <LoaderCircle size={18} className="spin" /> : null}{verification ? '인증 완료하기' : register ? '함께 시작하기' : '로그인'}<ArrowRight size={18} /></button>
    </form>{verification ? <button className="button ghost wide" disabled={busy} onClick={async () => { setBusy(true); try { await api.request(`/auth/resend-code?email=${encodeURIComponent(email)}`, { method: 'POST', auth: false }); notify('인증 코드를 다시 요청했어요.'); } catch (error) { setError(error.message); } finally { setBusy(false); } }}>인증 코드 다시 요청</button> : <p className="auth-switch">{register ? '이미 계정이 있나요?' : '아직 A.chat이 처음인가요?'} <Link to={register ? '/login' : '/register'} onClick={() => setError('')}>{register ? '로그인' : '회원가입'}</Link><span> · </span><button className="text-button" onClick={() => setVerification(true)}>이메일 인증</button></p>}
    {UI_PREVIEW && <div className="preview-entry"><p>화면 미리보기 · 샘플 데이터</p><button className="button secondary wide" disabled={busy} onClick={async () => { setBusy(true); try { await login('preview@example.test', 'ui-preview-only'); navigate('/'); } catch (error) { setError(error.message); } finally { setBusy(false); } }}>로그인 없이 화면 둘러보기<ArrowRight size={17} /></button></div>}
    <div className="auth-footnote"><ShieldCheck size={17} /> 서로를 존중하는 작은 대화가 큰 연결이 됩니다.</div></div></section></div>;
}
