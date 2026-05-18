import { useCallback, useEffect, useMemo, useState } from 'react';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Check, Loader2, Lock, Pencil, Plus, Send, Sparkles, X } from 'lucide-react';
import { api } from '@/api/client';
import { GoalValidationBanner } from '@/components/goals/GoalValidationBanner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardTitle } from '@/components/ui/card';
import { Input, Select, Textarea } from '@/components/ui/input';
import { Progress } from '@/components/ui/progress';
import { canCreateGoal, canSubmitSheet, MAX_GOALS, MIN_WEIGHTAGE } from '@/lib/goalRules';
import { Goal, ManagerComment, SharedGoal, User } from '@/types/domain';
import { useAuthStore } from '@/store/auth';

const goalSchema = z.object({
  title: z.string().min(4, 'Title must be at least 4 characters'),
  description: z.string().optional(),
  thrustArea: z.string().min(2, 'Thrust area is required'),
  uomType: z.string(),
  target: z.coerce.number().positive('Target must be greater than 0'),
  weightage: z.coerce.number().min(MIN_WEIGHTAGE, `Minimum weightage is ${MIN_WEIGHTAGE}%`).max(100),
  deadline: z.string().min(1, 'Deadline is required'),
  sharedGoalId: z.string().optional(),
});

type GoalForm = z.infer<typeof goalSchema>;

const THRUST_AREAS = [
  'Product Delivery',
  'Execution Excellence',
  'Operational Excellence',
  'Customer Success',
  'People Development',
  'Efficiency',
  'Department KPI',
];

const emptyForm: GoalForm = {
  title: '',
  description: '',
  thrustArea: '',
  uomType: 'PERCENT',
  target: 100,
  weightage: 10,
  deadline: new Date(Date.now() + 86400000 * 90).toISOString().slice(0, 10),
  sharedGoalId: '',
};

function goalsEndpoint(role?: string) {
  if (role === 'MANAGER') return '/manager/team-goals';
  if (role === 'ADMIN') return '/admin/goals';
  return '/goals';
}

export function GoalsPage() {
  const user = useAuthStore((s) => s.user);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [sharedGoals, setSharedGoals] = useState<SharedGoal[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [managerComments, setManagerComments] = useState<ManagerComment[]>([]);
  const [selectedSharedGoalId, setSelectedSharedGoalId] = useState('');
  const [editingGoal, setEditingGoal] = useState<Goal | null>(null);
  const [reviewEdits, setReviewEdits] = useState<Record<number, { target: number; weightage: number }>>({});
  const [rejectNote, setRejectNote] = useState('');
  const [rejectingGoalId, setRejectingGoalId] = useState<number | null>(null);
  const [pageLoading, setPageLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [pushForm, setPushForm] = useState({
    title: '',
    description: '',
    thrustArea: 'Department KPI',
    uomType: 'PERCENT',
    target: 100,
    defaultWeightage: 10,
    ownerId: '',
    employeeIds: [] as number[],
    deadline: new Date(Date.now() + 86400000 * 60).toISOString().slice(0, 10),
  });

  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<GoalForm>({
    resolver: zodResolver(goalSchema),
    defaultValues: emptyForm,
  });

  const myGoals = useMemo(
    () => (user?.role === 'EMPLOYEE' ? goals.filter((g) => g.employeeId === user.id) : goals),
    [goals, user],
  );
  const employeeGoalCount = myGoals.length;
  const canAddGoal = user?.role === 'EMPLOYEE' && canCreateGoal(employeeGoalCount);
  const submitCheck = canSubmitSheet(myGoals);
  const canReview = user?.role === 'MANAGER' || user?.role === 'ADMIN';

  const load = useCallback(async () => {
    if (!user) return;
    setPageLoading(true);
    try {
      const endpoint = goalsEndpoint(user.role);
      const goalResponse = await api.get(endpoint);
      const nextGoals: Goal[] = goalResponse.data;
      setGoals(nextGoals);
      const edits: Record<number, { target: number; weightage: number }> = {};
      nextGoals.forEach((goal) => {
        edits[goal.id] = { target: goal.target, weightage: goal.weightage };
      });
      setReviewEdits(edits);

      const dept = user.department ?? '';
      if (dept) {
        const sharedResponse = await api.get(`/shared-goals?department=${encodeURIComponent(dept)}`);
        setSharedGoals(sharedResponse.data);
      } else {
        setSharedGoals([]);
      }

      if (user.role === 'ADMIN') {
        const usersResponse = await api.get('/admin/users');
        setUsers(usersResponse.data);
      } else if (user.role === 'MANAGER') {
        const usersResponse = await api.get('/manager/team-users');
        setUsers(usersResponse.data);
      }

      if (user.role === 'EMPLOYEE') {
        const commentsResponse = await api.get('/goals/manager-comments');
        setManagerComments(commentsResponse.data);
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message ?? 'Failed to load goals');
      setGoals([]);
    } finally {
      setPageLoading(false);
    }
  }, [user]);

  useEffect(() => {
    load();
  }, [load]);

  function resetGoalForm() {
    reset(emptyForm);
    setSelectedSharedGoalId('');
    setEditingGoal(null);
  }

  function startEdit(goal: Goal) {
    setEditingGoal(goal);
    setSelectedSharedGoalId(goal.sharedGoalId ? String(goal.sharedGoalId) : '');
    reset({
      title: goal.title,
      description: goal.description ?? '',
      thrustArea: goal.thrustArea,
      uomType: goal.uomType,
      target: goal.target,
      weightage: goal.weightage,
      deadline: goal.deadline,
      sharedGoalId: goal.sharedGoalId ? String(goal.sharedGoalId) : '',
    });
  }

  async function saveGoal(values: GoalForm) {
    if (!editingGoal && !canCreateGoal(employeeGoalCount)) {
      toast.error(`Maximum ${MAX_GOALS} goals allowed per employee`);
      return;
    }
    setActionLoading(true);
    try {
      if (editingGoal) {
        const payload = editingGoal.sharedGoalId
          ? { weightage: values.weightage }
          : {
              title: values.title,
              description: values.description,
              thrustArea: values.thrustArea,
              uomType: values.uomType,
              target: values.target,
              weightage: values.weightage,
              deadline: values.deadline,
              sharedGoalId: values.sharedGoalId ? Number(values.sharedGoalId) : null,
            };
        await api.patch(`/goals/${editingGoal.id}`, payload);
        toast.success('Goal updated');
      } else {
        await api.post('/goals', {
          ...values,
          sharedGoalId: values.sharedGoalId ? Number(values.sharedGoalId) : null,
        });
        toast.success('Goal created');
      }
      resetGoalForm();
      await load();
    } catch (error: any) {
      toast.error(error.response?.data?.message ?? (editingGoal ? 'Could not update goal' : 'Could not create goal'));
    } finally {
      setActionLoading(false);
    }
  }

  async function submitGoals() {
    if (!submitCheck.ok) {
      toast.error(submitCheck.message);
      return;
    }
    setActionLoading(true);
    try {
      await api.post('/goals/submit');
      toast.success('Goal sheet submitted for manager approval');
      await load();
    } catch (error: any) {
      toast.error(error.response?.data?.message ?? 'Submission failed');
    } finally {
      setActionLoading(false);
    }
  }

  async function review(id: number, decision: 'APPROVED' | 'REJECTED', note?: string) {
    const edit = reviewEdits[id];
    setActionLoading(true);
    try {
      await api.post(`/manager/goals/${id}/review`, {
        decision,
        target: edit?.target,
        weightage: edit?.weightage,
        managerNote: note ?? (decision === 'APPROVED'
          ? 'Approved for quarterly tracking.'
          : 'Returned for rework. Please revise and resubmit.'),
      });
      toast.success(decision === 'APPROVED' ? 'Goal approved and locked' : 'Goal returned for rework');
      setRejectingGoalId(null);
      setRejectNote('');
      await load();
    } catch (error: any) {
      toast.error(error.response?.data?.message ?? 'Review action failed');
    } finally {
      setActionLoading(false);
    }
  }

  async function pushSharedGoal() {
    if (!pushForm.ownerId) {
      toast.error('Select a primary owner');
      return;
    }
    if (!user?.department) {
      toast.error('Your profile must have a department to push shared goals');
      return;
    }
    if (pushForm.defaultWeightage < MIN_WEIGHTAGE) {
      toast.error(`Minimum weightage is ${MIN_WEIGHTAGE}%`);
      return;
    }
    setActionLoading(true);
    try {
      const url = user.role === 'ADMIN' ? '/admin/shared-goals/push' : '/manager/shared-goals/push';
      await api.post(url, {
        ...pushForm,
        department: user.department,
        ownerId: Number(pushForm.ownerId),
        employeeIds: pushForm.employeeIds,
      });
      toast.success('Shared departmental KPI pushed');
      await load();
    } catch (error: any) {
      toast.error(error.response?.data?.message ?? 'Could not push shared goal');
    } finally {
      setActionLoading(false);
    }
  }

  const selectedSharedGoal = sharedGoals.find((goal) => String(goal.id) === selectedSharedGoalId);
  const isSharedGoalForm = !!selectedSharedGoalId;
  const pushRecipients = user?.role === 'ADMIN'
    ? users.filter((item) => item.role === 'EMPLOYEE' && item.department === user.department)
    : users.filter((item) => item.role === 'EMPLOYEE');

  const pageTitle = user?.role === 'MANAGER' ? 'Goal Approval' : user?.role === 'ADMIN' ? 'Organization Goals' : 'Goal Management';

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold tracking-normal">{pageTitle}</h1>
          <p className="text-slate-500">
            Maximum {MAX_GOALS} goals, minimum {MIN_WEIGHTAGE}% weightage per goal, total must equal 100% before submission.
          </p>
        </div>
        {user?.role === 'EMPLOYEE' && (
          <Button onClick={submitGoals} disabled={actionLoading || !submitCheck.ok}>
            {actionLoading ? <Loader2 className="animate-spin" size={16} /> : <Send size={16} />}
            Submit Sheet
          </Button>
        )}
      </div>

      {user?.role === 'EMPLOYEE' && managerComments.length > 0 && (
        <Card>
          <CardTitle>Manager Feedback</CardTitle>
          <div className="mt-3 space-y-2">
            {managerComments.slice(0, 3).map((comment) => (
              <div key={comment.id} className="rounded-lg border border-border p-3 text-sm">
                <div className="font-semibold">{comment.managerName}</div>
                <p className="mt-1 text-slate-600 dark:text-slate-300">{comment.comment}</p>
                <p className="mt-1 text-xs text-slate-400">{new Date(comment.createdAt).toLocaleString()}</p>
              </div>
            ))}
          </div>
        </Card>
      )}

      <div className="grid gap-4 xl:grid-cols-[minmax(360px,420px)_minmax(0,1fr)]">
        {user?.role === 'EMPLOYEE' && (
          <Card>
            <div className="flex items-center justify-between gap-3">
              <CardTitle className="flex items-center gap-2">
                {editingGoal ? <Pencil size={18} /> : <Plus size={18} />}
                {editingGoal ? 'Edit Goal' : 'Create Goal'}
                <span className="text-sm font-normal text-slate-500">({employeeGoalCount}/{MAX_GOALS})</span>
              </CardTitle>
              {editingGoal && (
                <Button type="button" variant="ghost" className="h-8 px-2" onClick={resetGoalForm}>
                  <X size={16} />
                  Cancel
                </Button>
              )}
            </div>
            {!canAddGoal && !editingGoal && (
              <p className="mt-2 text-sm text-amber-600">You have reached the maximum of {MAX_GOALS} goals.</p>
            )}
            <form onSubmit={handleSubmit(saveGoal)} className="mt-4 space-y-3">
              <Field label="Goal type" error={errors.sharedGoalId?.message}>
                <Select
                  {...register('sharedGoalId')}
                  value={selectedSharedGoalId}
                  disabled={!!editingGoal?.sharedGoalId || (!canAddGoal && !editingGoal)}
                  onChange={(event) => {
                    const value = event.target.value;
                    const shared = sharedGoals.find((goal) => String(goal.id) === value);
                    setSelectedSharedGoalId(value);
                    setValue('sharedGoalId', value);
                    if (shared) {
                      setValue('title', shared.title);
                      setValue('description', shared.description ?? '');
                      setValue('thrustArea', shared.thrustArea);
                      setValue('uomType', shared.uomType);
                      setValue('target', shared.target);
                      setValue('deadline', shared.deadline);
                    }
                  }}
                >
                  <option value="">Individual goal</option>
                  {sharedGoals.map((goal) => <option key={goal.id} value={goal.id}>{goal.title}</option>)}
                </Select>
              </Field>
              <Field label="Goal title" error={errors.title?.message}>
                <Input
                  placeholder="e.g. Improve team delivery predictability"
                  readOnly={isSharedGoalForm}
                  value={isSharedGoalForm ? (selectedSharedGoal?.title ?? editingGoal?.title ?? '') : undefined}
                  {...(!isSharedGoalForm ? register('title') : {})}
                />
              </Field>
              <Field label="Description" error={errors.description?.message}>
                <Textarea placeholder="Describe outcome and success criteria" readOnly={isSharedGoalForm} {...register('description')} />
              </Field>
              <Field label="Thrust area" error={errors.thrustArea?.message}>
                <Select disabled={isSharedGoalForm} {...register('thrustArea')}>
                  <option value="">Select thrust area</option>
                  {THRUST_AREAS.map((area) => <option key={area} value={area}>{area}</option>)}
                </Select>
              </Field>
              <div className="grid grid-cols-2 gap-3">
                <Field label="UoM" error={errors.uomType?.message}>
                  <Select disabled={isSharedGoalForm} {...register('uomType')}>
                    <option value="NUMERIC">Numeric</option>
                    <option value="PERCENT">Percent</option>
                    <option value="MAX">Max (lower is better)</option>
                    <option value="ZERO">Zero-based</option>
                    <option value="TIMELINE">Timeline</option>
                  </Select>
                </Field>
                <Field label="Target" error={errors.target?.message}>
                  <Input
                    type="number"
                    readOnly={isSharedGoalForm}
                    value={isSharedGoalForm ? (selectedSharedGoal?.target ?? editingGoal?.target ?? '') : undefined}
                    {...(!isSharedGoalForm ? register('target') : {})}
                  />
                </Field>
                <Field label="Weightage (%)" error={errors.weightage?.message}>
                  <Input type="number" {...register('weightage')} disabled={!canAddGoal && !editingGoal} />
                </Field>
                <Field label="Deadline" error={errors.deadline?.message}>
                  <Input type="date" readOnly={isSharedGoalForm} {...register('deadline')} />
                </Field>
              </div>
              <Button disabled={actionLoading || (!canAddGoal && !editingGoal)} className="w-full">
                {actionLoading ? <Loader2 className="animate-spin" size={16} /> : <Sparkles size={16} />}
                {editingGoal ? 'Update Goal' : 'Save Goal'}
              </Button>
            </form>
          </Card>
        )}

        {canReview && (
          <Card>
            <CardTitle>Push Shared Department KPI</CardTitle>
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <Input placeholder="Goal title" value={pushForm.title} onChange={(e) => setPushForm({ ...pushForm, title: e.target.value })} />
              <Select value={pushForm.thrustArea} onChange={(e) => setPushForm({ ...pushForm, thrustArea: e.target.value })}>
                {THRUST_AREAS.map((area) => <option key={area} value={area}>{area}</option>)}
              </Select>
              <Select value={pushForm.uomType} onChange={(e) => setPushForm({ ...pushForm, uomType: e.target.value })}>
                <option value="NUMERIC">Numeric</option>
                <option value="PERCENT">Percent</option>
                <option value="MAX">Max</option>
                <option value="ZERO">Zero-based</option>
                <option value="TIMELINE">Timeline</option>
              </Select>
              <Input type="number" placeholder="Target" value={pushForm.target} onChange={(e) => setPushForm({ ...pushForm, target: Number(e.target.value) })} />
              <Input type="number" placeholder={`Weightage (min ${MIN_WEIGHTAGE})`} value={pushForm.defaultWeightage} onChange={(e) => setPushForm({ ...pushForm, defaultWeightage: Number(e.target.value) })} />
              <Input type="date" value={pushForm.deadline} onChange={(e) => setPushForm({ ...pushForm, deadline: e.target.value })} />
              <Select value={pushForm.ownerId} onChange={(e) => setPushForm({ ...pushForm, ownerId: e.target.value })}>
                <option value="">Primary owner *</option>
                {pushRecipients.map((person) => <option key={person.id} value={person.id}>{person.name}</option>)}
              </Select>
              <Input placeholder="Description" value={pushForm.description} onChange={(e) => setPushForm({ ...pushForm, description: e.target.value })} />
              <div className="md:col-span-2 grid gap-2 rounded-lg border border-border p-3">
                <div className="text-sm font-semibold">Recipients</div>
                <p className="text-xs text-slate-500">Leave blank to push to all eligible employees in this department.</p>
                <div className="grid gap-2 md:grid-cols-2">
                  {pushRecipients.map((person) => (
                    <label key={person.id} className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={pushForm.employeeIds.includes(person.id)}
                        onChange={(event) => setPushForm({
                          ...pushForm,
                          employeeIds: event.target.checked
                            ? [...pushForm.employeeIds, person.id]
                            : pushForm.employeeIds.filter((id) => id !== person.id),
                        })}
                      />
                      {person.name}
                    </label>
                  ))}
                  {pushRecipients.length === 0 && (
                    <div className="text-sm text-slate-500">No eligible employees found.</div>
                  )}
                </div>
              </div>
              <Button className="md:col-span-2" disabled={actionLoading} onClick={pushSharedGoal}>
                {actionLoading ? <Loader2 className="animate-spin" size={16} /> : null}
                Push KPI
              </Button>
            </div>
          </Card>
        )}

        <Card className={user?.role === 'EMPLOYEE' ? 'min-w-0' : 'xl:col-span-2'}>
          <div className="space-y-3">
            <CardTitle>Goal Sheet</CardTitle>
            {user?.role === 'EMPLOYEE' && <GoalValidationBanner goals={myGoals} mode="employee" />}
            {canReview && <GoalValidationBanner goals={goals} mode="manager" />}
          </div>

          {pageLoading ? (
            <div className="flex items-center justify-center py-16 text-slate-500">
              <Loader2 className="mr-2 animate-spin" size={20} />
              Loading goals...
            </div>
          ) : goals.length === 0 ? (
            <p className="py-12 text-center text-slate-500">No goals found. {user?.role === 'EMPLOYEE' ? 'Create your first goal to get started.' : 'Team goals will appear here once employees submit.'}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead className="text-slate-500">
                  <tr>
                    <th className="py-3">Goal</th>
                    <th>Owner</th>
                    <th>Status</th>
                    <th>Weight</th>
                    <th>Target</th>
                    <th>Progress</th>
                    <th>Deadline</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {goals.map((goal) => {
                    const progress = Math.round(goal.latestProgressScore ?? 0);
                    const isReviewer = canReview && goal.status === 'SUBMITTED';
                    return (
                      <tr key={goal.id} className="border-t border-border">
                        <td className="py-3">
                          <div className="font-semibold">{goal.title}</div>
                          <div className="text-xs text-slate-500">
                            {goal.thrustArea}
                            {goal.sharedGoalId && ' · Shared'}
                            {goal.locked && <Lock className="ml-1 inline" size={12} />}
                          </div>
                        </td>
                        <td>{goal.employeeName}</td>
                        <td><Badge status={goal.status}>{goal.status.replace('_', ' ')}</Badge></td>
                        <td>
                          {isReviewer ? (
                            <Input
                              type="number"
                              className="w-20"
                              value={reviewEdits[goal.id]?.weightage ?? goal.weightage}
                              onChange={(e) => setReviewEdits({
                                ...reviewEdits,
                                [goal.id]: { target: reviewEdits[goal.id]?.target ?? goal.target, weightage: Number(e.target.value) },
                              })}
                            />
                          ) : `${goal.weightage}%`}
                        </td>
                        <td>
                          {isReviewer ? (
                            <Input
                              type="number"
                              className="w-24"
                              disabled={!!goal.sharedGoalId}
                              value={reviewEdits[goal.id]?.target ?? goal.target}
                              onChange={(e) => setReviewEdits({
                                ...reviewEdits,
                                [goal.id]: { target: Number(e.target.value), weightage: reviewEdits[goal.id]?.weightage ?? goal.weightage },
                              })}
                            />
                          ) : goal.target}
                        </td>
                        <td>
                          <div className="flex min-w-32 items-center gap-2"><Progress value={progress} /><span>{progress}%</span></div>
                          {goal.latestQuarter && (
                            <div className="mt-1 text-xs text-slate-500">{goal.latestQuarter} actual: {goal.achievement ?? 0}</div>
                          )}
                        </td>
                        <td>{goal.deadline}</td>
                        <td>
                          {user?.role === 'EMPLOYEE' && goal.employeeId === user.id && !goal.locked && (goal.status === 'DRAFT' || goal.status === 'REJECTED') ? (
                            <Button variant="secondary" className="h-8 px-3" onClick={() => startEdit(goal)}>
                              <Pencil size={14} />
                              Edit
                            </Button>
                          ) : isReviewer ? (
                            <div className="flex flex-col gap-2">
                              <div className="flex gap-2">
                                <Button className="h-8 px-3" disabled={actionLoading} onClick={() => review(goal.id, 'APPROVED')}>
                                  <Check size={14} />
                                </Button>
                                <Button variant="danger" className="h-8 px-3" disabled={actionLoading} onClick={() => setRejectingGoalId(goal.id)}>
                                  <X size={14} />
                                </Button>
                              </div>
                              {rejectingGoalId === goal.id && (
                                <div className="flex min-w-48 flex-col gap-1">
                                  <Input
                                    placeholder="Rejection reason"
                                    value={rejectNote}
                                    onChange={(e) => setRejectNote(e.target.value)}
                                  />
                                  <Button variant="danger" className="h-8" disabled={actionLoading} onClick={() => review(goal.id, 'REJECTED', rejectNote || undefined)}>
                                    Confirm reject
                                  </Button>
                                </div>
                              )}
                            </div>
                          ) : goal.status === 'APPROVED' ? (
                            <span className="text-emerald-600">Locked</span>
                          ) : goal.status === 'REJECTED' ? (
                            <span className="text-amber-600">Rework</span>
                          ) : (
                            <span className="text-slate-400">—</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>

    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="mb-1 block text-sm font-medium text-slate-600 dark:text-slate-300">{label}</label>
      {children}
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  );
}


