import { Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Bar, BarChart } from 'recharts';
import { Analytics } from '@/types/domain';
import { Card, CardTitle } from '@/components/ui/card';

const palette = ['#2563eb', '#14b8a6', '#f59e0b', '#22c55e', '#ef4444', '#64748b', '#8b5cf6'];

export function AnalyticsCharts({ data }: { data: Analytics }) {
  const status = Object.entries(data.statusDistribution).map(([name, value]) => ({ name, value }));
  const departments = Object.entries(data.departmentProgress).map(([department, score]) => ({ department, score: Math.round(score) }));
  const thrustAreas = Object.entries(data.thrustAreaDistribution).map(([name, value]) => ({ name, value }));
  const uomTypes = Object.entries(data.uomDistribution).map(([name, value]) => ({ name, value }));

  return (
    <div className="grid gap-4 xl:grid-cols-2 2xl:grid-cols-3">
      <Card>
        <CardTitle>Goal Status</CardTitle>
        <div className="h-64">
          <ResponsiveContainer>
            <PieChart>
              <Pie data={status} dataKey="value" nameKey="name" outerRadius={88} innerRadius={48}>
                {status.map((_, i) => <Cell key={i} fill={palette[i % palette.length]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </Card>
      <Card>
        <CardTitle>Quarterly Trend</CardTitle>
        <div className="h-64">
          <ResponsiveContainer>
            <LineChart data={data.quarterlyTrend}>
              <XAxis dataKey="quarter" />
              <YAxis domain={[0, 100]} />
              <Tooltip />
              <Line type="monotone" dataKey="score" stroke="#2563eb" strokeWidth={3} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </Card>
      <Card>
        <CardTitle>Department Progress</CardTitle>
        <div className="h-64">
          <ResponsiveContainer>
            <BarChart data={departments}>
              <XAxis dataKey="department" />
              <YAxis domain={[0, 100]} />
              <Tooltip />
              <Bar dataKey="score" fill="#14b8a6" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
      <Card>
        <CardTitle>Thrust Area Distribution</CardTitle>
        <div className="h-64">
          <ResponsiveContainer>
            <PieChart>
              <Pie data={thrustAreas} dataKey="value" nameKey="name" outerRadius={88} innerRadius={48}>
                {thrustAreas.map((_, i) => <Cell key={i} fill={palette[i % palette.length]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </Card>
      <Card>
        <CardTitle>UoM Distribution</CardTitle>
        <div className="h-64">
          <ResponsiveContainer>
            <BarChart data={uomTypes}>
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#8b5cf6" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </div>
  );
}
