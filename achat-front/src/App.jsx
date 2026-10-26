import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext.jsx';
import { Empty, ErrorState, Loading } from './components/UI.jsx';
import Shell from './components/Shell.jsx';
import AuthPage from './pages/AuthPage.jsx';
import HomePage from './pages/HomePage.jsx';
import FeedPage from './pages/FeedPage.jsx';
import RoomsPage from './pages/RoomsPage.jsx';
import ChatPage from './pages/ChatPage.jsx';
import BoardPage from './pages/BoardPage.jsx';
import BoardDetailPage from './pages/BoardDetailPage.jsx';
import FriendsPage from './pages/FriendsPage.jsx';
import PartyPage from './pages/PartyPage.jsx';
import MatchPage from './pages/MatchPage.jsx';
import CardsPage from './pages/CardsPage.jsx';
import ShopPage from './pages/ShopPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';
import SeasonsPage from './pages/SeasonsPage.jsx';

function Protected() {
  const { user, status, error, reloadUser, logout } = useAuth(); const location = useLocation();
  if (status === 'loading') return <Loading />;
  if (status === 'error') return <div className="auth-recovery"><ErrorState error={error} retry={reloadUser} /><button className="button secondary" onClick={logout}>다시 로그인</button></div>;
  return user ? <Outlet /> : <Navigate to="/login" state={{ from: location.pathname }} replace />;
}
export default function App() {
  return <Routes>
    <Route path="/login" element={<AuthPage />} /><Route path="/register" element={<AuthPage register />} />
    <Route element={<Protected />}><Route element={<Shell />}>
      <Route index element={<HomePage />} /><Route path="feed" element={<FeedPage />} /><Route path="rooms" element={<RoomsPage />} /><Route path="rooms/:roomId" element={<ChatPage />} />
      <Route path="boards" element={<BoardPage />} /><Route path="boards/:boardId" element={<BoardDetailPage />} />
      <Route path="friends" element={<FriendsPage />} /><Route path="party" element={<PartyPage />} /><Route path="match" element={<MatchPage />} />
      <Route path="cards" element={<CardsPage />} /><Route path="shop" element={<ShopPage />} /><Route path="profile" element={<ProfilePage />} />
      <Route path="seasons" element={<SeasonsPage />} />
      <Route path="*" element={<Empty title="페이지를 찾을 수 없어요" description="메뉴에서 다른 페이지를 선택해주세요." />} />
    </Route></Route>
  </Routes>;
}
