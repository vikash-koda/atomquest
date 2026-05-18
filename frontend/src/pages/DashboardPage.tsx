import { useEffect, useState } from 'react';
import { Activity, CheckCircle2, ClipboardList, Lightbulb, Loader2, TrendingUp } from 'lucide-react';
import { api } from '@/api/client';
import { Card, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Analytics, Goal } from '@/types/domain';
import { useAuthStore } from '@/store/auth';
import { Badge } from '@/components/ui/badge';

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [analytics, setAnalytics] = useState<Analytics>();
  const [goals, setGoals] = useState<Goal[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      user?.role === 'EMPLOYEE' ? Promise.resolve() : api.get('/analytics/overview').then((r) => setAnalytics(r.data)).catch(() => {}),
      api.get(user?.role === 'MANAGER' ? '/manager/team-goals' : '/goals').then((r) => setGoals(r.data)).catch(() => setGoals([]))
    ]).finally(() => setLoading(false));
  }, [user?.role]);

  const suggestions = [
    'Increase delivery predictability to 90% sprint commitment reliability.',
    'Reduce repeated production issues by 30% through preventive reviews.',
    'Publish one reusable knowledge asset every quarter for the department.',
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-normal">{user?.role === 'ADMIN' ? 'Organization Dashboard' : user?.role === 'MANAGER' ? 'Team Dashboard' : 'Employee Dashboard'}</h1>
        <p className="text-slate-500">Quarterly performance, approvals, insights, and progress in one place.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-4">
        <Stat icon={ClipboardList} label="Total Goals" value={analytics?.totalGoals ?? 0} />
        <Stat icon={CheckCircle2} label="Completed" value={analytics?.completedGoals ?? 0} />
        <Stat icon={TrendingUp} label="Completion" value={`${analytics?.completionRate ?? 0}%`} />
        <Stat icon={Activity} label="Employees" value={analytics?.employeeCount ?? 0} />
      </div>
      <div className="grid gap-4 xl:grid-cols-[1.3fr_.7fr]">
        <Card>
          <CardTitle>Active Goal Portfolio</CardTitle>
          <div className="mt-4 space-y-4">
            {loading ? (
              <div className="flex py-8 justify-center items-center text-slate-500"><Loader2 className="animate-spin mr-2" size={20} /> Loading...</div>
            ) : goals.length === 0 ? (
              <div className="py-8 text-center text-sm text-slate-500">No active goals found.</div>
            ) : goals.slice(0, 5).map((goal) => {
              const progress = Math.round(goal.latestProgressScore ?? 0);
              return (
                <div key={goal.id} className="rounded-lg border border-border p-4">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <div className="font-semibold">{goal.title}</div>
                      <div className="text-sm text-slate-500">{goal.employeeName} · {goal.weightage}% · {goal.thrustArea}</div>
                    </div>
                    <Badge status={goal.status}>{goal.status.replace('_', ' ')}</Badge>
                  </div>
                  <div className="mt-3 flex items-center gap-3">
                    <Progress value={progress} />
                    <span className="w-12 text-right text-sm font-semibold">{progress}%</span>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
        <Card>
          <CardTitle>Smart Insights</CardTitle>
          <div className="mt-4 space-y-3">
            {(analytics?.lowPerformingGoals ?? []).map((goal) => (
              <div key={goal.id} className="rounded-lg bg-red-50 p-3 text-sm text-red-900 dark:bg-red-950/40 dark:text-red-200">
                {goal.title} is at {goal.score}% for {goal.employee}.
              </div>
            ))}
            <div className="rounded-lg bg-teal-50 p-3 text-sm text-teal-900 dark:bg-teal-950/40 dark:text-teal-100">
              {analytics?.approvedGoals ?? 0} goals are manager-approved and locked for tracking.
            </div>
          </div>
          <CardTitle className="mt-6 flex items-center gap-2"><Lightbulb size={18} /> AI Goal Suggestions</CardTitle>
          <div className="mt-3 space-y-2">
            {suggestions.map((item) => <div key={item} className="rounded-md border border-border p-3 text-sm">{item}</div>)}
          </div>
        </Card>
      </div>
    </div>
  );
}

function Stat({ icon: Icon, label, value }: { icon: React.ElementType; label: string; value: React.ReactNode }) {
  return (
    <Card>
      <div className="flex items-center justify-between">
        <div>
          <div className="text-sm text-slate-500">{label}</div>
          <div className="mt-1 text-3xl font-bold">{value}</div>
        </div>
        <div className="grid h-11 w-11 place-items-center rounded-lg bg-primary/10 text-primary"><Icon size={22} /></div>
      </div>
    </Card>
  );
}
