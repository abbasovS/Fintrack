import { ArrowDownLeft, ArrowRight, ArrowUpRight, Check, LoaderCircle, Pencil, Plus, Trash2, WalletCards } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, api } from '../../lib/api/client';
import type { Category, CategoryType } from '../../lib/api/types';

export function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState('');
  const [type, setType] = useState<CategoryType>('EXPENSE');
  const [editing, setEditing] = useState<Category | null>(null);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const load = () => api.categories().then(setCategories).catch(err => {
    setError(err instanceof Error ? err.message : 'Kateqoriyaları yükləmək mümkün olmadı.');
  }).finally(() => setLoading(false));

  useEffect(() => { void load(); }, []);

  function startEdit(category: Category) {
    setEditing(category);
    setName(category.name);
    setType(category.type);
    setError('');
    setNotice('');
  }

  function cancelEdit() {
    setEditing(null);
    setName('');
    setType('EXPENSE');
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setNotice('');
    setBusy(true);
    try {
      if (editing) {
        const updated = await api.updateCategory(editing.id, name.trim(), type);
        setCategories(items => items.map(item => item.id === updated.id ? updated : item));
        setNotice('Kateqoriya yeniləndi.');
        cancelEdit();
      } else {
        const created = await api.createCategory(name.trim(), type);
        setCategories(items => [...items, created].sort((a, b) => a.name.localeCompare(b.name, 'az')));
        setNotice('Kateqoriya yaradıldı.');
        setName('');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sorğunu icra etmək mümkün olmadı.');
    } finally { setBusy(false); }
  }

  async function remove(category: Category) {
    if (!window.confirm('“' + category.name + '” kateqoriyasını silmək istəyirsiniz?')) return;
    setError('');
    setNotice('');
    setBusy(true);
    try {
      await api.deleteCategory(category.id);
      setCategories(items => items.filter(item => item.id !== category.id));
      setNotice('Kateqoriya silindi.');
      if (editing?.id === category.id) cancelEdit();
    } catch (err) {
      const message = err instanceof ApiError && err.status === 409
        ? 'Kateqoriya əməliyyat, büdcə və ya təkrarlanan qaydada istifadə olunur. Əvvəl həmin əlaqələri silin və ya dəyişin.'
        : err instanceof Error ? err.message : 'Kateqoriyanı silmək mümkün olmadı.';
      setError(message);
    } finally { setBusy(false); }
  }

  return <div className="page-stack">
    <section className="page-heading"><div><span className="eyebrow">GƏLİR VƏ XƏRC QURUPLARI</span><h1>Kateqoriyalar</h1><p>Kateqoriyalar real hesab məlumatlarınızdan yüklənir.</p></div><div className="heading-icon heading-purple"><WalletCards size={22} /></div></section>
    {(error || notice) && <div className={'alert ' + (error ? 'alert-error' : 'alert-success')} role="status">{error || <><Check size={16} /> {notice}</>}</div>}
    <div className="feature-grid">
      <section className="panel form-panel">
        <div className="panel-heading"><div><span className="eyebrow">{editing ? 'DƏYİŞİKLİK' : 'YENİ QRUP'}</span><h2>{editing ? 'Kateqoriyanı dəyiş' : 'Kateqoriya yarat'}</h2></div><span className="panel-icon panel-icon-violet">{editing ? <Pencil size={18} /> : <Plus size={18} />}</span></div>
        <p className="panel-copy">Əməliyyat əlavə etmək üçün uyğun gəlir və ya xərc kateqoriyası yaradın.</p>
        <form className="stack-form" onSubmit={submit}>
          <label className="field-label" htmlFor="category-name">Kateqoriya adı</label>
          <input className="input" id="category-name" value={name} onChange={e => setName(e.target.value)} placeholder="Məs. Ərzaq, Maaş" maxLength={100} required />
          <label className="field-label" htmlFor="category-type">Növ</label>
          <select className="input select" id="category-type" value={type} onChange={e => setType(e.target.value as CategoryType)}><option value="EXPENSE">Xərc</option><option value="INCOME">Gəlir</option></select>
          <div className="form-actions"><button className="button button-primary" disabled={busy}>{busy ? <><LoaderCircle className="spin" size={17} /> Yadda saxlanır...</> : <>{editing ? 'Dəyişikliyi yadda saxla' : 'Kateqoriya yarat'} <ArrowRight size={16} /></>}</button>{editing && <button className="button button-secondary" type="button" onClick={cancelEdit}>Ləğv et</button>}</div>
        </form>
      </section>
      <section className="panel lookup-panel">
        <div className="panel-heading"><div><span className="eyebrow">REAL MƏLUMATLAR</span><h2>Kateqoriyalarınız</h2></div><span className="panel-icon panel-icon-green"><WalletCards size={18} /></span></div>
        {loading ? <div className="data-loading"><LoaderCircle className="spin" size={18} /> Yüklənir...</div> : categories.length ? <div className="managed-list">{categories.map(category => <div className="managed-row" key={category.id}><span className={'transaction-type-icon ' + (category.type === 'INCOME' ? 'income' : 'expense')}>{category.type === 'INCOME' ? <ArrowDownLeft size={17} /> : <ArrowUpRight size={17} />}</span><span className="managed-copy"><strong>{category.name}</strong><small>{category.type === 'INCOME' ? 'Gəlir' : 'Xərc'} · #{category.id}</small></span><button className="icon-button" onClick={() => startEdit(category)} disabled={busy} aria-label={category.name + ' kateqoriyasını dəyiş'}><Pencil size={16} /></button><button className="icon-button danger-icon" onClick={() => remove(category)} disabled={busy} aria-label={category.name + ' kateqoriyasını sil'}><Trash2 size={17} /></button></div>)}</div> : <div className="empty-state"><div className="empty-icon"><WalletCards size={21} /></div><strong>Kateqoriya yoxdur</strong><span>İlk kateqoriyanızı yaradın.</span></div>}
        <p className="inline-hint"><span className="hint-mark">i</span><span>Kateqoriyalar yalnız sizin hesabınızda görünür və əməliyyatlarınızla birlikdə saxlanılır.</span></p>
      </section>
    </div>
  </div>;
}
