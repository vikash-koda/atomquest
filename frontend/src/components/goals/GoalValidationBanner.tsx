import { Goal } from '@/types/domain';
import { REQUIRED_TOTAL_WEIGHTAGE, totalWeightage, weightageByEmployee } from '@/lib/goalRules';

type Props = {
  goals: Goal[];
  mode: 'employee' | 'manager';
};

function bannerClass(valid: boolean) {
  return valid
    ? 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-200'
    : 'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-200';
}

function ValidationShell({ valid, children }: { valid: boolean; children: React.ReactNode }) {
  return <div className={`rounded-lg border px-3 py-2 text-sm ${bannerClass(valid)}`}>{children}</div>;
}

export function GoalValidationBanner({ goals, mode }: Props) {
  if (mode === 'employee') {
    const total = totalWeightage(goals);
    const valid = total === REQUIRED_TOTAL_WEIGHTAGE;
    return (
      <ValidationShell valid={valid}>
        Total weightage: <strong>{total}%</strong>
        {!valid && <span className="ml-2">— must equal {REQUIRED_TOTAL_WEIGHTAGE}% before submission</span>}
      </ValidationShell>
    );
  }

  const grouped = weightageByEmployee(goals);
  if (Object.keys(grouped).length === 0) return null;

  const allValid = Object.values(grouped).every((info) => info.total === REQUIRED_TOTAL_WEIGHTAGE);
  return (
    <ValidationShell valid={allValid}>
      <div className="flex flex-wrap gap-3">
        {Object.entries(grouped).map(([id, info]) => {
          const rowValid = info.total === REQUIRED_TOTAL_WEIGHTAGE;
          return (
            <span
              key={id}
              className={rowValid ? 'text-emerald-700 dark:text-emerald-300' : 'text-amber-700 dark:text-amber-300'}
            >
              {info.name}: <strong>{info.total}%</strong>
              {!rowValid && ` (needs ${REQUIRED_TOTAL_WEIGHTAGE}%)`}
            </span>
          );
        })}
      </div>
    </ValidationShell>
  );
}
