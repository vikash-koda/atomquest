import { useEffect, useState } from 'react';
import { Download, LockOpen, Plus, FileSpreadsheet, Settings } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/api/client';
import { Button } from '@/components/ui/button';
import { Card, CardTitle } from '@/components/ui/card';
import { Input, Select } from '@/components/ui/input';
import { Goal, User } from '@/types/domain';

export function AdminPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('EMPLOYEE');
  const [department, setDepartment] = useState('Engineering');
  const [managerId, setManagerId] = useState('');
  const [config, setConfig] = useState<Record<string, boolean>>({});

  useEffect(() => {
    load();
  }, []);

  async function load() {
    const [u, g, c] = await Promise.all([api.get('/admin/users'), api.get('/admin/goals'), api.get('/admin/config')]);
    setUsers(u.data);
    setGoals(g.data);
    setConfig(c.data);
  }

  async function createUser() {
    await api.post('/admin/users', { name, email, role, department, managerId: managerId ? Number(managerId) : null, password: '123456' });
    toast.success('User created with password 123456');
    setName('');
    setEmail('');
    setManagerId('');
    load();
  }

  async function updateManager(userId: number, nextManagerId: string) {
    await api.patch(`/admin/users/${userId}`, { managerId: nextManagerId ? Number(nextManagerId) : 0 });
    toast.success('Reporting line updated');
    load();
  }

  async function unlock(id: number) {
    await api.post(`/admin/goals/${id}/unlock`);
    toast.success('Goal unlocked');
    load();
  }

  async function exportCsv() {
    try {
      const response = await api.get('/admin/goals/export', { responseType: 'blob' });
      const blob = new Blob([response.data], { type: 'text/csv' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = 'atomquest-goals-report.csv';
      link.click();
    } catch (e) {
      toast.error('Failed to export CSV');
    }
  }

  async function exportExcel() {
    const xlsx = await import('xlsx');
    const rows = goals.map((g) => ({
      Employee: g.employeeName,
      Title: g.title,
      Status: g.status,
      Weightage: g.weightage,
      'Planned Target': g.target,
      'Actual Achievement': g.achievement ?? '',
      'Progress Score': g.latestProgressScore ?? '',
      Quarter: g.latestQuarter ?? '',
      Deadline: g.deadline,
    }));
    const ws = xlsx.utils.json_to_sheet(rows);
    const wb = xlsx.utils.book_new();
    xlsx.utils.book_append_sheet(wb, ws, 'Goals');
    xlsx.writeFile(wb, 'atomquest-goals-report.xlsx');
  }

  async function toggleConfig(key: string, value: boolean) {
    await api.post(`/admin/config/${key}?value=${value}`);
    toast.success('Configuration updated');
    load();
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-normal">Admin / HR Console</h1>
          <p className="text-slate-500">Manage users, unlock goals, push shared goals, and export reports.</p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={exportCsv}><Download size={16} className="mr-2" /> CSV</Button>
          <Button variant="primary" onClick={exportExcel}><FileSpreadsheet size={16} className="mr-2" /> Excel</Button>
        </div>
      </div>
      <div className="grid gap-4 xl:grid-cols-[.7fr_1.3fr]">
        <Card>
          <CardTitle>Create User</CardTitle>
          <div className="mt-4 space-y-3">
            <Input placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} />
            <Input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
            <Select value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="EMPLOYEE">Employee</option>
              <option value="MANAGER">Manager</option>
              <option value="ADMIN">Admin</option>
            </Select>
            <Input placeholder="Department" value={department} onChange={(e) => setDepartment(e.target.value)} />
            <Select value={managerId} onChange={(e) => setManagerId(e.target.value)}>
              <option value="">No manager</option>
              {users.filter((u) => u.role === 'MANAGER' || u.role === 'ADMIN').map((u) => <option key={u.id} value={u.id}>{u.name}</option>)}
            </Select>
            <Button onClick={createUser}><Plus size={16} /> Create</Button>
          </div>
        </Card>
        <Card>
          <CardTitle>Users</CardTitle>
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-slate-500"><tr><th className="py-2">Name</th><th>Email</th><th>Role</th><th>Department</th><th>Manager</th></tr></thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-t border-border">
                    <td className="py-2 font-semibold">{u.name}</td>
                    <td>{u.email}</td>
                    <td>{u.role}</td>
                    <td>{u.department}</td>
                    <td>
                      <Select value={u.managerId ?? ''} onChange={(e) => updateManager(u.id, e.target.value)}>
                        <option value="">Unassigned</option>
                        {users.filter((manager) => manager.id !== u.id && (manager.role === 'MANAGER' || manager.role === 'ADMIN')).map((manager) => (
                          <option key={manager.id} value={manager.id}>{manager.name}</option>
                        ))}
                      </Select>
                    </td>
                  </tr>
                ))}
                {users.length === 0 && (
                  <tr><td colSpan={5} className="py-8 text-center text-slate-500">No users found. Create one to get started.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
      <div className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardTitle>Unlock Goals</CardTitle>
          <div className="mt-4 grid gap-3">
            {goals.filter((g) => g.locked).map((goal) => (
              <div key={goal.id} className="flex items-center justify-between rounded-lg border border-border p-3">
                <div><div className="font-semibold">{goal.title}</div><div className="text-sm text-slate-500">{goal.employeeName}</div></div>
                <Button variant="secondary" onClick={() => unlock(goal.id)}><LockOpen size={16} /> Unlock</Button>
              </div>
            ))}
          </div>
        </Card>
        <Card>
          <CardTitle className="flex items-center gap-2"><Settings size={20} /> Cycle Management</CardTitle>
          <div className="mt-4 space-y-3">
            {Object.entries(config).map(([key, val]) => (
              <div key={key} className="flex items-center justify-between rounded-lg border border-border p-3">
                <div className="font-semibold text-sm">{key.replace(/_/g, ' ')}</div>
                <Button variant={val ? 'primary' : 'secondary'} onClick={() => toggleConfig(key, !val)}>
                  {val ? 'Enabled' : 'Disabled'}
                </Button>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
