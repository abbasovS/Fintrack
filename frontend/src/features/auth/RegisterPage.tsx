import { ArrowLeft, ArrowRight, CheckCircle2, Eye, EyeOff, Mail } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { api } from '../../lib/api/client';
import { AuthFrame } from './LoginPage';

export function RegisterPage() {
  const navigate = useNavigate();
  const [name, setName] = useState(''); const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [code, setCode] = useState(''); const [step, setStep] = useState<'details' | 'verify'>('details');
  const [showPassword, setShowPassword] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true);
    try { await api.register(name.trim(), email.trim(), password); setStep('verify'); setNotice(`Təsdiq kodu ${email.trim()} ünvanına göndərildi.`); }
    catch (err) { setError(err instanceof Error ? err.message : 'Qeydiyyat mümkün olmadı. Yenidən yoxlayın.'); }
    finally { setBusy(false); }
  }

  async function verify(event: FormEvent) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true);
    try { await api.verifyEmail(email.trim(), code); navigate('/login', { replace: true, state: { verified: true } }); }
    catch (err) { setError(err instanceof Error ? err.message : 'Kodu təsdiqləmək mümkün olmadı.'); }
    finally { setBusy(false); }
  }

  async function resend() {
    setError(''); setNotice(''); setBusy(true);
    try { await api.resendVerification(email.trim()); setNotice('Yeni təsdiq kodu göndərildi.'); }
    catch (err) { setError(err instanceof Error ? err.message : 'Kodu göndərmək mümkün olmadı.'); }
    finally { setBusy(false); }
  }

  return <AuthFrame eyebrow={step === 'verify' ? 'E-POÇT TƏSDİQİ' : 'YENİ HESAB'} title={step === 'verify' ? 'E-poçtunuzu təsdiqləyin.' : 'Maliyyənizi aydın görün.'} subtitle={step === 'verify' ? 'Göndərdiyimiz 6 rəqəmli kodu daxil edin.' : 'Bir neçə addımda hesab yaradın.'}>
    {step === 'verify' ? <form className="auth-form" onSubmit={verify}>
      <div className="verification-note"><span><Mail size={18} /></span><div><strong>Kod göndərildi</strong><small>{email}</small></div></div>
      {notice && <div className="alert alert-success" role="status">{notice}</div>}{error && <div className="alert alert-error" role="alert">{error}</div>}
      <label className="field-label" htmlFor="verification-code">Təsdiq kodu</label><input id="verification-code" className="input verification-code" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} placeholder="000000" value={code} onChange={event => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))} required />
      <button className="button button-primary button-full" disabled={busy || code.length !== 6}>{busy ? <><span className="spinner spinner-light" /> Yoxlanılır...</> : <>E-poçtu təsdiqlə <CheckCircle2 size={17} /></>}</button>
      <button type="button" className="text-button verification-resend" onClick={resend} disabled={busy}>Kodu yenidən göndər</button>
      <button type="button" className="text-button verification-back" onClick={() => { setStep('details'); setCode(''); setError(''); setNotice(''); }}><ArrowLeft size={15} /> Məlumatları dəyiş</button>
    </form> : <form className="auth-form" onSubmit={submit}>
      {error && <div className="alert alert-error" role="alert">{error}</div>}
      <label className="field-label" htmlFor="name">Ad</label><input id="name" className="input" autoComplete="name" placeholder="Adınız" value={name} onChange={e => setName(e.target.value)} maxLength={100} required />
      <label className="field-label" htmlFor="email">E-poçt</label><input id="email" className="input" type="email" autoComplete="email" placeholder="ad@example.com" value={email} onChange={e => setEmail(e.target.value)} maxLength={150} required />
      <label className="field-label" htmlFor="password">Şifrə</label><div className="password-wrap"><input id="password" className="input" type={showPassword ? 'text' : 'password'} autoComplete="new-password" placeholder="Ən azı 8 simvol" value={password} onChange={e => setPassword(e.target.value)} minLength={8} maxLength={72} required /><button type="button" className="password-toggle" aria-label="Şifrəni göstər/gizlət" onClick={() => setShowPassword(!showPassword)}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></div>
      <button className="button button-primary button-full" disabled={busy}>{busy ? <><span className="spinner spinner-light" /> Kod göndərilir...</> : <>Təsdiq kodu al <ArrowRight size={17} /></>}</button>
      <p className="auth-switch">Artıq hesabınız var? <Link to="/login">Daxil olun</Link></p>
    </form>}
  </AuthFrame>;
}
