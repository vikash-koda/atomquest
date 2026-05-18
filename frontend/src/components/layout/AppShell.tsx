import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { BarChart3, CalendarCheck, ClipboardCheck, LayoutDashboard, LogOut, Moon, ShieldCheck, Sun, Users } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/store/auth';
import { useEffect, useState } from 'react';
import { cn } from '@/lib/utils';
import { Role } from '@/types/domain';

const nav: Array<{ to: string; label: string; icon: React.ElementType; roles: Role[] }> = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
  { to: '/goals', label: 'Goals', icon: ClipboardCheck, roles: ['EMPLOYEE', 'MANAGER', 'ADMIN'] },
  { to: '/check-in', label: 'Check-in', icon: CalendarCheck, roles: ['EMPLOYEE'] },
  { to: '/team-check-in', label: 'Team Check-in', icon: CalendarCheck, roles: ['MANAGER', 'ADMIN'] },
  { to: '/analytics', label: 'Analytics', icon: BarChart3, roles: ['MANAGER', 'ADMIN'] },
  { to: '/admin', label: 'Admin', icon: Users, roles: ['ADMIN'] },
  { to: '/audit', label: 'Audit', icon: ShieldCheck, roles: ['ADMIN'] },
];

export function AppShell() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [dark, setDark] = useState(false);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark);
  }, [dark]);

  return (
    <div className="min-h-screen">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-border bg-white/80 p-4 backdrop-blur-xl dark:bg-slate-950/80 lg:block">
        <div className="mb-8 flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-lg bg-primary text-lg font-black text-white">AQ</div>
          <div>
            <div className="font-bold">ATOMQUEST</div>
            <div className="text-xs text-slate-500">Goal Tracking Portal</div>
          </div>
        </div>
        <nav className="space-y-1">
          {nav
            .filter((item) => user && item.roles.includes(user.role))
            .map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn('flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition', isActive ? 'bg-primary text-white' : 'hover:bg-muted')
                }
              >
                <item.icon size={18} />
                {item.label}
              </NavLink>
            ))}
        </nav>
      </aside>
      <main className="lg:pl-64">
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-background/82 px-4 backdrop-blur-xl lg:px-8">
          <div>
            <div className="text-sm text-slate-500">{user?.department}</div>
            <div className="font-semibold">{user?.name} · {user?.role}</div>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="secondary" onClick={() => setDark((v) => !v)}>{dark ? <Sun size={16} /> : <Moon size={16} />}</Button>
            <Button
              variant="secondary"
              onClick={() => {
                logout();
                navigate('/login');
              }}
            >
              <LogOut size={16} />
              Logout
            </Button>
          </div>
        </header>
        <div className="p-4 lg:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
