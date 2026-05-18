import { Goal } from '@/types/domain';

export const MAX_GOALS = 8;
export const MIN_WEIGHTAGE = 10;
export const MAX_WEIGHTAGE = 100;
export const REQUIRED_TOTAL_WEIGHTAGE = 100;

export function totalWeightage(goals: Goal[]) {
  return goals.reduce((sum, goal) => sum + goal.weightage, 0);
}

export function canSubmitSheet(goals: Goal[]) {
  if (goals.length === 0) {
    return { ok: false, message: 'Create at least one goal before submission.' };
  }
  if (goals.some((goal) => goal.status === 'SUBMITTED')) {
    return { ok: false, message: 'Goals are already pending manager approval.' };
  }
  const submittable = goals.filter((goal) => goal.status === 'DRAFT' || goal.status === 'REJECTED');
  if (submittable.length === 0) {
    return { ok: false, message: 'No draft or rejected goals available to submit.' };
  }
  const total = totalWeightage(goals);
  if (total !== REQUIRED_TOTAL_WEIGHTAGE) {
    return { ok: false, message: `Total weightage must equal ${REQUIRED_TOTAL_WEIGHTAGE}% (current: ${total}%).` };
  }
  return { ok: true, message: '' };
}

export function weightageByEmployee(goals: Goal[]) {
  return goals.reduce<Record<number, { name: string; total: number }>>((acc, goal) => {
    const current = acc[goal.employeeId] ?? { name: goal.employeeName, total: 0 };
    current.total += goal.weightage;
    acc[goal.employeeId] = current;
    return acc;
  }, {});
}

export function canCreateGoal(goalCount: number) {
  return goalCount < MAX_GOALS;
}
