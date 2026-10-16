import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, Link } from 'react-router-dom';
import { Home, MessageSquare, Users, Layers, Shuffle, CreditCard, ShoppingBag, UserRound, LogOut, Menu, X, Coins, ArrowUpRight, BookOpen, Trophy } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext.jsx';
import { Avatar, Brand } from './UI.jsx';
import { number } from '../lib/format.js';
import { UI_PREVIEW } from '../preview/mode.js';

const navigation = [
  ['/', '홈', Home], ['/rooms', '채팅방', MessageSquare], ['/boards', '커뮤니티', BookOpen], ['/friends', '친구', Users],
  ['/party', '내 파티', Layers], ['/match', '랜덤 매칭', Shuffle], ['/seasons', '시즌', Trophy], ['/cards', '프로필 카드', CreditCard], ['/shop', '상점', ShoppingBag], ['/profile', '내 프로필', UserRound],
];
export default function Shell() {
  const { user, logout } = useAuth(); const location = useLocation(); const [open, setOpen] = useState(false);
  const title = navigation.find(([path]) => path === '/' ? location.pathname === '/' : location.pathname.startsWith(path))?.[1] || 'A.chat';
  useEffect(() => { document.title = `${title} · A.chat`; }, [title]);
  return <div className="app-shell">
    {open && <button className="sidebar-overlay" aria-label="메뉴 닫기" onClick={() => setOpen(false)} />}
    <aside className={`sidebar ${open ? 'open' : ''}`}>
      <Link to="/" onClick={() => setOpen(false)} className="brand-link"><Brand /></Link><button className="icon-button mobile-close" aria-label="메뉴 닫기" onClick={() => setOpen(false)}><X /></button>
      <p className="nav-label">YOUR LITTLE COMMUNITY</p>
      <nav aria-label="주 메뉴">{navigation.map(([path, label, Icon], index) => <NavLink key={path} to={path} end={path === '/'} onClick={() => setOpen(false)} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''} ${index === 4 ? 'nav-separator' : ''}`}><Icon size={20} /><span>{label}</span></NavLink>)}</nav>
      <div className="sidebar-bottom"><Link to="/match" onClick={() => setOpen(false)} className="sidebar-invite"><span>새로운 대화가 기다려요</span><strong>랜덤 매칭 시작하기 <ArrowUpRight size={17} /></strong></Link><button className="nav-item logout" onClick={logout}><LogOut size={19} />로그아웃</button><small>작은 대화, 더 넓은 세상.</small></div>
    </aside>
    <div className="workspace"><header className="topbar"><div className="topbar-title"><button className="icon-button mobile-menu" onClick={() => setOpen(true)} aria-label="메뉴 열기"><Menu /></button><span>A.chat <span className="breadcrumb-separator">/</span> <strong>{title}</strong></span></div><div className="topbar-user"><span className="coin-pill"><Coins size={17} />{number(user.coins)}<small>코인</small></span><Link to="/profile" className="user-link"><Avatar name={user.username} size="small" /><span>{user.username}</span></Link></div></header>{UI_PREVIEW && <div className="preview-label" role="status">화면 미리보기 · 샘플 데이터</div>}<main id="main-content" className="main-content"><Outlet /></main><footer className="footer">A.chat <span>대화가 되는 곳</span><span>Make a little connection.</span></footer></div>
  </div>;
}
