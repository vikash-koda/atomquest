import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { api } from '@/api/client';
import { Button } from '@/components/ui/button';
import { Card, CardTitle } from '@/components/ui/card';
import { Input, Select, Textarea } from '@/components/ui/input';
import { Goal, Quarter, ProgressStatus } from '@/types/domain';

export function CheckInPage() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [goalId, setGoalId] = useState<number>();
  const [quarter, setQuarter] = useState<Quarter>('Q2');
  const [achievement, setAchievement] = useState(0);
  const [comment, setComment] = useState('');
  const [progressStatus, setProgressStatus] = useState<ProgressStatus>('ON_TRACK');
  const [completionDate, setCompletionDate] = useState('');

  const selectedGoal = goals.find((g) => g.id === goalId);

  useEffect(() => {
    api.get('/goals').then((r) => {
      setGoals(r.data);
      setGoalId(r.data[0]?.id);
    });
  }, []);

  async function save() {
    try {
      const payload: any = { 
        quarter,
        achievement,
        comment,
        progressStatus,
      };
      if (selectedGoal?.uomType === 'TIMELINE' && completionDate) {
        payload.completionDate = completionDate;
      }
      await api.post(`/goals/${goalId}/quarterly-updates`, payload);
      toast.success('Quarterly check-in saved');
      const response = await api.get('/goals');
      setGoals(response.data);
    } catch (error: any) {
      toast.error(error.response?.data?.message ?? 'Check-in failed');
    }
  }

  return (
    <Card className="max-w-2xl">
      <CardTitle>Quarterly Check-in</CardTitle>
      <div className="mt-4 space-y-4">
        <Select value={goalId} onChange={(e) => setGoalId(Number(e.target.value))}>
          {goals.map((goal) => <option key={goal.id} value={goal.id}>{goal.title}</option>)}
        </Select>
        <Select value={quarter} onChange={(e) => setQuarter(e.target.value as Quarter)}>
          <option value="Q1">Q1</option>
          <option value="Q2">Q2</option>
          <option value="Q3">Q3</option>
          <option value="Q4">Q4</option>
        </Select>
        <Select value={progressStatus} onChange={(e) => setProgressStatus(e.target.value as ProgressStatus)}>
          <option value="NOT_STARTED">Not Started</option>
          <option value="ON_TRACK">On Track</option>
          <option value="COMPLETED">Completed</option>
        </Select>
        
        {selectedGoal && (
          <div className="rounded-lg border border-border p-3 text-sm text-slate-600 dark:text-slate-300">
            <div>Planned target: <strong>{selectedGoal.target}</strong> {selectedGoal.uomType} {selectedGoal.uomType === 'TIMELINE' ? `(Deadline: ${selectedGoal.deadline})` : ''}</div>
            <div>Latest actual: <strong>{selectedGoal.achievement ?? 0}</strong></div>
            <div>Computed progress score: <strong>{Math.round(selectedGoal.latestProgressScore ?? 0)}%</strong></div>
          </div>
        )}

        <Input type="number" value={achievement} onChange={(e) => setAchievement(Number(e.target.value))} placeholder={selectedGoal?.uomType === 'TIMELINE' ? "Days spent/achievement" : "Achievement"} />
        
        {selectedGoal?.uomType === 'TIMELINE' && (
          <Input type="date" value={completionDate} onChange={(e) => setCompletionDate(e.target.value)} placeholder="Completion Date" />
        )}

        <Textarea value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Achievement notes" />
        <Button onClick={save}>Save Check-in</Button>
      </div>
    </Card>
  );
}
