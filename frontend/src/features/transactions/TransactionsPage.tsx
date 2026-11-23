import { ArrowDownLeft, ArrowLeftRight, ArrowRight, ArrowUpRight, Check, ChevronLeft, ChevronRight, Download, LoaderCircle, Pencil, Plus, Search, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { api } from '../../lib/api/client';
import type { Category, CategoryType, PageResponse, Transaction } from '../../lib/api/types';

const PAGE_SIZE = 10;
const EXPORT_PAGE_SIZE = 100;
const today = new Date();
const firstOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);

function dateInput(date: Date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
}
function fromDate(value: string) {
  return value ? new Date(value + 'T00:00:00').toISOString() : undefined;
}
function toDateExclusive(value: string) {
  return value ? new Date(new Date(value + 'T00:00:00').getTime() + 86_400_000).toISOString() : undefined;
}
function money(value: number) {
  return new Intl.NumberFormat('az-AZ', { style: 'currency', currency: 'AZN', maximumFractionDigits: 2 }).format(value);
}

export function TransactionsPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [pageData, setPageData] = useState<PageResponse<Transaction> | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ from: dateInput(firstOfMonth), to: dateInput(today), type: '', categoryId: '', query: '' });
  const [draftFilters, setDraftFilters] = useState(filters);
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [categoryId, setCategoryId] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');

  const selectedCategory = useMemo(() => categories.find(item => item.id === Number(categoryId)), [categories, categoryId]);

  useEffect(() => {
    api.categories().then(setCategories).catch(err => setError(err instanceof Error ? err.message : 'Kateqoriyaları yükləmək mümkün olmadı.'));
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    api.transactions({
      page, size: PAGE_SIZE,
      from: fromDate(filters.from), to: toDateExclusive(filters.to),
      type: filters.type ? filters.type as CategoryType : undefined,
      categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
      query: filters.query || undefined,
    }).then(result => { if (active) setPageData(result); })
      .catch(err => { if (active) setError(err instanceof Error ? err.message : 'Əməliyyatları yükləmək mümkün olmadı.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, filters]);

  function clearMessages() { setError(''); setNotice(''); }

  function startEdit(transaction: Transaction) {
    setEditing(transaction);
    setCategoryId(String(transaction.categoryId));
    setAmount(String(transaction.amount));
    setDescription(transaction.description ?? '');
    clearMessages();
  }

  function resetForm() {
    setEditing(null); setCategoryId(''); setAmount(''); setDescription('');
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    clearMessages();
    if (!selectedCategory) { setError('Əvvəlcə kateqoriya seçin.'); return; }
    setBusy(true);
    const payload = { categoryId: selectedCategory.id, amount: Number(amount), type: selectedCategory.type, description: description.trim() };
    try {
      if (editing) {
        await api.updateTransaction(editing.id, payload);
        setNotice('Əməliyyat yeniləndi.');
      } else {
        const created = await api.createTransaction(payload);
        setNotice('Əməliyyat yaradıldı · ID ' + created.id);
      }
      resetForm();
      setPage(0);
      setFilters(current => ({ ...current }));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Əməliyyatı yadda saxlamaq mümkün olmadı.');
    } finally { setBusy(false); }
  }

  async function remove(transaction: Transaction) {
    const linkedPayment = transaction.description?.startsWith('Abunəlik ödənişi:') ?? false;
    const message = '“' + (transaction.description || transaction.categoryName) + '” əməliyyatını silmək istəyirsiniz?'
      + (linkedPayment ? '\n\nBu abunəlik ödənişi ilə əlaqəlidir. Silindikdə ödəniş dövrü yenidən ödənilməmiş olacaq.' : '');
    if (!window.confirm(message)) return;
    clearMessages();
    setBusy(true);
    try {
      await api.deleteTransaction(transaction.id);
      setNotice('Əməliyyat silindi.');
      if (editing?.id === transaction.id) resetForm();
      setPageData(current => current ? { ...current, items: current.items.filter(item => item.id !== transaction.id), totalItems: current.totalItems - 1 } : current);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Əməliyyatı silmək mümkün olmadı.');
    } finally { setBusy(false); }
  }

  function applyFilters(event: FormEvent) {
    event.preventDefault();
    if (draftFilters.from && draftFilters.to && draftFilters.from > draftFilters.to) {
      setError('Başlanğıc tarixi son tarixdən sonra ola bilməz.');
      return;
    }
    clearMessages();
    setPage(0);
    setFilters({ ...draftFilters });
  }

  async function exportCsv() {
    clearMessages();
    setExporting(true);
    try {
      const params = {
        size: EXPORT_PAGE_SIZE,
        from: fromDate(filters.from),
        to: toDateExclusive(filters.to),
        type: filters.type ? filters.type as CategoryType : undefined,
        categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
        query: filters.query || undefined,
      };
      const first = await api.transactions({ ...params, page: 0 });
      const remaining: PageResponse<Transaction>[] = [];
      for (let start = 1; start < first.totalPages; start += 10) {
        const batch = await Promise.all(Array.from({ length: Math.min(10, first.totalPages - start) }, (_, index) =>
          api.transactions({ ...params, page: start + index })));
        remaining.push(...batch);
      }
      const rows = [
        ['Tarix', 'Növ', 'Kateqoriya', 'Məbləğ (AZN)', 'Təsvir'],
        ...[...first.items, ...remaining.flatMap(result => result.items)].map(item => [
          new Intl.DateTimeFormat('az-AZ', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(item.createdAt)),
          item.type === 'INCOME' ? 'Gəlir' : 'Xərc',
          item.categoryName,
          String(item.amount),
          item.description ?? '',
        ]),
      ];
      const csv = rows.map(row => row.map(value => {
        const safe = /^[\s]*[=+@-]/.test(value) ? `'${value}` : value;
        return `"${safe.replace(/"/g, '""')}"`;
      }).join(',')).join('\r\n');
      const blob = new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `fintrack-emeliyyatlar-${dateInput(new Date())}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      setNotice(`${rows.length - 1} əməliyyat CSV faylına ixrac edildi.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'CSV faylını hazırlamaq mümkün olmadı.');
    } finally {
      setExporting(false);
    }
  }

  return <div className="page-stack">
    <section className="page-heading"><div><span className="eyebrow">MƏLUMATLARINIZ</span><h1>Əməliyyatlar</h1><p>PostgreSQL-dəki real əməliyyatlarınız və tarix filtrləri.</p></div><div className="heading-icon heading-orange"><ArrowLeftRight size={22} /></div></section>
    {(error || notice) && <div className={'alert ' + (error ? 'alert-error' : 'alert-success')} role="status">{error || <><Check size={16} /> {notice}</>}</div>}

    <section className="panel filter-panel"><div className="panel-heading"><div><span className="eyebrow">AXTARIŞ VƏ FİLTRLƏR</span><h2>Əməliyyatları süz</h2></div><Search size={17} className="muted-icon" /></div>
      <form className="filter-form" onSubmit={applyFilters}>
        <div><label className="field-label" htmlFor="from-date">Başlanğıc</label><input className="input" id="from-date" type="date" value={draftFilters.from} onChange={e => setDraftFilters(current => ({ ...current, from: e.target.value }))} /></div>
        <div><label className="field-label" htmlFor="to-date">Son tarix</label><input className="input" id="to-date" type="date" value={draftFilters.to} onChange={e => setDraftFilters(current => ({ ...current, to: e.target.value }))} /></div>
        <div><label className="field-label" htmlFor="filter-type">Növ</label><select className="input select" id="filter-type" value={draftFilters.type} onChange={e => setDraftFilters(current => ({ ...current, type: e.target.value }))}><option value="">Hamısı</option><option value="INCOME">Gəlir</option><option value="EXPENSE">Xərc</option></select></div>
        <div><label className="field-label" htmlFor="filter-category">Kateqoriya</label><select className="input select" id="filter-category" value={draftFilters.categoryId} onChange={e => setDraftFilters(current => ({ ...current, categoryId: e.target.value }))}><option value="">Hamısı</option>{categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
        <div className="filter-search"><label className="field-label" htmlFor="filter-query">Təsvir üzrə</label><input className="input" id="filter-query" value={draftFilters.query} onChange={e => setDraftFilters(current => ({ ...current, query: e.target.value }))} placeholder="Axtarış..." /></div>
        <button className="button button-dark filter-submit"><Search size={15} /> Tətbiq et</button>
      </form>
    </section>

    <section className="panel form-panel transaction-form-panel"><div className="panel-heading"><div><span className="eyebrow">{editing ? 'DƏYİŞİKLİK' : 'YENİ QEYD'}</span><h2>{editing ? 'Əməliyyatı dəyiş' : 'Əməliyyat əlavə et'}</h2></div><span className="panel-icon panel-icon-orange">{editing ? <Pencil size={18} /> : <Plus size={18} />}</span></div>
      <form className="transaction-entry-form" onSubmit={submit}>
        <div><label className="field-label" htmlFor="transaction-category">Kateqoriya</label><select className="input select" id="transaction-category" value={categoryId} onChange={e => setCategoryId(e.target.value)} required><option value="">Kateqoriya seçin</option>{categories.map(item => <option key={item.id} value={item.id}>{item.name} · {item.type === 'INCOME' ? 'Gəlir' : 'Xərc'}</option>)}</select></div>
        <div><label className="field-label" htmlFor="transaction-amount">Məbləğ (AZN)</label><input className="input" id="transaction-amount" type="number" min="0.01" step="0.01" value={amount} onChange={e => setAmount(e.target.value)} placeholder="0.00" required /></div>
        <div className="entry-description"><label className="field-label" htmlFor="transaction-description">Təsvir</label><input className="input" id="transaction-description" maxLength={500} value={description} onChange={e => setDescription(e.target.value)} placeholder="İstəyə bağlı" /></div>
        <div className="entry-actions"><button className="button button-primary" disabled={busy || !categories.length}>{busy ? <><LoaderCircle className="spin" size={16} /> Yadda saxlanır...</> : <>{editing ? 'Yenilə' : 'Əməliyyatı əlavə et'} <ArrowRight size={16} /></>}</button>{editing && <button className="button button-secondary" type="button" onClick={resetForm}>Ləğv et</button>}</div>
      </form>
    </section>

    <section className="panel transactions-list-panel">
      <div className="panel-heading"><div><span className="eyebrow">HESABINIZIN TARİXÇƏSİ</span><h2>Əməliyyatlar <span className="count-badge">{pageData?.totalItems ?? 0}</span></h2></div><button className="button button-secondary export-button" type="button" onClick={exportCsv} disabled={exporting || !pageData?.totalItems}>{exporting ? <><LoaderCircle className="spin" size={16} /> Hazırlanır...</> : <><Download size={16} /> CSV yüklə</>}</button></div>
      {loading ? <div className="data-loading"><LoaderCircle className="spin" size={18} /> Yüklənir...</div> : pageData?.items.length ? <div className="transaction-table-wrap"><table className="transaction-table"><thead><tr><th>Əməliyyat</th><th>Kateqoriya</th><th>Tarix</th><th>Məbləğ</th><th><span className="sr-only">Əməliyyatlar</span></th></tr></thead><tbody>{pageData.items.map(item => <tr key={item.id}><td><div className="transaction-cell"><span className={'transaction-type-icon ' + (item.type === 'INCOME' ? 'income' : 'expense')}>{item.type === 'INCOME' ? <ArrowDownLeft size={17} /> : <ArrowUpRight size={17} />}</span><span><strong>{item.description || item.categoryName}</strong><small>#{item.id}</small></span></div></td><td>{item.categoryName}</td><td>{new Intl.DateTimeFormat('az-AZ', { dateStyle: 'medium' }).format(new Date(item.createdAt))}</td><td><strong className={item.type === 'INCOME' ? 'text-income' : ''}>{item.type === 'INCOME' ? '+' : '−'}{money(item.amount)}</strong></td><td className="row-actions"><button className="icon-button" onClick={() => startEdit(item)} disabled={busy} aria-label="Əməliyyatı dəyiş"><Pencil size={16} /></button><button className="icon-button danger-icon" onClick={() => remove(item)} disabled={busy} aria-label="Əməliyyatı sil"><Trash2 size={17} /></button></td></tr>)}</tbody></table></div> : <div className="empty-state"><div className="empty-icon"><ArrowLeftRight size={21} /></div><strong>Əməliyyat tapılmadı</strong><span>Tarix və filtrləri dəyişin və ya yeni qeyd yaradın.</span></div>}
      {pageData && pageData.totalPages > 1 && <div className="pagination"><span>{pageData.totalItems} nəticədən {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, pageData.totalItems)}</span><div><button className="icon-button" onClick={() => setPage(current => Math.max(0, current - 1))} disabled={!pageData.hasPrevious}><ChevronLeft size={17} /></button><span>{pageData.page + 1} / {pageData.totalPages}</span><button className="icon-button" onClick={() => setPage(current => current + 1)} disabled={!pageData.hasNext}><ChevronRight size={17} /></button></div></div>}
    </section>
  </div>;
}
