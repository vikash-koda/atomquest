import { useEffect, useState } from 'react';
import { api } from '@/api/client';
import { AnalyticsCharts } from '@/components/charts/AnalyticsCharts';
import { Card, CardTitle } from '@/components/ui/card';
import { Analytics } from '@/types/domain';

export function AnalyticsPage() {
  const [data, setData] = useState<Analytics>();

  useEffect(() => {
    api.get('/analytics/overview').then((r) => setData(r.data));
  }, []);

  if (!data) return <Card>Loading analytics...</Card>;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-normal">Analytics</h1>
        <p className="text-slate-500">Completion, distribution, quarterly trend, and department progress.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        <Metric label="Approved Goals" value={data.approvedGoals} />
        <Metric label="Overall Completion Rate" value={`${data.completionRate}%`} />
        <Metric label="Average Progress" value={`${data.averageProgress}%`} />
      </div>
      <AnalyticsCharts data={data} />
      <Card>
        <CardTitle>Check-in Completion ({data.checkInCompletion.quarter})</CardTitle>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <Metric label="Employee Achievement Capture" value={`${data.checkInCompletion.achievementCompletionRate}%`} />
          <Metric label="Manager Check-ins Complete" value={`${data.checkInCompletion.managerCheckInCompletionRate}%`} />
        </div>
        <div className="mt-4 overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="text-slate-500">
              <tr><th className="py-2">Employee</th><th>Manager</th><th>Department</th><th>Achievement</th><th>Manager Check-in</th></tr>
            </thead>
            <tbody>
              {data.checkInCompletion.employeeRows.map((row) => (
                <tr key={row.employeeId} className="border-t border-border">
                  <td className="py-2 font-semibold">{row.employeeName}</td>
                  <td>{row.managerName || 'Unassigned'}</td>
                  <td>{row.department}</td>
                  <td className={row.achievementDone ? 'text-emerald-600' : 'text-amber-600'}>{row.achievementDone ? 'Done' : 'Pending'}</td>
                  <td className={row.managerCheckInDone ? 'text-emerald-600' : 'text-amber-600'}>{row.managerCheckInDone ? 'Done' : 'Pending'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
      <Card>
        <CardTitle>Manager Check-in Effectiveness</CardTitle>
        <div className="mt-4 grid gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
          {Object.entries(data.managerEffectiveness).length === 0 ? (
            <div className="text-sm text-slate-500">No managers found.</div>
          ) : (
            Object.entries(data.managerEffectiveness).map(([name, score]) => (
              <Metric key={name} label={name} value={`${Math.round(score)}%`} />
            ))
          )}
        </div>
      </Card>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: React.ReactNode }) {
  return <div className="rounded-lg border border-border p-4"><div className="text-sm text-slate-500">{label}</div><div className="mt-1 text-2xl font-bold">{value}</div></div>;
}
