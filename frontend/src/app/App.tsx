import { Navigate, Route, Routes } from 'react-router';
import { AuthProvider } from '../features/auth/AuthProvider';
import { RequireAuth } from '../components/RequireAuth';
import { AppLayout } from '../components/AppLayout';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { TransactionsPage } from '../features/transactions/TransactionsPage';
import { CategoriesPage } from '../features/categories/CategoriesPage';
import { SubscriptionsPage } from '../features/subscriptions/SubscriptionsPage';
import { RecurringExpensesPage } from '../features/recurring-expenses/RecurringExpensesPage';
import { RecurringIncomePage } from '../features/recurring-expenses/RecurringExpensesPage';
import { BudgetsPage } from '../features/budgets/BudgetsPage';
import { ProfilePage } from '../features/profile/ProfilePage';

export function App() {
  return <AuthProvider><Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route element={<RequireAuth />}>
      <Route element={<AppLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="transactions" element={<TransactionsPage />} />
        <Route path="budgets" element={<BudgetsPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="categories" element={<CategoriesPage />} />
        <Route path="subscriptions" element={<SubscriptionsPage />} />
        <Route path="recurring-expenses" element={<RecurringExpensesPage />} />
        <Route path="recurring-income" element={<RecurringIncomePage />} />
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes></AuthProvider>;
}
