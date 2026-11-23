import { getToken } from '../auth/token';
import type {
  AuthResponse, Category, CategoryReport, CategoryType, CreateTransaction,
  PageResponse, ReportSummary, Transaction, UpdateTransaction, User,
  ReportTrendPoint,
  Subscription, SubscriptionCategory, SubscriptionPayload,
  SubscriptionStatus, SubscriptionSummary,
  MarkSubscriptionPaidPayload, SubscriptionOccurrence,
  RecurringExpense, RecurringExpensePayload, RecurringExpenseOccurrence,
  MonthlyBudget,
} from './types';

const API_BASE = '/api/v1';

export class ApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, init: RequestInit = {}, authenticated = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  const token = authenticated ? getToken() : null;
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!response.ok) {
    if (response.status === 401 && authenticated) window.dispatchEvent(new Event('fintrack:session-expired'));
    let message = `Sorğu uğursuz oldu (${response.status})`;
    try {
      const body = await response.json() as { message?: string; error?: string; detail?: string };
      message = body.message ?? body.detail ?? body.error ?? message;
    } catch { /* Use the status-based message when the API has no JSON body. */ }
    throw new ApiError(message, response.status);
  }
  if (response.status === 204 || response.headers.get('content-length') === '0') return undefined as T;
  const text = await response.text();
  return text ? JSON.parse(text) as T : undefined as T;
}

function queryString(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value));
  });
  const value = search.toString();
  return value ? `?${value}` : '';
}

export const api = {
  login: (email: string, password: string) => request<AuthResponse>('/auth/login', {
    method: 'POST', body: JSON.stringify({ email, password }),
  }, false),
  register: (name: string, email: string, password: string) => request<void>('/auth/register', {
    method: 'POST', body: JSON.stringify({ name, email, password }),
  }, false),
  verifyEmail: (email: string, code: string) => request<void>('/auth/verify-email', { method: 'POST', body: JSON.stringify({ email, code }) }, false),
  resendVerification: (email: string) => request<void>('/auth/resend-verification', { method: 'POST', body: JSON.stringify({ email }) }, false),
  updateProfile: (name: string) => request<User>('/users/me', { method: 'PUT', body: JSON.stringify({ name }) }),
  changePassword: (currentPassword: string, newPassword: string) => request<void>('/users/me/password', {
    method: 'POST', body: JSON.stringify({ currentPassword, newPassword }),
  }),
  me: () => request<User>('/users/me'),
  transactions: (params: { page?: number; size?: number; from?: string; to?: string; type?: CategoryType; categoryId?: number; query?: string }) =>
    request<PageResponse<Transaction>>(`/transactions${queryString(params)}`),
  createTransaction: (data: CreateTransaction) => request<Transaction>('/transactions', {
    method: 'POST', body: JSON.stringify(data),
  }),
  transaction: (id: number) => request<Transaction>(`/transactions/${id}`),
  updateTransaction: (id: number, data: UpdateTransaction) => request<Transaction>(`/transactions/${id}`, {
    method: 'PUT', body: JSON.stringify(data),
  }),
  deleteTransaction: (id: number) => request<void>(`/transactions/${id}`, { method: 'DELETE' }),
  categories: () => request<Category[]>('/categories'),
  createCategory: (name: string, type: CategoryType) => request<Category>('/categories', {
    method: 'POST', body: JSON.stringify({ name, type }),
  }),
  category: (id: number) => request<Category>(`/categories/${id}`),
  updateCategory: (id: number, name: string, type: CategoryType) => request<Category>(`/categories/${id}`, {
    method: 'PUT', body: JSON.stringify({ name, type }),
  }),
  deleteCategory: (id: number) => request<void>(`/categories/${id}`, { method: 'DELETE' }),
  reportSummary: (from: string, to: string) => request<ReportSummary>(`/reports/summary${queryString({ from, to })}`),
  reportCategories: (from: string, to: string, type?: CategoryType) =>
    request<CategoryReport[]>(`/reports/categories${queryString({ from, to, type })}`),
  reportTrend: (from: string, to: string, interval: 'DAY' | 'WEEK' | 'MONTH' = 'DAY') =>
    request<ReportTrendPoint[]>(`/reports/trend${queryString({ from, to, interval })}`),
  subscriptionCategories: () => request<SubscriptionCategory[]>('/subscriptions/categories'),
  createSubscriptionCategory: (name: string) => request<SubscriptionCategory>('/subscriptions/categories', {
    method: 'POST', body: JSON.stringify({ name }),
  }),
  subscriptions: (params: { page?: number; size?: number; status?: SubscriptionStatus; categoryId?: number }) =>
    request<PageResponse<Subscription>>(`/subscriptions${queryString(params)}`),
  subscriptionSummary: () => request<SubscriptionSummary>('/subscriptions/summary'),
  createSubscription: (data: SubscriptionPayload) => request<Subscription>('/subscriptions', {
    method: 'POST', body: JSON.stringify(data),
  }),
  updateSubscription: (id: number, data: SubscriptionPayload) => request<Subscription>(`/subscriptions/${id}`, {
    method: 'PUT', body: JSON.stringify(data),
  }),
  deleteSubscription: (id: number) => request<void>(`/subscriptions/${id}`, { method: 'DELETE' }),
  markSubscriptionOccurrencePaid: (subscriptionId: number, occurrenceId: number, data: MarkSubscriptionPaidPayload) =>
    request<SubscriptionOccurrence>(`/subscriptions/${subscriptionId}/occurrences/${occurrenceId}/pay`, {
      method: 'POST', body: JSON.stringify(data),
    }),
  skipSubscriptionOccurrence: (subscriptionId: number, occurrenceId: number) =>
    request<SubscriptionOccurrence>(`/subscriptions/${subscriptionId}/occurrences/${occurrenceId}/skip`, { method: 'POST' }),
  recurringExpenses: () => request<RecurringExpense[]>('/recurring-expenses'),
  createRecurringExpense: (data: RecurringExpensePayload) => request<RecurringExpense>('/recurring-expenses', {
    method: 'POST', body: JSON.stringify(data),
  }),
  updateRecurringExpense: (id: number, data: RecurringExpensePayload) => request<RecurringExpense>(`/recurring-expenses/${id}`, {
    method: 'PUT', body: JSON.stringify(data),
  }),
  pauseRecurringExpense: (id: number) => request<RecurringExpense>(`/recurring-expenses/${id}/pause`, { method: 'POST' }),
  resumeRecurringExpense: (id: number) => request<RecurringExpense>(`/recurring-expenses/${id}/resume`, { method: 'POST' }),
  deleteRecurringExpense: (id: number) => request<void>(`/recurring-expenses/${id}`, { method: 'DELETE' }),
  skipRecurringExpenseDate: (id: number, date: string) => request<RecurringExpenseOccurrence>(`/recurring-expenses/${id}/skip`, {
    method: 'POST', body: JSON.stringify({ date }),
  }),
  budgets: (month: string) => request<MonthlyBudget[]>(`/budgets${queryString({ month })}`),
  createBudget: (data: { categoryId: number; month: string; amount: number }) => request<MonthlyBudget>('/budgets', {
    method: 'POST', body: JSON.stringify(data),
  }),
  updateBudget: (id: number, amount: number) => request<MonthlyBudget>(`/budgets/${id}`, {
    method: 'PUT', body: JSON.stringify({ amount }),
  }),
  deleteBudget: (id: number) => request<void>(`/budgets/${id}`, { method: 'DELETE' }),
};
