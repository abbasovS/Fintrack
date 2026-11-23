import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { CalendarDays, Check, CirclePause, LoaderCircle, Pencil, Play, Plus, Repeat2, Trash2 } from 'lucide-react';
import { Link } from 'react-router';
import { api } from '../../lib/api/client';
import type { Category, DayOfWeek, RecurringExpense, RecurringExpenseFrequency, RecurringExpensePayload } from '../../lib/api/types';
import './recurring-expenses.css';

const weekdays: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Bazar ertəsi' }, { value: 'TUESDAY', label: 'Çərşənbə axşamı' },
  { value: 'WEDNESDAY', label: 'Çərşənbə' }, { value: 'THURSDAY', label: 'Cümə axşamı' },
  { value: 'FRIDAY', label: 'Cümə' }, { value: 'SATURDAY', label: 'Şənbə' }, { value: 'SUNDAY', label: 'Bazar' },
];

function today() {
  const date = new Date();
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

function displayDate(value: string) {
  return new Intl.DateTimeFormat('az-AZ', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`));
}

function money(amount: number, currency: string) {
  return new Intl.NumberFormat('az-AZ', { style: 'currency', currency, maximumFractionDigits: 2 }).format(amount);
}

function scheduleLabel(rule: RecurringExpense) {
  if (rule.frequency === 'DAILY') return rule.skipWeekends ? 'Hər iş günü' : 'Hər gün';
  if (rule.frequency === 'WEEKLY') return `Hər həftə · ${weekdays.find(day => day.value === rule.dayOfWeek)?.label ?? ''}`;
  return `Hər ayın ${rule.dayOfMonth}-i`;
}

function statusLabel(status: RecurringExpense['status']) {
  if (status === 'ACTIVE') return 'Aktiv';
  if (status === 'PAUSED') return 'Dayandırılıb';
  return 'Bitib';
}

export function RecurringExpensesPage() {
  return <RecurringTransactionsPage transactionType="EXPENSE" />;
}

export function RecurringIncomePage() {
  return <RecurringTransactionsPage transactionType="INCOME" />;
}

function RecurringTransactionsPage({ transactionType }: { transactionType: 'INCOME' | 'EXPENSE' }) {
  const isIncome = transactionType === 'INCOME';
  const [rules, setRules] = useState<RecurringExpense[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState<RecurringExpense | null>(null);
  const [skipDates, setSkipDates] = useState<Record<number, string>>({});
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [frequency, setFrequency] = useState<RecurringExpenseFrequency>('DAILY');
  const [dayOfWeek, setDayOfWeek] = useState<DayOfWeek>('MONDAY');
  const [dayOfMonth, setDayOfMonth] = useState('1');
  const [skipWeekends, setSkipWeekends] = useState(true);
  const [startDate, setStartDate] = useState(today());
  const [endDate, setEndDate] = useState('');

  const transactionCategories = useMemo(() => categories.filter(category => category.type === transactionType), [categories, transactionType]);

  useEffect(() => {
    setLoading(true);
    Promise.all([api.recurringExpenses(), api.categories()])
      .then(([ruleList, categoryList]) => {
        setRules(ruleList.filter(rule => rule.categoryType === transactionType));
        setCategories(categoryList);
        setCategoryId(current => current || String(categoryList.find(category => category.type === transactionType)?.id ?? ''));
      })
      .catch(err => setError(err instanceof Error ? err.message : 'Təkrarlanan xərcləri yükləmək mümkün olmadı.'))
      .finally(() => setLoading(false));
  }, [reload, transactionType]);

  function resetForm() {
    setEditing(null);
    setName('');
    setAmount('');
    setFrequency('DAILY');
    setDayOfWeek('MONDAY');
    setDayOfMonth('1');
    setSkipWeekends(true);
    setStartDate(today());
    setEndDate('');
  }

  function editRule(rule: RecurringExpense) {
    setEditing(rule);
    setName(rule.name);
    setAmount(String(rule.amount));
    setCategoryId(String(rule.categoryId));
    setFrequency(rule.frequency);
    setDayOfWeek(rule.dayOfWeek ?? 'MONDAY');
    setDayOfMonth(String(rule.dayOfMonth ?? 1));
    setSkipWeekends(rule.skipWeekends);
    setStartDate(rule.startDate < today() ? today() : rule.startDate);
    setEndDate(rule.endDate && rule.endDate >= today() ? rule.endDate : '');
    setError('');
    setNotice('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError('');
    setNotice('');
    const payload: RecurringExpensePayload = {
      name: name.trim(),
      amount: Number(amount),
      categoryId: Number(categoryId),
      frequency,
      dayOfWeek: frequency === 'WEEKLY' ? dayOfWeek : null,
      dayOfMonth: frequency === 'MONTHLY' ? Number(dayOfMonth) : null,
      skipWeekends: frequency === 'DAILY' && skipWeekends,
      startDate,
      endDate: endDate || null,
    };
    try {
      if (editing) await api.updateRecurringExpense(editing.id, payload);
      else await api.createRecurringExpense(payload);
      setNotice(editing ? 'Qayda yeniləndi.' : (isIncome ? 'Təkrarlanan gəlir qaydası yaradıldı.' : 'Təkrarlanan xərc qaydası yaradıldı.'));
      resetForm();
      setReload(value => value + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Qaydanı yadda saxlamaq mümkün olmadı.');
    } finally {
      setBusy(false);
    }
  }

  async function toggle(rule: RecurringExpense) {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      if (rule.status === 'ACTIVE') await api.pauseRecurringExpense(rule.id);
      else await api.resumeRecurringExpense(rule.id);
      setReload(value => value + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Qaydanın statusunu dəyişmək mümkün olmadı.');
    } finally {
      setBusy(false);
    }
  }

  async function skipDate(rule: RecurringExpense) {
    const date = skipDates[rule.id] || today();
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await api.skipRecurringExpenseDate(rule.id, date);
      setNotice(`${displayDate(date)} tarixi üçün avtomatik xərc ötürüldü.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Tarixi ötürmək mümkün olmadı.');
    } finally {
      setBusy(false);
    }
  }

  async function remove(rule: RecurringExpense) {
    if (!window.confirm(`“${rule.name}” avtomatik xərc qaydasını silmək istəyirsiniz? Əvvəl yaranmış xərclər saxlanacaq.`)) return;
    setBusy(true);
    setError('');
    try {
      await api.deleteRecurringExpense(rule.id);
      setRules(current => current.filter(item => item.id !== rule.id));
      if (editing?.id === rule.id) resetForm();
      setNotice('Qayda silindi. Əvvəllər yaranmış xərclər saxlanıldı.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Qaydanı silmək mümkün olmadı.');
    } finally {
      setBusy(false);
    }
  }

  return <>
    <section className="page-heading recurring-heading">
      <div><span className="eyebrow">{isIncome ? 'TƏKRARLANAN GƏLİR' : 'AVTOMATİK XƏRCLƏR'}</span><h1>{isIncome ? 'Təkrarlanan gəlirlər' : 'Təkrarlanan xərclər'}</h1><p>{isIncome ? 'Maaş və digər müntəzəm gəlirləri bir dəfə planlaşdırın.' : 'Daimi xərcləri planlaşdırın, uyğun gəlməyən günü ötürün.'}</p></div>
      <div className="heading-icon heading-purple"><Repeat2 size={22} /></div>
    </section>

    {error && <div className="alert alert-error" role="alert">{error}</div>}
    {notice && <div className="alert alert-success" role="status">{notice}</div>}

    <section className="panel recurring-form-panel">
      <div className="panel-heading"><div><span className="eyebrow">{editing ? 'QAYDANI DƏYİŞ' : 'YENİ QAYDA'}</span><h2>{editing ? 'Xərc qaydasını redaktə et' : 'Avtomatik xərc yarat'}</h2></div><span className="panel-icon panel-icon-green"><Plus size={18} /></span></div>
      {!transactionCategories.length && !loading ? <p className="inline-hint">Əvvəlcə <Link to="/categories">{isIncome ? 'gəlir' : 'xərc'} kateqoriyası</Link> yaradın.</p> : <form className="recurring-form" onSubmit={save}>
        <div><label className="field-label" htmlFor="recurring-name">{isIncome ? 'Gəlirin adı' : 'Xərcin adı'}</label><input className="input" id="recurring-name" value={name} onChange={event => setName(event.target.value)} placeholder={isIncome ? 'Məs. Maaş' : 'Məs. Metro'} maxLength={120} required /></div>
        <div><label className="field-label" htmlFor="recurring-amount">Hər dəfə məbləğ</label><input className="input" id="recurring-amount" type="number" min="0.01" step="0.01" value={amount} onChange={event => setAmount(event.target.value)} placeholder="0.00" required /></div>
        <div><label className="field-label" htmlFor="recurring-category">Kateqoriya</label><select className="input select" id="recurring-category" value={categoryId} onChange={event => setCategoryId(event.target.value)} required><option value="">Kateqoriya seçin</option>{transactionCategories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></div>
        <div><label className="field-label" htmlFor="recurring-frequency">Təkrarlanma</label><select className="input select" id="recurring-frequency" value={frequency} onChange={event => setFrequency(event.target.value as RecurringExpenseFrequency)}><option value="DAILY">Hər gün</option><option value="WEEKLY">Həftənin seçilmiş günü</option><option value="MONTHLY">Ayda bir dəfə</option></select></div>
        {frequency === 'DAILY' && <label className="recurring-checkbox"><input type="checkbox" checked={skipWeekends} onChange={event => setSkipWeekends(event.target.checked)} /> Həftəsonlarını ötür</label>}
        {frequency === 'WEEKLY' && <div><label className="field-label" htmlFor="recurring-weekday">Həftənin günü</label><select className="input select" id="recurring-weekday" value={dayOfWeek} onChange={event => setDayOfWeek(event.target.value as DayOfWeek)}>{weekdays.map(day => <option key={day.value} value={day.value}>{day.label}</option>)}</select></div>}
        {frequency === 'MONTHLY' && <div><label className="field-label" htmlFor="recurring-monthday">Ayın günü</label><input className="input" id="recurring-monthday" type="number" min="1" max="31" value={dayOfMonth} onChange={event => setDayOfMonth(event.target.value)} required /><small className="recurring-field-hint">29–31 seçiləndə qısa aylarda son gün istifadə olunur.</small></div>}
        <div><label className="field-label" htmlFor="recurring-start">Başlama tarixi</label><input className="input" id="recurring-start" type="date" min={today()} value={startDate} onChange={event => setStartDate(event.target.value)} required /></div>
        <div><label className="field-label" htmlFor="recurring-end">Bitmə tarixi <span className="muted-label">(istəyə bağlı)</span></label><input className="input" id="recurring-end" type="date" min={startDate} value={endDate} onChange={event => setEndDate(event.target.value)} /></div>
        <div className="recurring-form-actions"><button className="button button-primary" disabled={busy || loading}>{busy ? <><LoaderCircle className="spin" size={16} /> Yadda saxlanır...</> : <><Check size={16} /> {editing ? 'Dəyişiklikləri saxla' : 'Qayda yarat'}</>}</button>{editing && <button className="button button-secondary" type="button" onClick={resetForm}>Ləğv et</button>}</div>
      </form>}
      <p className="recurring-note"><CalendarDays size={15} /> {isIncome ? 'Gəlir seçilmiş tarixlərdə avtomatik qeyd olunacaq.' : 'Xərc seçilmiş günün sonunda avtomatik yazılır. O gün olmayacaqsa, həmin tarixi ötürün.'}</p>
    </section>

    <section className="panel recurring-list-panel">
      <div className="panel-heading"><div><span className="eyebrow">ŞƏXSİ QAYDALARINIZ</span><h2>{isIncome ? 'Təkrarlanan gəlirlər' : 'Avtomatik xərclər'}</h2></div><span className="panel-icon panel-icon-soft"><Repeat2 size={18} /></span></div>
      {loading ? <div className="data-loading"><LoaderCircle className="spin" size={18} /> Yüklənir...</div> : rules.length ? <div className="recurring-list">{rules.map(rule => <article className="recurring-card" key={rule.id}>
        <div className="recurring-card-main"><div className="recurring-card-title"><span className="recurring-card-icon"><Repeat2 size={17} /></span><div><strong>{rule.name}</strong><small>{rule.categoryName} · {scheduleLabel(rule)}</small></div></div><div className="recurring-card-amount">{money(rule.amount, rule.currency)}<span className={`recurring-status status-${rule.status.toLowerCase()}`}>{statusLabel(rule.status)}</span></div></div>
        <div className="recurring-card-details"><span>Növbəti: <strong>{displayDate(rule.nextRunDate)}</strong></span>{rule.endDate && <span>Son tarix: {displayDate(rule.endDate)}</span>}</div>
        <div className="recurring-card-actions">
          {rule.status !== 'COMPLETED' && <button className="button button-secondary recurring-action" onClick={() => toggle(rule)} disabled={busy}>{rule.status === 'ACTIVE' ? <><CirclePause size={15} /> Dayandır</> : <><Play size={15} /> Davam et</>}</button>}
          {rule.status !== 'COMPLETED' && <div className="recurring-skip"><input className="input" aria-label={`${rule.name} üçün ötürüləcək tarix`} type="date" min={today()} value={skipDates[rule.id] ?? rule.nextRunDate} onChange={event => setSkipDates(current => ({ ...current, [rule.id]: event.target.value }))} /><button className="button button-secondary recurring-action" onClick={() => skipDate(rule)} disabled={busy}>Tarixi ötür</button></div>}
          {rule.status !== 'COMPLETED' && <button className="icon-button" onClick={() => editRule(rule)} disabled={busy} aria-label={`${rule.name} qaydasını redaktə et`}><Pencil size={16} /></button>}
          <button className="icon-button danger-icon" onClick={() => remove(rule)} disabled={busy} aria-label={`${rule.name} qaydasını sil`}><Trash2 size={17} /></button>
        </div>
      </article>)}</div> : <div className="empty-state"><div className="empty-icon"><Repeat2 size={21} /></div><strong>{isIncome ? 'Təkrarlanan gəlir yoxdur' : 'Təkrarlanan xərc yoxdur'}</strong><span>{isIncome ? 'Maaş kimi müntəzəm gəliri yuxarıdan planlaşdırın.' : 'Daimi xərci yuxarıdan bir dəfə qurun.'}</span></div>}
    </section>
  </>;
}
