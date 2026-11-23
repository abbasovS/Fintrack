import { ArrowRight, CalendarClock, Check, ChevronLeft, ChevronRight, CirclePause, CirclePlay, LoaderCircle, Pencil, Plus, RefreshCw, Trash2, WalletCards } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../../lib/api/client';
import type { BillingInterval, Category, PageResponse, Subscription, SubscriptionCategory, SubscriptionPayload, SubscriptionStatus, SubscriptionSummary } from '../../lib/api/types';

const PAGE_SIZE = 10;
const today = () => {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

const intervals: { value: BillingInterval; label: string }[] = [
  { value: 'WEEKLY', label: 'Həftəlik' },
  { value: 'MONTHLY', label: 'Aylıq' },
  { value: 'QUARTERLY', label: 'Rüblük' },
  { value: 'YEARLY', label: 'İllik' },
];
const statuses: { value: SubscriptionStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Aktiv' },
  { value: 'PAUSED', label: 'Dayandırılıb' },
  { value: 'CANCELLED', label: 'Ləğv edilib' },
];

function money(value: number, currency = 'AZN') {
  return new Intl.NumberFormat('az-AZ', { style: 'currency', currency, maximumFractionDigits: 2 }).format(value);
}
function dateLabel(value: string) {
  return new Intl.DateTimeFormat('az-AZ', { dateStyle: 'medium' }).format(new Date(`${value}T00:00:00`));
}
function intervalLabel(value: BillingInterval) {
  return intervals.find(item => item.value === value)?.label ?? value;
}
function dueLabel(value: string) {
  const due = new Date(`${value}T00:00:00`);
  const start = new Date(`${today()}T00:00:00`);
  const difference = Math.round((due.getTime() - start.getTime()) / 86_400_000);
  if (difference < 0) return `${Math.abs(difference)} gün gecikib`;
  if (difference === 0) return 'Bu gün ödəniləcək';
  if (difference === 1) return 'Sabah ödəniləcək';
  return `${difference} gün sonra`;
}

export function SubscriptionsPage() {
  const [categories, setCategories] = useState<SubscriptionCategory[]>([]);
  const [expenseCategories, setExpenseCategories] = useState<Category[]>([]);
  const [summary, setSummary] = useState<SubscriptionSummary | null>(null);
  const [pageData, setPageData] = useState<PageResponse<Subscription> | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [editing, setEditing] = useState<Subscription | null>(null);
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [billingInterval, setBillingInterval] = useState<BillingInterval>('MONTHLY');
  const [nextBillingDate, setNextBillingDate] = useState(today());
  const [status, setStatus] = useState<SubscriptionStatus>('ACTIVE');
  const [note, setNote] = useState('');
  const [showCategoryForm, setShowCategoryForm] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [payingSubscriptionId, setPayingSubscriptionId] = useState<number | null>(null);
  const [actualAmount, setActualAmount] = useState('');
  const [expenseCategoryId, setExpenseCategoryId] = useState('');
  const [paidOn, setPaidOn] = useState(today());
  const [paymentNote, setPaymentNote] = useState('');

  useEffect(() => {
    let active = true;
    Promise.all([api.subscriptionCategories(), api.subscriptionSummary(), api.categories()])
      .then(([categoryList, subscriptionSummary, transactionCategories]) => {
        if (!active) return;
        setCategories(categoryList);
        setSummary(subscriptionSummary);
        const expenseList = transactionCategories.filter(item => item.type === 'EXPENSE');
        setExpenseCategories(expenseList);
        if (!expenseCategoryId && expenseList.length) setExpenseCategoryId(String(expenseList[0].id));
        if (!categoryId && categoryList.length) setCategoryId(String(categoryList[0].id));
      })
      .catch(err => { if (active) setError(err instanceof Error ? err.message : 'Abunəlik məlumatlarını yükləmək mümkün olmadı.'); });
    return () => { active = false; };
  }, [reloadKey]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    api.subscriptions({ page, size: PAGE_SIZE, status: statusFilter ? statusFilter as SubscriptionStatus : undefined,
      categoryId: categoryFilter ? Number(categoryFilter) : undefined })
      .then(result => { if (active) setPageData(result); })
      .catch(err => { if (active) setError(err instanceof Error ? err.message : 'Abunəlikləri yükləmək mümkün olmadı.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, statusFilter, categoryFilter, reloadKey]);

  function resetForm() {
    setEditing(null); setName(''); setAmount(''); setBillingInterval('MONTHLY');
    setNextBillingDate(today()); setStatus('ACTIVE'); setNote('');
    if (categories.length) setCategoryId(String(categories[0].id));
  }

  function beginEdit(item: Subscription) {
    setEditing(item); setName(item.name); setAmount(String(item.amount));
    setCategoryId(String(item.categoryId)); setBillingInterval(item.billingInterval);
    setNextBillingDate(item.nextBillingDate); setStatus(item.status); setNote(item.note ?? '');
    setError(''); setNotice('');
  }

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true);
    if (editing && editing.status !== 'CANCELLED' && status === 'CANCELLED'
        && !window.confirm('Ləğv edilmiş abunəlik yenidən aktiv edilə bilməz. Davam edək?')) {
      setBusy(false);
      return;
    }
    const payload: SubscriptionPayload = { name: name.trim(), amount: Number(amount), categoryId: Number(categoryId),
      billingInterval, nextBillingDate, status, note: note.trim() };
    try {
      if (editing) {
        await api.updateSubscription(editing.id, payload);
        setNotice('Abunəlik yeniləndi.');
      } else {
        await api.createSubscription(payload);
        setNotice('Abunəlik yaradıldı.');
      }
      resetForm(); setPage(0); setReloadKey(value => value + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Abunəliyi yadda saxlamaq mümkün olmadı.');
    } finally { setBusy(false); }
  }

  async function createCategory(event: FormEvent) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true);
    try {
      const created = await api.createSubscriptionCategory(newCategoryName.trim());
      setCategories(items => [...items, created].sort((a, b) => a.name.localeCompare(b.name, 'az')));
      setCategoryId(String(created.id)); setNewCategoryName(''); setShowCategoryForm(false);
      setNotice('Abunəlik kateqoriyası əlavə edildi.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kateqoriya əlavə etmək mümkün olmadı.');
    } finally { setBusy(false); }
  }

  async function remove(item: Subscription) {
    if (!window.confirm(`“${item.name}” abunəliyini silmək istəyirsiniz?`)) return;
    setError(''); setNotice(''); setBusy(true);
    try {
      await api.deleteSubscription(item.id);
      if (editing?.id === item.id) resetForm();
      setNotice('Abunəlik silindi.'); setReloadKey(value => value + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Abunəliyi silmək mümkün olmadı.');
    } finally { setBusy(false); }
  }

  async function toggleStatus(item: Subscription) {
    setError(''); setNotice(''); setBusy(true);
    const nextStatus = item.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    try {
      await api.updateSubscription(item.id, {
        categoryId: item.categoryId, name: item.name, amount: item.amount,
        billingInterval: item.billingInterval, nextBillingDate: item.nextBillingDate,
        status: nextStatus, note: item.note ?? '',
      });
      setNotice(nextStatus === 'PAUSED' ? 'Abunəlik dayandırıldı.' : 'Abunəlik yenidən aktivləşdirildi.');
      setReloadKey(value => value + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Abunəliyin statusunu dəyişmək mümkün olmadı.');
    } finally { setBusy(false); }
  }

  function openPayment(item: Subscription) {
    setPayingSubscriptionId(item.id); setActualAmount(String(item.amount));
    setPaidOn(today()); setPaymentNote(''); setError(''); setNotice('');
    if (expenseCategories.length && !expenseCategoryId) setExpenseCategoryId(String(expenseCategories[0].id));
  }

  async function recordPayment(event: FormEvent, item: Subscription) {
    event.preventDefault();
    if (!item.nextOccurrenceId || !expenseCategoryId) {
      setError('Ödənişi qeyd etmək üçün xərc kateqoriyası seçin.');
      return;
    }
    setError(''); setNotice(''); setBusy(true);
    try {
      await api.markSubscriptionOccurrencePaid(item.id, item.nextOccurrenceId, {
        actualAmount: Number(actualAmount), expenseCategoryId: Number(expenseCategoryId),
        paidOn, note: paymentNote.trim(),
      });
      setPayingSubscriptionId(null);
      setNotice('Ödəniş qeyd edildi və xərc əməliyyatı yaradıldı.');
      setReloadKey(value => value + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ödənişi qeyd etmək mümkün olmadı.');
    } finally { setBusy(false); }
  }

  async function skipPayment(item: Subscription) {
    if (!item.nextOccurrenceId || !window.confirm(`“${item.name}” üçün bu ödəniş dövrünü buraxmaq istəyirsiniz? Xərc əməliyyatı yaradılmayacaq.`)) return;
    setError(''); setNotice(''); setBusy(true);
    try {
      await api.skipSubscriptionOccurrence(item.id, item.nextOccurrenceId);
      setNotice('Ödəniş dövrü buraxıldı; xərc əməliyyatı yaradılmadı.');
      setReloadKey(value => value + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ödəniş dövrünü buraxmaq mümkün olmadı.');
    } finally { setBusy(false); }
  }

  return <div className="page-stack">
    <section className="page-heading"><div><span className="eyebrow">TƏKRARLANAN XƏRCLƏR</span><h1>Abunəliklər</h1><p>Abunəliklərinizi və yaxınlaşan ödənişləri bir yerdən izləyin.</p></div><div className="heading-icon heading-purple"><RefreshCw size={22} /></div></section>
    {(error || notice) && <div className={'alert ' + (error ? 'alert-error' : 'alert-success')} role="status">{error || <><Check size={16} /> {notice}</>}</div>}

    <section className="subscription-summary">
      <article className="subscription-stat subscription-stat-featured"><span>Öhdəliklərdən sonra qalıq · bu ay</span><strong>{summary ? money(summary.availableAfterCommitments, summary.currency) : '—'}</strong><small>{summary ? `${money(summary.actualIncome, summary.currency)} gəlir − ${money(summary.actualExpense, summary.currency)} faktiki xərc − ${money(summary.plannedExpenses, summary.currency)} ödənilməmiş abunəlik` : 'Bu ayın maliyyə xülasəsi'}</small></article>
      <article className="subscription-stat"><span>Aylıq abunə ekvivalenti</span><strong>{summary ? money(summary.monthlyEquivalent, summary.currency) : '—'}</strong><small>{summary ? `${summary.activeCount} aktiv abunəlik` : 'Aktiv abunəliklər'}</small></article>
      <article className="subscription-stat"><span>Bu ay plan üzrə</span><strong>{summary ? money(summary.plannedExpenses, summary.currency) : '—'}</strong><small>{summary ? `${summary.upcomingCount} ödəniş növbəti 30 gündə` : 'Ödənilməmiş dövrlər'}</small></article>
    </section>

    <section className="panel form-panel subscription-form-panel">
      <div className="panel-heading"><div><span className="eyebrow">{editing ? 'DƏYİŞİKLİK' : 'YENİ QEYD'}</span><h2>{editing ? 'Abunəliyi dəyiş' : 'Abunəlik əlavə et'}</h2></div><span className="panel-icon panel-icon-violet">{editing ? <Pencil size={18} /> : <Plus size={18} />}</span></div>
      <form className="subscription-form" onSubmit={submit}>
        <div><label className="field-label" htmlFor="subscription-name">Xidmətin adı</label><input className="input" id="subscription-name" value={name} onChange={event => setName(event.target.value)} placeholder="Məs. Netflix, idman zalı" maxLength={120} required /></div>
        <div><label className="field-label" htmlFor="subscription-category">Kateqoriya</label><select className="input select" id="subscription-category" value={categoryId} onChange={event => setCategoryId(event.target.value)} required><option value="">Kateqoriya seçin</option>{categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
        <div><label className="field-label" htmlFor="subscription-amount">Məbləğ ({summary?.currency ?? 'AZN'})</label><input className="input" id="subscription-amount" type="number" min="0.01" step="0.01" value={amount} onChange={event => setAmount(event.target.value)} placeholder="0.00" required /></div>
        <div><label className="field-label" htmlFor="subscription-interval">Ödəniş dövrü</label><select className="input select" id="subscription-interval" value={billingInterval} onChange={event => setBillingInterval(event.target.value as BillingInterval)}>{intervals.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}</select></div>
        <div><label className="field-label" htmlFor="subscription-next-date">Növbəti ödəniş</label><input className="input" id="subscription-next-date" type="date" value={nextBillingDate} onChange={event => setNextBillingDate(event.target.value)} required /></div>
        <div><label className="field-label" htmlFor="subscription-status">Status</label><select className="input select" id="subscription-status" value={status} onChange={event => setStatus(event.target.value as SubscriptionStatus)}>{statuses.filter(item => editing || item.value !== 'CANCELLED').map(item => <option key={item.value} value={item.value}>{item.label}</option>)}</select></div>
        <div className="subscription-note"><label className="field-label" htmlFor="subscription-note">Qeyd</label><input className="input" id="subscription-note" value={note} onChange={event => setNote(event.target.value)} maxLength={500} placeholder="İstəyə bağlı" /></div>
        <div className="subscription-form-actions"><button className="button button-primary" disabled={busy || !categories.length}>{busy ? <><LoaderCircle className="spin" size={16} /> Yadda saxlanır...</> : <>{editing ? 'Dəyişikliyi saxla' : 'Abunəliyi əlavə et'} <ArrowRight size={16} /></>}</button>{editing && <button className="button button-secondary" type="button" onClick={resetForm}>Ləğv et</button>}</div>
      </form>
      {!categories.length && !loading && <p className="inline-hint">Kateqoriyalar yüklənmədi. Yenidən yükləmək üçün səhifəni təzələyin.</p>}
      <p className="inline-hint"><span className="hint-mark">i</span><span>“Ödənişi qeyd et” seçiləndə faktiki məbləğlə xərc əməliyyatı bir dəfə yaradılır. “Burax” seçimi xərc yaratmadan növbəti dövrə keçir.</span></p>
      <div className="subscription-category-toggle"><button type="button" className="text-button" onClick={() => setShowCategoryForm(value => !value)}><Plus size={14} /> Öz kateqoriyanı yarat</button></div>
      {showCategoryForm && <form className="subscription-category-form" onSubmit={createCategory}><label className="sr-only" htmlFor="new-subscription-category">Yeni kateqoriya adı</label><input id="new-subscription-category" className="input" value={newCategoryName} onChange={event => setNewCategoryName(event.target.value)} placeholder="Məs. Kitab və media" maxLength={100} required /><button className="button button-secondary" disabled={busy}>Kateqoriyanı əlavə et</button></form>}
    </section>

    <section className="panel subscription-list-panel">
      <div className="panel-heading"><div><span className="eyebrow">ÖDƏNİŞ PLANI</span><h2>Abunəliklər <span className="count-badge">{pageData?.totalItems ?? 0}</span></h2></div><span className="panel-icon panel-icon-green"><CalendarClock size={18} /></span></div>
      <div className="subscription-filters"><label><span className="field-label">Status</span><select className="input select" value={statusFilter} onChange={event => { setPage(0); setStatusFilter(event.target.value); }}><option value="">Hamısı</option>{statuses.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label><label><span className="field-label">Kateqoriya</span><select className="input select" value={categoryFilter} onChange={event => { setPage(0); setCategoryFilter(event.target.value); }}><option value="">Hamısı</option>{categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label></div>
      {loading ? <div className="data-loading"><LoaderCircle className="spin" size={18} /> Yüklənir...</div> : pageData?.items.length ? <div className="subscription-list">{pageData.items.map(item => <div className="subscription-item" key={item.id}><article className="subscription-row">
        <span className="subscription-mark"><WalletCards size={19} /></span>
        <div className="subscription-main"><div className="subscription-title"><strong>{item.name}</strong><span className={`subscription-status status-${item.status.toLowerCase()}`}>{statuses.find(value => value.value === item.status)?.label}</span></div><span className="subscription-meta">{item.categoryName} · {intervalLabel(item.billingInterval)}</span><span className="subscription-due"><CalendarClock size={13} /> {dateLabel(item.nextBillingDate)} · {dueLabel(item.nextBillingDate)}</span>{item.note && <span className="subscription-note-text">{item.note}</span>}</div>
        <div className="subscription-amount"><strong>{money(item.amount, item.currency)}</strong><small>{money(item.monthlyEquivalent, item.currency)} / ay</small></div>
        <div className="subscription-actions"><button className="icon-button" onClick={() => beginEdit(item)} disabled={busy} aria-label={`${item.name} abunəliyini dəyiş`}><Pencil size={16} /></button>{item.status !== 'CANCELLED' && <button className="icon-button" onClick={() => toggleStatus(item)} disabled={busy} aria-label={item.status === 'ACTIVE' ? 'Abunəliyi dayandır' : 'Abunəliyi aktivləşdir'}>{item.status === 'ACTIVE' ? <CirclePause size={17} /> : <CirclePlay size={17} />}</button>}<button className="icon-button danger-icon" onClick={() => remove(item)} disabled={busy} aria-label={`${item.name} abunəliyini sil`}><Trash2 size={17} /></button></div>
      </article>
      {item.status === 'ACTIVE' && item.nextOccurrenceId && <div className="cycle-actions"><button className="button button-secondary" type="button" onClick={() => payingSubscriptionId === item.id ? setPayingSubscriptionId(null) : openPayment(item)} disabled={busy}><Check size={14} /> Ödənişi qeyd et</button><button className="text-button" type="button" onClick={() => skipPayment(item)} disabled={busy}>Bu dövrü burax</button></div>}
      {payingSubscriptionId === item.id && <form className="payment-form" onSubmit={event => recordPayment(event, item)}><div><label className="field-label" htmlFor={`paid-amount-${item.id}`}>Faktiki ödənilən ({item.currency})</label><input className="input" id={`paid-amount-${item.id}`} type="number" min="0.01" step="0.01" required value={actualAmount} onChange={event => setActualAmount(event.target.value)} /></div><div><label className="field-label" htmlFor={`paid-category-${item.id}`}>Xərc kateqoriyası</label><select className="input select" id={`paid-category-${item.id}`} value={expenseCategoryId} onChange={event => setExpenseCategoryId(event.target.value)} required><option value="">Kateqoriya seçin</option>{expenseCategories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></div><div><label className="field-label" htmlFor={`paid-date-${item.id}`}>Ödəniş tarixi</label><input className="input" id={`paid-date-${item.id}`} type="date" max={today()} value={paidOn} onChange={event => setPaidOn(event.target.value)} required /></div><div><label className="field-label" htmlFor={`paid-note-${item.id}`}>Qeyd</label><input className="input" id={`paid-note-${item.id}`} maxLength={450} value={paymentNote} onChange={event => setPaymentNote(event.target.value)} placeholder="İstəyə bağlı" /></div><div className="payment-form-actions"><button className="button button-primary" disabled={busy || !expenseCategories.length}>{busy ? 'Yadda saxlanır...' : 'Təsdiqlə və xərc yarat'}</button><button className="button button-secondary" type="button" onClick={() => setPayingSubscriptionId(null)}>Bağla</button></div>{!expenseCategories.length && <p className="inline-hint">Ödənişi xərc əməliyyatına çevirmək üçün əvvəlcə “Xərc” kateqoriyası yaradın.</p>}</form>}
      </div>)}</div> : <div className="empty-state"><div className="empty-icon"><RefreshCw size={21} /></div><strong>Abunəlik tapılmadı</strong><span>Filtrləri dəyişin və ya ilk abunəliyinizi əlavə edin.</span></div>}
      {pageData && pageData.totalPages > 1 && <div className="pagination"><span>{pageData.totalItems} nəticədən {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, pageData.totalItems)}</span><div><button className="icon-button" onClick={() => setPage(value => Math.max(0, value - 1))} disabled={!pageData.hasPrevious}><ChevronLeft size={17} /></button><span>{pageData.page + 1} / {pageData.totalPages}</span><button className="icon-button" onClick={() => setPage(value => value + 1)} disabled={!pageData.hasNext}><ChevronRight size={17} /></button></div></div>}
    </section>
  </div>;
}
