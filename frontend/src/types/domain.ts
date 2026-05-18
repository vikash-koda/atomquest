export type Role = 'EMPLOYEE' | 'MANAGER' | 'ADMIN';
export type GoalStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'NOT_STARTED' | 'ON_TRACK' | 'COMPLETED';
export type ProgressStatus = 'NOT_STARTED' | 'ON_TRACK' | 'COMPLETED';
export type UomType = 'NUMERIC' | 'PERCENT' | 'MAX' | 'ZERO' | 'TIMELINE';
export type Quarter = 'Q1' | 'Q2' | 'Q3' | 'Q4';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  department: string;
  managerId?: number;
}

export interface Goal {
  id: number;
  employeeId: number;
  employeeName: string;
  title: string;
  description?: string;
  thrustArea: string;
  uomType: UomType;
  target: number;
  achievement?: number;
  weightage: number;
  status: GoalStatus;
  locked: boolean;
  sharedGoalId?: number;
  deadline: string;
  createdAt: string;
  latestQuarter?: Quarter;
  latestProgressScore?: number;
  latestProgressStatus?: ProgressStatus;
  latestUpdateComment?: string;
  latestCompletionDate?: string;
}

export interface SharedGoal {
  id: number;
  title: string;
  description?: string;
  thrustArea: string;
  uomType: UomType;
  target: number;
  deadline: string;
  department: string;
}

export interface ManagerComment {
  id: number;
  employeeId: number;
  employeeName: string;
  managerName: string;
  quarter: string;
  comment: string;
  createdAt: string;
}

export interface Analytics {
  totalGoals: number;
  completedGoals: number;
  approvedGoals: number;
  completionRate: number;
  averageProgress: number;
  employeeCount: number;
  statusDistribution: Record<string, number>;
  departmentProgress: Record<string, number>;
  quarterlyTrend: Array<{ quarter: string; score: number }>;
  lowPerformingGoals: Array<{ id: number; title: string; employee: string; score: number }>;
  thrustAreaDistribution: Record<string, number>;
  uomDistribution: Record<string, number>;
  managerEffectiveness: Record<string, number>;
  checkInCompletion: {
    quarter: Quarter;
    achievementCompletionRate: number;
    managerCheckInCompletionRate: number;
    employeeRows: Array<{
      employeeId: number;
      employeeName: string;
      managerName: string;
      department: string;
      achievementDone: boolean;
      managerCheckInDone: boolean;
    }>;
  };
}
