import { Check, KeyRound, LoaderCircle, Mail, Save, ShieldCheck, UserRound } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../../lib/api/client';
import { useAuth } from '../auth/AuthProvider';

export function ProfilePage() {
  const { user, refreshUser } = useAuth();
  const [name, setName] = useState(user?.name ?? '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [profileBusy, setProfileBusy] = useState(false);
  const [passwordBusy, setPasswordBusy] = useState(false);
  const [profileMessage, setProfileMessage] = useState('');
  const [passwordMessage, setPasswordMessage] = useState('');
  const [profileError, setProfileError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  useEffect(() => setName(user?.name ?? ''), [user?.name]);

  async function saveProfile(event: FormEvent) {
    event.preventDefault(); setProfileBusy(true); setProfileError(''); setProfileMessage('');
    try { await api.updateProfile(name.trim()); await refreshUser(); setProfileMessage('Profil məlumatları yeniləndi.'); }
    catch (err) { setProfileError(err instanceof Error ? err.message : 'Profili yeniləmək mümkün olmadı.'); }
    finally { setProfileBusy(false); }
  }

  async function savePassword(event: FormEvent) {
    event.preventDefault(); setPasswordBusy(true); setPasswordError(''); setPasswordMessage('');
    if (newPassword !== confirmPassword) { setPasswordError('Yeni şifrələr eyni deyil.'); setPasswordBusy(false); return; }
    try {
      await api.changePassword(currentPassword, newPassword);
      setCurrentPassword(''); setNewPassword(''); setConfirmPassword(''); setPasswordMessage('Şifrəniz təhlükəsiz şəkildə dəyişdirildi.');
    } catch (err) { setPasswordError(err instanceof Error ? err.message : 'Şifrəni dəyişmək mümkün olmadı.'); }
    finally { setPasswordBusy(false); }
  }

  return <div className="page-stack profile-page">
    <section className="page-heading"><div><span className="eyebrow">HESAB PARAMETRLƏRİ</span><h1>Profilim</h1><p>Şəxsi məlumatlarınızı və hesab təhlükəsizliyini idarə edin.</p></div><div className="heading-icon heading-green"><UserRound size={22} /></div></section>
    <div className="profile-grid">
      <section className="panel profile-card"><div className="panel-heading"><div><span className="eyebrow">ŞƏXSİ MƏLUMATLAR</span><h2>Profil məlumatları</h2></div><span className="panel-icon panel-icon-green"><UserRound size={18} /></span></div>
        <div className="profile-identity"><span className="profile-large-avatar">{user?.name?.slice(0, 1).toUpperCase() ?? 'F'}</span><div><strong>{user?.name}</strong><small>FinTrack istifadəçisi</small></div></div>
        {(profileError || profileMessage) && <div className={`alert ${profileError ? 'alert-error' : 'alert-success'}`} role={profileError ? 'alert' : 'status'}>{profileError || <><Check size={15} /> {profileMessage}</>}</div>}
        <form className="profile-form" onSubmit={saveProfile}>
          <label className="field-label" htmlFor="profile-name">Ad və soyad</label><input id="profile-name" className="input" autoComplete="name" maxLength={100} value={name} onChange={event => setName(event.target.value)} required />
          <label className="field-label" htmlFor="profile-email">Təsdiqlənmiş e-poçt</label><div className="profile-email"><Mail size={16} /><input id="profile-email" className="input" value={user?.email ?? ''} readOnly /><span className="verified-badge"><ShieldCheck size={14} /> Təsdiqlənib</span></div>
          <p className="profile-hint">E-poçt ünvanı hesabın giriş məlumatıdır. Ünvanın dəyişdirilməsi e-poçt təsdiq prosesi ilə ayrıca qorunur.</p>
          <button className="button button-primary" disabled={profileBusy || name.trim() === user?.name}>{profileBusy ? <><LoaderCircle className="spin" size={16} /> Saxlanır...</> : <><Save size={16} /> Profili yadda saxla</>}</button>
        </form>
      </section>
      <section className="panel profile-card"><div className="panel-heading"><div><span className="eyebrow">HESAB TƏHLÜKƏSİZLİYİ</span><h2>Şifrəni dəyiş</h2></div><span className="panel-icon panel-icon-violet"><KeyRound size={18} /></span></div>
        <p className="profile-security-copy">Hesabınızı qorumaq üçün əvvəlcə cari şifrənizi təsdiqləyin.</p>
        {(passwordError || passwordMessage) && <div className={`alert ${passwordError ? 'alert-error' : 'alert-success'}`} role={passwordError ? 'alert' : 'status'}>{passwordError || <><Check size={15} /> {passwordMessage}</>}</div>}
        <form className="profile-form" onSubmit={savePassword}>
          <label className="field-label" htmlFor="current-password">Cari şifrə</label><input id="current-password" className="input" type="password" autoComplete="current-password" value={currentPassword} onChange={event => setCurrentPassword(event.target.value)} required />
          <label className="field-label" htmlFor="new-password">Yeni şifrə</label><input id="new-password" className="input" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={newPassword} onChange={event => setNewPassword(event.target.value)} required /><small className="profile-hint">Ən azı 8 simvol istifadə edin.</small>
          <label className="field-label" htmlFor="confirm-password">Yeni şifrəni təkrarlayın</label><input id="confirm-password" className="input" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)} required />
          <button className="button button-primary" disabled={passwordBusy}>{passwordBusy ? <><LoaderCircle className="spin" size={16} /> Yenilənir...</> : <><KeyRound size={16} /> Şifrəni yenilə</>}</button>
        </form>
      </section>
    </div>
  </div>;
}
