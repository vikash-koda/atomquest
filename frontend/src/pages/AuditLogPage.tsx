import { useEffect, useState } from 'react';
import { Card, CardTitle } from '@/components/ui/card';
import { api } from '@/api/client';

interface AuditLog {
  id: number;
  action: string;
  entityType: string;
  entityId?: number;
  oldValue?: string;
  newValue?: string;
  timestamp: string;
}

export function AuditLogPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);

  useEffect(() => {
    api.get('/admin/audit-logs').then((r) => setLogs(r.data.content));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-normal">Audit Trail</h1>
        <p className="text-slate-500">Critical user, goal, approval, and check-in actions.</p>
      </div>
      <Card>
        <CardTitle>Recent Activity</CardTitle>
        <div className="mt-4 overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="text-slate-500"><tr><th className="py-2">Time</th><th>Action</th><th>Entity</th><th>Details</th></tr></thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} className="border-t border-border">
                  <td className="py-2">{new Date(log.timestamp).toLocaleString()}</td>
                  <td className="font-semibold">{log.action}</td>
                  <td>{log.entityType} {log.entityId}</td>
                  <td>
                    {log.oldValue && <div className="text-red-500/80 line-through text-xs">{log.oldValue}</div>}
                    {log.newValue && <div className="text-green-600/80 text-xs">{log.newValue}</div>}
                    {!log.oldValue && !log.newValue && <span className="text-slate-400 text-xs">No details</span>}
                  </td>
                </tr>
              ))}
              {logs.length === 0 && (
                <tr><td colSpan={4} className="py-8 text-center text-slate-500">No audit logs found.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
