import { AlertTriangle, ArrowDown, ArrowLeft, Check, ChevronLeft, ChevronRight, CircleHelp, LoaderCircle, Pencil, Plus, Target, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { api } from '../../lib/api/client';
import type { Category, MonthlyBudget } from '../../lib/api/types';

function monthKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function money(amount: number, currency = 'AZN') {
  return new Intl.NumberFormat('az-AZ', { style: 'currency', currency, maximumFractionDigits: 2 }).format(amount);
}

export function BudgetsPage() {
  const [selectedMonth, setSelectedMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [budgets, setBudgets] = useState<MonthlyBudget[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState('');
  const [amount, setAmount] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editAmount, setEditAmount] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const month = monthKey(selectedMonth);
  const monthLabel = new Intl.DateTimeFormat('az-AZ', { month: 'long', year: 'numeric' }).format(selectedMonth);
  const expenseCategories = useMemo(() => categories.filter(item => item.type === 'EXPENSE'), [categories]);
  const availableCategories = useMemo(() => expenseCategories.filter(category => !budgets.some(budget => budget.categoryId === category.id)), [expenseCategories, budgets]);
  const monthTotal = budgets.reduce((sum, item) => sum + item.amount, 0);
  const spentTotal = budgets.reduce((sum, item) => sum + item.actualAmount, 0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([api.budgets(month), api.categories()]).then(([budgetList, categoryList]) => {
      if (!active) return;
      setBudgets(budgetList);
      setCategories(categoryList);
      setCategoryId(current => budgetList.some(item => String(item.categoryId) === current) ? '' : current || String(categoryList.find(item => item.type === 'EXPENSE' && !budgetList.some(b => b.categoryId === item.id))?.id ?? ''));
    }).catch(err => { if (active) setError(err instanceof Error ? err.message : 'Büdcə məlumatlarını yükləmək mümkün olmadı.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [month]);

  async function createBudget(event: FormEvent) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true);
    try {
      const created = await api.createBudget({ categoryId: Number(categoryId), month, amount: Number(amount) });
      setBudgets(items => [...items, created].sort((a, b) => a.categoryName.localeCompare(b.categoryName, 'az')));
      setCategoryId(''); setAmount(''); setNotice('Büdcə əlavə edildi.');
    } catch (err) { setError(err instanceof Error ? err.message : 'Büdcəni yaratmaq mümkün olmadı.'); }
    finally { setBusy(false); }
  }

  async function saveBudget(item: MonthlyBudget) {
    setBusy(true); setError(''); setNotice('');
    try {
      const updated = await api.updateBudget(item.id, Number(editAmount));
      setBudgets(items => items.map(value => value.id === item.id ? updated : value));
      setEditingId(null); setNotice('Büdcə yeniləndi.');
    } catch (err) { setError(err instanceof Error ? err.message : 'Büdcəni yeniləmək mümkün olmadı.'); }
    finally { setBusy(false); }
  }

  async function removeBudget(item: MonthlyBudget) {
    if (!window.confirm(`“${item.categoryName}” üçün ${monthLabel} büdcəsini silmək istəyirsiniz?`)) return;
    setBusy(true); setError(''); setNotice('');
    try {
      await api.deleteBudget(item.id);
      setBudgets(items => items.filter(value => value.id !== item.id));
      setNotice('Büdcə silindi.');
    } catch (err) { setError(err instanceof Error ? err.message : 'Büdcəni silmək mümkün olmadı.'); }
    finally { setBusy(false); }
  }

  return <div className="page-stack budgets-page">
    <section className="page-heading"><div><span className="eyebrow">XƏRC NƏZARƏTİ</span><h1>Aylıq büdcələr</h1><p>Hər kateqoriyaya limit qoyun, xərcləməni ay ərzində izləyin.</p></div><div className="heading-icon heading-green"><Target size={22} /></div></section>
    <div className="budget-toolbar"><div className="month-switch" aria-label="Büdcə ayını seçin"><button className="icon-button" type="button" aria-label="Əvvəlki ay" onClick={() => setSelectedMonth(value => new Date(value.getFullYear(), value.getMonth() - 1, 1))}><ChevronLeft size={18} /></button><strong>{monthLabel}</strong><button className="icon-button" type="button" aria-label="Növbəti ay" onClick={() => setSelectedMonth(value => new Date(value.getFullYear(), value.getMonth() + 1, 1))}><ChevronRight size={18} /></button></div><div className="budget-total"><span>Planlaşdırılan xərc</span><strong>{money(monthTotal)}</strong><small>{money(spentTotal)} istifadə olunub</small></div></div>
    {(error || notice) && <div className={`alert ${error ? 'alert-error' : 'alert-success'}`} role={error ? 'alert' : 'status'}>{error || <><Check size={16} /> {notice}</>}</div>}
    <section className="panel budget-create-panel"><div className="panel-heading"><div><span className="eyebrow">PLAN QUR</span><h2>Kateqoriya büdcəsi əlavə et</h2></div><span className="panel-icon panel-icon-green"><Plus size={18} /></span></div>
      {availableCategories.length ? <form className="budget-form" onSubmit={createBudget}><div><label className="field-label" htmlFor="budget-category">Xərc kateqoriyası</label><select id="budget-category" className="input select" value={categoryId} onChange={event => setCategoryId(event.target.value)} required><option value="">Kateqoriya seçin</option>{availableCategories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div><div><label className="field-label" htmlFor="budget-amount">Aylıq limit (AZN)</label><input id="budget-amount" className="input" type="number" min="0.01" step="0.01" value={amount} onChange={event => setAmount(event.target.value)} placeholder="Məs. 300" required /></div><button className="button button-primary" disabled={busy || !categoryId}>{busy ? <><LoaderCircle className="spin" size={16} /> Saxlanır...</> : <><Plus size={16} /> Büdcə yarat</>}</button></form> : <p className="inline-hint"><CircleHelp size={16} /> Bu ay üçün bütün xərc kateqoriyalarına büdcə təyin edilib. Növbəti ayı da seçib əvvəlcədən plan qura bilərsiniz.</p>}
    </section>
    <section className="budget-grid" aria-label={`${monthLabel} büdcə izləyicisi`}>
      {loading ? <div className="panel data-loading"><LoaderCircle className="spin" size={18} /> Yüklənir...</div> : budgets.length ? budgets.map(item => {
        const percent = Math.max(0, item.usagePercent);
        const over = item.remainingAmount < 0;
        const warning = percent >= 80;
        return <article className={`panel budget-card ${over ? 'budget-over' : warning ? 'budget-warning' : ''}`} key={item.id}>
          <div className="budget-card-top"><div><span className="budget-category-icon"><ArrowDown size={17} /></span><div><strong>{item.categoryName}</strong><small>{monthLabel}</small></div></div><div className="budget-actions">{editingId === item.id ? <><button className="icon-button" type="button" aria-label="Yadda saxla" onClick={() => saveBudget(item)} disabled={busy}><Check size={17} /></button><button className="icon-button" type="button" aria-label="Ləğv et" onClick={() => setEditingId(null)}><ArrowLeft size={17} /></button></> : <><button className="icon-button" type="button" aria-label={`${item.categoryName} büdcəsini dəyiş`} onClick={() => { setEditingId(item.id); setEditAmount(String(item.amount)); }}><Pencil size={16} /></button><button className="icon-button danger-icon" type="button" aria-label={`${item.categoryName} büdcəsini sil`} onClick={() => removeBudget(item)} disabled={busy}><Trash2 size={16} /></button></>}</div></div>
          {editingId === item.id && <div className="budget-edit"><label className="sr-only" htmlFor={`budget-edit-${item.id}`}>Yeni aylıq limit</label><input id={`budget-edit-${item.id}`} className="input" type="number" min="0.01" step="0.01" value={editAmount} onChange={event => setEditAmount(event.target.value)} /></div>}
          <div className="budget-numbers"><strong>{money(item.actualAmount, item.currency)}</strong><span> / {money(item.amount, item.currency)}</span></div>
          <div className="budget-progress" role="progressbar" aria-label={`${item.categoryName} büdcəsinin istifadəsi`} aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}><span style={{ width: `${Math.min(100, percent)}%` }} /></div>
          <div className="budget-foot"><span>{over ? <><AlertTriangle size={14} /> Limit {money(Math.abs(item.remainingAmount), item.currency)} aşılıb</> : `${money(item.remainingAmount, item.currency)} qalıb`}</span><strong>{percent}%</strong></div>
        </article>;
      }) : <div className="panel budget-empty"><div className="empty-icon"><Target size={20} /></div><strong>Bu ay üçün büdcə yoxdur</strong><span>Bir neçə kateqoriyaya limit qoyaraq xərclərinizə nəzarət edin.</span></div>}
    </section>
  </div>;
}
