import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '../features/auth/AuthProvider';

export function RequireAuth() {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <div className="app-loading"><span className="spinner" /> Yoxlanılır...</div>;
  return user ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />;
}
