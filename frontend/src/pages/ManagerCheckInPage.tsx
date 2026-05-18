import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { api } from '@/api/client';
import { Button } from '@/components/ui/button';
import { Card, CardTitle } from '@/components/ui/card';
import { Select, Textarea } from '@/components/ui/input';
import { Goal, ManagerComment, Quarter, User } from '@/types/domain';
import { Progress } from '@/components/ui/progress';

export function ManagerCheckInPage() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [team, setTeam] = useState<User[]>([]);
  const [comments, setComments] = useState<ManagerComment[]>([]);
  const [employeeId, setEmployeeId] = useState<number>();
  const [quarter, setQuarter] = useState<Quarter>('Q2');
  const [comment, setComment] = useState('');

  useEffect(() => {
    Promise.all([
      api.get('/manager/team-goals'),
      api.get('/manager/team-users'),
      api.get('/manager/comments')
    ]).then(([gResponse, tResponse, cResponse]) => {
      setGoals(gResponse.data);
      setTeam(tResponse.data);
      setComments(cResponse.data);
      if (tResponse.data.length > 0) setEmployeeId(tResponse.data[0].id);
    });
  }, []);

  async function save() {
    try {
      await api.post(`/manager/comments`, { employeeId, quarter, comment });
      toast.success('Manager check-in comment saved');
      setComment('');
      const response = await api.get('/manager/comments');
      setComments(response.data);
    } catch (error: any) {
      toast.error(error.response?.data?.message ?? 'Failed to save comment');
    }
  }

  const employeeGoals = goals.filter(g => g.employeeId === employeeId);
  const selectedEmployee = useMemo(() => team.find((person) => person.id === employeeId), [employeeId, team]);
  const visibleComments = comments.filter((item) => item.employeeId === employeeId && item.quarter === quarter);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-normal">Team Check-in</h1>
        <p className="text-slate-500">Review your team's quarterly progress and add feedback.</p>
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.5fr_1fr]">
        <div className="space-y-4">
          <Card>
            <CardTitle>Team Progress</CardTitle>
            <div className="mt-4 flex gap-4">
              <Select value={employeeId} onChange={(e) => setEmployeeId(Number(e.target.value))}>
                {team.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </Select>
            </div>
            
            <div className="mt-6 space-y-4">
              {employeeGoals.map((goal) => {
                const progress = Math.round(goal.latestProgressScore ?? 0);
                return (
                  <div key={goal.id} className="rounded-lg border border-border p-4">
                    <div className="flex justify-between items-center">
                      <div className="font-semibold">{goal.title}</div>
                      <div className="text-sm font-semibold">{goal.status}</div>
                    </div>
                    <div className="text-sm text-slate-500 mt-1">
                      Planned: {goal.target} {goal.uomType} | Actual: {goal.achievement ?? 0}
                      {goal.latestQuarter ? ` | ${goal.latestQuarter}` : ''}
                    </div>
                    {goal.latestUpdateComment && <div className="mt-2 text-sm text-slate-600 dark:text-slate-300">{goal.latestUpdateComment}</div>}
                    <div className="mt-3 flex items-center gap-3">
                      <Progress value={progress} />
                      <span className="w-12 text-right text-sm font-semibold">{progress}%</span>
                    </div>
                  </div>
                );
              })}
              {employeeGoals.length === 0 && <div className="text-sm text-slate-500">No goals found for this employee.</div>}
            </div>
          </Card>
        </div>

        <div>
          <Card className="sticky top-20">
            <CardTitle>Add Feedback</CardTitle>
            <div className="mt-4 space-y-4">
              <Select value={quarter} onChange={(e) => setQuarter(e.target.value as Quarter)}>
                <option value="Q1">Q1</option>
                <option value="Q2">Q2</option>
                <option value="Q3">Q3</option>
                <option value="Q4">Q4</option>
              </Select>
              <Textarea 
                value={comment} 
                onChange={(e) => setComment(e.target.value)} 
                placeholder="Manager's feedback on the employee's progress for this quarter..." 
                className="min-h-[150px]"
              />
              <Button onClick={save} className="w-full">Save Feedback</Button>
            </div>
          </Card>
          <Card className="mt-4">
            <CardTitle>{selectedEmployee?.name ?? 'Team'} Feedback Log</CardTitle>
            <div className="mt-4 space-y-3">
              {visibleComments.map((item) => (
                <div key={item.id} className="rounded-lg border border-border p-3 text-sm">
                  <div className="font-semibold">{item.quarter}</div>
                  <div className="mt-1 text-slate-600 dark:text-slate-300">{item.comment}</div>
                  <div className="mt-1 text-xs text-slate-400">{new Date(item.createdAt).toLocaleString()}</div>
                </div>
              ))}
              {visibleComments.length === 0 && <div className="text-sm text-slate-500">No feedback saved for this quarter yet.</div>}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
