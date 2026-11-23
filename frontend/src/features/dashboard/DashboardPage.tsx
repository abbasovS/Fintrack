import { ArrowDownLeft, ArrowLeftRight, ArrowRight, ArrowUpRight, ChevronLeft, ChevronRight, CircleHelp, LoaderCircle, Plus, ShieldCheck, WalletCards } from 'lucide-react';
import { useEffect, useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { api } from '../../lib/api/client';
import type { CategoryReport, PageResponse, ReportSummary, ReportTrendPoint, SubscriptionSummary, Transaction } from '../../lib/api/types';
import { useAuth } from '../auth/AuthProvider';

function monthRange(date: Date) {
  const asLocalInstant = (value: Date) => new Date(value.getTime() - value.getTimezoneOffset() * 60_000).toISOString();
  const from = new Date(date.getFullYear(), date.getMonth(), 1);
  const to = new Date(date.getFullYear(), date.getMonth() + 1, 1);
  return { from: asLocalInstant(from), to: asLocalInstant(to) };
}

function money(amount: number, currency = 'AZN') {
  return new Intl.NumberFormat('az-AZ', { style: 'currency', currency, maximumFractionDigits: 2 }).format(amount);
}

export function DashboardPage() {
  const { user } = useAuth();
  const [summary, setSummary] = useState<ReportSummary | null>(null);
  const [categories, setCategories] = useState<CategoryReport[]>([]);
  const [trend, setTrend] = useState<ReportTrendPoint[]>([]);
  const [recent, setRecent] = useState<Transaction[]>([]);
  const [commitments, setCommitments] = useState<SubscriptionSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedMonth, setSelectedMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const month = monthRange(selectedMonth);
  const now = new Date();
  const isCurrentMonth = selectedMonth.getFullYear() === now.getFullYear() && selectedMonth.getMonth() === now.getMonth();

  useEffect(() => {
    let active = true;
    Promise.all([
      api.reportSummary(month.from, month.to),
      api.reportCategories(month.from, month.to, 'EXPENSE'),
      api.transactions({ page: 0, size: 5 }),
      api.reportTrend(month.from, new Date().toISOString()),
      api.subscriptionSummary(),
    ]).then(([report, categoryReport, transactions, dailyTrend, subscriptionInfo]: [ReportSummary, CategoryReport[], PageResponse<Transaction>, ReportTrendPoint[], SubscriptionSummary]) => {
      if (!active) return;
      setSummary(report);
      setCategories(categoryReport);
      setRecent(transactions.items);
      setTrend(dailyTrend);
      setCommitments(subscriptionInfo);
    }).catch(err => {
      if (active) setError(err instanceof Error ? err.message : 'Məlumatları yükləmək mümkün olmadı.');
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [month.from, month.to]);

  const hour = new Date().getHours();
  const trendMax = Math.max(1, ...trend.flatMap(item => [item.income, item.expense]));
  const greeting = hour < 12 ? 'Sabahınız xeyir' : hour < 18 ? 'Günortanız xeyir' : 'Axşamınız xeyir';
  return <div className="page-stack">
    <section className="welcome-row"><div><span className="eyebrow">ŞƏXSİ MALİYYƏ İCMALI</span><h1>{greeting}, {user?.name?.split(' ')[0]} <span className="wave">✦</span></h1><p>Gəlir və xərclərinizi aylar üzrə izləyin.</p></div><div className="month-switch" aria-label="İcmal ayını seçin"><button className="icon-button" type="button" aria-label="Əvvəlki ay" onClick={() => setSelectedMonth(value => new Date(value.getFullYear(), value.getMonth() - 1, 1))}><ChevronLeft size={18} /></button><strong>{new Intl.DateTimeFormat('az-AZ', { month: 'long', year: 'numeric' }).format(selectedMonth)}</strong><button className="icon-button" type="button" aria-label="Növbəti ay" disabled={isCurrentMonth} onClick={() => setSelectedMonth(value => new Date(value.getFullYear(), value.getMonth() + 1, 1))}><ChevronRight size={18} /></button>{!isCurrentMonth && <button className="text-button" type="button" onClick={() => setSelectedMonth(new Date(now.getFullYear(), now.getMonth(), 1))}>Bu ay</button>}</div></section>
    {error && <div className="alert alert-error" role="alert">{error}</div>}
    <section className="summary-grid" aria-label="Seçilən ayın maliyyə xülasəsi">
      <SummaryCard label="Balans" value={loading ? null : summary?.balance ?? 0} currency={summary?.currency} icon={<WalletCards size={18} />} featured />
      <SummaryCard label="Gəlir" value={loading ? null : summary?.income ?? 0} currency={summary?.currency} icon={<ArrowDownLeft size={18} />} positive />
      <SummaryCard label="Xərc" value={loading ? null : summary?.expense ?? 0} currency={summary?.currency} icon={<ArrowUpRight size={18} />} />
    </section>
    {isCurrentMonth && commitments && <section className="commitment-banner"><span className="commitment-mark"><ShieldCheck size={18} /></span><div><strong>Öhdəliklərdən sonra proqnoz</strong><small>Bu ayın faktiki gəlir və xərcləri, aktiv abunəliklərin planlı ödənişləri nəzərə alınmaqla</small></div><b>{money(commitments.availableAfterCommitments, commitments.currency)}</b></section>}
    <section className="dashboard-columns">
      <div className="panel"><div className="panel-heading"><div><span className="eyebrow">SON QEYDLƏR</span><h2>Əməliyyatlar</h2></div><span className="panel-icon"><ArrowLeftRight size={18} /></span></div>
        {loading ? <div className="data-loading"><LoaderCircle className="spin" size={18} /> Yüklənir...</div> : recent.length ? <div className="recent-list">{recent.map(item => <div className="recent-item" key={item.id}><span className={'transaction-type-icon ' + (item.type === 'INCOME' ? 'income' : 'expense')}>{item.type === 'INCOME' ? <ArrowDownLeft size={17} /> : <ArrowUpRight size={17} />}</span><span className="recent-copy"><strong>{item.categoryName}</strong><small>{item.description || new Intl.DateTimeFormat('az-AZ', { dateStyle: 'medium' }).format(new Date(item.createdAt))}</small></span><strong className={'recent-amount ' + (item.type === 'INCOME' ? 'text-income' : '')}>{item.type === 'INCOME' ? '+' : '−'}{money(item.amount, summary?.currency)}</strong></div>)}</div> : <EmptyState text="Bu dövr üçün əməliyyat yoxdur." />}
        <Link className="text-link" to="/transactions">Bütün əməliyyatlar <ArrowRight size={14} /></Link>
      </div>
      <div className="panel"><div className="panel-heading"><div><span className="eyebrow">XƏRCLƏRİN BÖLÜNMƏSİ</span><h2>Kateqoriyalar</h2></div><span className="panel-icon panel-icon-soft"><WalletCards size={18} /></span></div>
        {loading ? <div className="data-loading"><LoaderCircle className="spin" size={18} /> Yüklənir...</div> : categories.length ? <div className="category-report-list">{categories.map(item => <div className="category-report" key={item.categoryId + '-' + item.type}><div className="category-report-top"><span>{item.categoryName}</span><strong>{money(item.totalAmount, summary?.currency)}</strong></div><div className="progress-track"><span style={{ width: Math.max(4, (item.totalAmount / categories[0].totalAmount) * 100) + '%' }} /></div><small>{item.transactionCount} əməliyyat</small></div>)}</div> : <EmptyState text="Xərc kateqoriyası üzrə məlumat yoxdur." />}
        <Link className="text-link" to="/categories">Kateqoriyalara keç <ArrowRight size={14} /></Link>
      </div>
    </section>
    <section className="dashboard-columns">
      <div className="panel trend-panel"><div className="panel-heading"><div><span className="eyebrow">GÜNLÜK HƏRƏKƏT</span><h2>Gəlir və xərc</h2></div><span className="panel-icon panel-icon-orange"><ArrowLeftRight size={18} /></span></div>
        {loading ? <div className="data-loading"><LoaderCircle className="spin" size={18} /> Yüklənir...</div> : trend.some(item => item.income > 0 || item.expense > 0) ? <><div className="trend-legend"><span><i className="legend-income" /> Gəlir</span><span><i className="legend-expense" /> Xərc</span></div><div className="trend-chart" role="img" aria-label="Günlər üzrə gəlir və xərc qrafiki">{trend.map(item => { const day = Number(item.period.slice(-2)); return <div className="trend-day" key={item.period} title={item.period + ' · gəlir ' + money(item.income, summary?.currency) + ' · xərc ' + money(item.expense, summary?.currency)}><div className="trend-bars"><span className="trend-bar trend-income" style={{ height: Math.max(item.income ? 3 : 0, item.income / trendMax * 100) + '%' }} /><span className="trend-bar trend-expense" style={{ height: Math.max(item.expense ? 3 : 0, item.expense / trendMax * 100) + '%' }} /></div><small>{day === 1 || day % 5 === 0 ? day : ''}</small></div>; })}</div><div className="trend-footnote">Günlər · {money(trend.reduce((sum, item) => sum + item.income, 0), summary?.currency)} gəlir · {money(trend.reduce((sum, item) => sum + item.expense, 0), summary?.currency)} xərc</div></> : <EmptyState text="Bu ay üçün trend məlumatı yoxdur." />}
        <div className="quick-actions compact-actions"><Link to="/transactions" className="quick-action"><span className="quick-icon quick-orange"><Plus size={17} /></span><span><strong>Əməliyyat əlavə et</strong><small>Gəlir və ya xərc yaradın</small></span><ArrowRight size={16} className="quick-arrow" /></Link></div>
      </div>
      <div className="panel info-panel"><div className="panel-heading"><div><span className="eyebrow">HESAB</span><h2>Məlumatlarınız qorunur</h2></div><span className="panel-icon panel-icon-soft"><ShieldCheck size={18} /></span></div><div className="api-status"><span className="api-status-dot" /><div><strong>{user?.name}</strong><small>{user?.email}</small></div></div><div className="api-note"><CircleHelp size={16} /><p>Xülasə və əməliyyatlar PostgreSQL-dən cari hesab üçün alınır. Əvvəlki ümumi kateqoriyalar yalnız oxumaq üçün saxlanılıb.</p></div></div>
    </section>
  </div>;
}

function SummaryCard({ label, value, currency, icon, featured = false, positive = false }: { label: string; value: number | null; currency?: string; icon: ReactNode; featured?: boolean; positive?: boolean }) {
  const classes = 'summary-card' + (featured ? ' summary-card-featured' : '');
  const valueClasses = 'summary-value' + (value === null ? ' summary-muted' : '');
  return <div className={classes}><div className="summary-top"><span className="summary-label">{label}</span><span className={'summary-icon' + (positive ? ' icon-green' : '')}>{icon}</span></div><div className={valueClasses}>{value === null ? 'Yüklənir...' : money(value, currency)}</div><div className="summary-bottom"><span>Seçilən ay</span></div></div>;
}

function EmptyState({ text }: { text: string }) {
  return <div className="empty-state"><div className="empty-icon"><CircleHelp size={20} /></div><span>{text}</span></div>;
}
