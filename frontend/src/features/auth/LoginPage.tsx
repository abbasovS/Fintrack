import { ArrowRight, Eye, EyeOff, WalletCards } from 'lucide-react';
import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { useAuth } from './AuthProvider';

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const registered = (location.state as { verified?: boolean } | null)?.verified === true;
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setBusy(true);
    try {
      await signIn(email.trim(), password);
      const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/';
      navigate(from, { replace: true });
    } catch (err) { setError(err instanceof Error ? err.message : 'Giriş mümkün olmadı. Yenidən yoxlayın.'); }
    finally { setBusy(false); }
  }

  return <AuthFrame eyebrow="YENİDƏN XOŞ GƏLDİNİZ" title="Maliyyəniz nəzarətinizdə." subtitle="Davam etmək üçün hesabınıza daxil olun.">
    <form className="auth-form" onSubmit={submit}>
      {registered && <div className="alert alert-success" role="status">E-poçt təsdiqləndi. İndi hesabınıza daxil ola bilərsiniz.</div>}
      {error && <div className="alert alert-error" role="alert">{error}</div>}
      <label className="field-label" htmlFor="email">E-poçt</label><input id="email" className="input" type="email" autoComplete="email" placeholder="ad@example.com" value={email} onChange={e => setEmail(e.target.value)} required />
      <div className="field-row"><label className="field-label" htmlFor="password">Şifrə</label></div>
      <div className="password-wrap"><input id="password" className="input" type={showPassword ? 'text' : 'password'} autoComplete="current-password" placeholder="Şifrənizi daxil edin" value={password} onChange={e => setPassword(e.target.value)} required /><button type="button" className="password-toggle" aria-label={showPassword ? 'Şifrəni gizlət' : 'Şifrəni göstər'} onClick={() => setShowPassword(!showPassword)}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></div>
      <button className="button button-primary button-full" disabled={busy}>{busy ? <><span className="spinner spinner-light" /> Daxil olunur...</> : <>Daxil ol <ArrowRight size={17} /></>}</button>
      <p className="auth-switch">Hesabınız yoxdur? <Link to="/register">Qeydiyyatdan keçin</Link></p>
    </form>
  </AuthFrame>;
}

export function AuthFrame({ eyebrow, title, subtitle, children }: { eyebrow: string; title: string; subtitle: string; children: ReactNode }) {
  return <div className="auth-page"><div className="auth-brand"><span className="brand-mark"><WalletCards size={20} /></span><span>fin<span className="brand-light">track</span></span></div><div className="auth-card"><div className="auth-heading"><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{subtitle}</p></div>{children}</div><p className="auth-footnote">Sadə maliyyə izləmə · Şəxsi və təhlükəsiz</p></div>;
}
