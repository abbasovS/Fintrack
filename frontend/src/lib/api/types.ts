export type CategoryType = 'INCOME' | 'EXPENSE';
export type SubscriptionStatus = 'ACTIVE' | 'PAUSED' | 'CANCELLED';
export type BillingInterval = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
export type RecurringExpenseFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type RecurringExpenseStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  createdAt: string;
}

export interface Category {
  id: number;
  name: string;
  type: CategoryType;
}

export interface Transaction {
  id: number;
  userId: number;
  categoryId: number;
  categoryName: string;
  amount: number;
  type: CategoryType;
  description: string | null;
  createdAt: string;
}

export interface CreateTransaction {
  categoryId: number;
  amount: number;
  type: CategoryType;
  description: string;
}

export type UpdateTransaction = CreateTransaction;

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ReportSummary {
  from: string;
  to: string;
  currency: string;
  income: number;
  expense: number;
  balance: number;
  transactionCount: number;
}

export interface CategoryReport {
  categoryId: number;
  categoryName: string;
  type: CategoryType;
  totalAmount: number;
  transactionCount: number;
}

export interface ReportTrendPoint {
  period: string;
  income: number;
  expense: number;
}

export interface SubscriptionCategory {
  id: number;
  name: string;
}

export interface Subscription {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  amount: number;
  currency: string;
  billingInterval: BillingInterval;
  nextBillingDate: string;
  status: SubscriptionStatus;
  note: string | null;
  monthlyEquivalent: number;
  nextOccurrenceId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface SubscriptionSummary {
  currency: string;
  activeCount: number;
  monthlyEquivalent: number;
  upcomingCount: number;
  upcomingAmount: number;
  monthStart: string;
  monthEnd: string;
  actualIncome: number;
  actualExpense: number;
  plannedExpenses: number;
  availableAfterCommitments: number;
}

export interface SubscriptionPayload {
  categoryId: number;
  name: string;
  amount: number;
  billingInterval: BillingInterval;
  nextBillingDate: string;
  status: SubscriptionStatus;
  note: string;
}

export interface MarkSubscriptionPaidPayload {
  actualAmount: number;
  expenseCategoryId: number;
  paidOn: string;
  note: string;
}

export interface SubscriptionOccurrence {
  id: number;
  subscriptionId: number;
  subscriptionName: string;
  dueDate: string;
  expectedAmount: number;
  actualAmount: number | null;
  status: 'PLANNED' | 'PAID' | 'SKIPPED';
  paidOn: string | null;
  transactionId: number | null;
  currency: string;
}

export interface RecurringExpense {
  id: number;
  name: string;
  amount: number;
  currency: string;
  categoryId: number;
  categoryName: string;
  categoryType: CategoryType;
  frequency: RecurringExpenseFrequency;
  dayOfWeek: DayOfWeek | null;
  dayOfMonth: number | null;
  skipWeekends: boolean;
  startDate: string;
  endDate: string | null;
  nextRunDate: string;
  status: RecurringExpenseStatus;
}

export interface RecurringExpensePayload {
  name: string;
  amount: number;
  categoryId: number;
  frequency: RecurringExpenseFrequency;
  dayOfWeek: DayOfWeek | null;
  dayOfMonth: number | null;
  skipWeekends: boolean;
  startDate: string;
  endDate: string | null;
}

export interface MonthlyBudget {
  id: number;
  categoryId: number;
  categoryName: string;
  monthStart: string;
  amount: number;
  actualAmount: number;
  remainingAmount: number;
  usagePercent: number;
  currency: string;
}

export interface RecurringExpenseOccurrence {
  id: number;
  ruleId: number;
  scheduledDate: string;
  status: 'CREATED' | 'SKIPPED';
  transactionId: number | null;
}
