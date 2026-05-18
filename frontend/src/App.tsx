import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/layout/AppShell';
import { useAuthStore } from '@/store/auth';
import { Role } from '@/types/domain';

const AdminPage = lazy(() => import('@/pages/AdminPage').then((module) => ({ default: module.AdminPage })));
const AnalyticsPage = lazy(() => import('@/pages/AnalyticsPage').then((module) => ({ default: module.AnalyticsPage })));
const AuditLogPage = lazy(() => import('@/pages/AuditLogPage').then((module) => ({ default: module.AuditLogPage })));
const CheckInPage = lazy(() => import('@/pages/CheckInPage').then((module) => ({ default: module.CheckInPage })));
const DashboardPage = lazy(() => import('@/pages/DashboardPage').then((module) => ({ default: module.DashboardPage })));
const GoalsPage = lazy(() => import('@/pages/GoalsPage').then((module) => ({ default: module.GoalsPage })));
const LoginPage = lazy(() => import('@/pages/LoginPage').then((module) => ({ default: module.LoginPage })));
const ManagerCheckInPage = lazy(() => import('@/pages/ManagerCheckInPage').then((module) => ({ default: module.ManagerCheckInPage })));

function Protected({ children, roles }: { children: React.ReactNode; roles?: Role[] }) {
  const { token, user } = useAuthStore();
  if (!token || !user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export function App() {
  return (
    <Suspense fallback={<div className="p-6 text-sm text-slate-500">Loading...</div>}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<Protected><AppShell /></Protected>}>
          <Route index element={<DashboardPage />} />
          <Route path="/goals" element={<GoalsPage />} />
          <Route path="/check-in" element={<CheckInPage />} />
          <Route path="/team-check-in" element={<Protected roles={['MANAGER', 'ADMIN']}><ManagerCheckInPage /></Protected>} />
          <Route path="/analytics" element={<Protected roles={['MANAGER', 'ADMIN']}><AnalyticsPage /></Protected>} />
          <Route path="/admin" element={<Protected roles={['ADMIN']}><AdminPage /></Protected>} />
          <Route path="/audit" element={<Protected roles={['ADMIN']}><AuditLogPage /></Protected>} />
        </Route>
      </Routes>
    </Suspense>
  );
}
