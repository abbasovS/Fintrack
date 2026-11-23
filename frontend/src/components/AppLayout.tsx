import { ArrowLeftRight, ArrowUpRight, LayoutDashboard, LogOut, Menu, RefreshCw, WalletCards, Repeat2, Target, TrendingUp, UserRound } from 'lucide-react';
import { NavLink, Outlet, useNavigate } from 'react-router';
import { useAuth } from '../features/auth/AuthProvider';
import { useState } from 'react';

const links = [
  { to: '/', label: 'İcmal', icon: LayoutDashboard, end: true },
  { to: '/transactions', label: 'Əməliyyatlar', icon: ArrowLeftRight },
  { to: '/budgets', label: 'Büdcələr', icon: Target },
  { to: '/categories', label: 'Kateqoriyalar', icon: WalletCards },
  { to: '/subscriptions', label: 'Abunəliklər', icon: RefreshCw },
  { to: '/recurring-expenses', label: 'Təkrarlanan xərclər', icon: Repeat2 },
  { to: '/recurring-income', label: 'Təkrarlanan gəlir', icon: TrendingUp },
];

export function AppLayout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const logout = () => { signOut(); navigate('/login', { replace: true }); };

  return <div className="app-shell">
    <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
      <a className="brand" href="/" aria-label="FinTrack ana səhifə"><span className="brand-mark"><WalletCards size={20} /></span><span>fin<span className="brand-light">track</span></span></a>
      <div className="workspace-label">ŞƏXSİ MALİYYƏ</div>
      <nav className="main-nav" aria-label="Əsas menyu">
        {links.map(({ to, label, icon: Icon, end }) => <NavLink key={to} to={to} end={end} onClick={() => setMenuOpen(false)} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><Icon size={18} strokeWidth={1.8} /><span>{label}</span>{to === '/transactions' && <span className="nav-dot" />}</NavLink>)}
      </nav>
      <div className="sidebar-bottom">
        <div className="side-note"><div className="side-note-icon"><ArrowUpRight size={17} /></div><div><strong>Kiçik addımlar</strong><p>Maliyyəni izləmək vərdişlə başlayır.</p></div></div>
        <button className="profile-button" onClick={() => { navigate('/profile'); setMenuOpen(false); }} title="Profil parametrləri">
          <span className="avatar">{user?.name?.slice(0, 1).toUpperCase() ?? 'F'}</span>
          <span className="profile-copy"><strong>{user?.name}</strong><small>{user?.email}</small></span>
          <UserRound size={17} className="logout-icon" />
        </button>
        <button className="sidebar-logout" onClick={logout}><LogOut size={15} /> Hesabdan çıxış</button>
      </div>
    </aside>
    <main className="main-area">
      <header className="topbar"><button className="icon-button mobile-menu" onClick={() => setMenuOpen(!menuOpen)} aria-label="Menyunu aç"><Menu size={20} /></button><div className="breadcrumb">FinTrack <span>/</span> <strong>Şəxsi maliyyə</strong></div><div className="topbar-right"><span className="status-pill"><span /> Hesab aktivdir</span><span className="top-avatar">{user?.name?.slice(0, 1).toUpperCase()}</span></div></header>
      <div className="page-content"><Outlet /></div>
    </main>
    {menuOpen && <button className="scrim" onClick={() => setMenuOpen(false)} aria-label="Menyunu bağla" />}
  </div>;
}
