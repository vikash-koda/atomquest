import { cn } from '@/lib/utils';
import { GoalStatus } from '@/types/domain';

const statusTone: Record<GoalStatus, string> = {
  DRAFT: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200',
  SUBMITTED: 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200',
  APPROVED: 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-200',
  REJECTED: 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200',
  NOT_STARTED: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200',
  ON_TRACK: 'bg-teal-100 text-teal-800 dark:bg-teal-900/40 dark:text-teal-200',
  COMPLETED: 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-200',
};

export function Badge({ children, status }: { children: React.ReactNode; status?: GoalStatus }) {
  return <span className={cn('inline-flex rounded-full px-2.5 py-1 text-xs font-semibold', status ? statusTone[status] : 'bg-muted text-foreground')}>{children}</span>;
}
