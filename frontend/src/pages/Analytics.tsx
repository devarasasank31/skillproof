import { useQuery } from '@tanstack/react-query'
import {
  BarChart, Bar, CartesianGrid, Legend, LineChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { api } from '../api/client'
import type { AnalyticsData } from '../api/types'
import { Card, EmptyState, Spinner } from '../components/ui'

export default function Analytics() {
  const q = useQuery({ queryKey: ['analytics'], queryFn: () => api<AnalyticsData>('/analytics') })

  if (q.isLoading) return <Spinner />
  if (q.isError) return <EmptyState message="Failed to load analytics" hint={(q.error as Error).message} />
  const d = q.data!

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Analytics</h1>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card title="Job readiness">
          <p className="text-3xl font-bold">{d.readiness}%</p>
        </Card>
        <Card title="Strongest skill">
          <p className="text-3xl font-bold">{d.strongest}%</p>
        </Card>
        <Card title="Weakest skill">
          <p className="text-3xl font-bold">{d.weakestConfidence}%</p>
          <p className="text-xs text-slate-400">{d.weakestName || '—'}</p>
        </Card>
      </div>

      <Card title="Average confidence by category">
        {d.categories.length === 0 ? (
          <EmptyState message="No data yet." hint="Claim skills and gather evidence to populate analytics." />
        ) : (
          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer>
              <BarChart data={d.categories}>
                <CartesianGrid strokeDasharray="3 3" stroke="#8884" />
                <XAxis dataKey="category" fontSize={11} />
                <YAxis domain={[0, 100]} fontSize={11} />
                <Tooltip />
                <Bar dataKey="avgConfidence" name="Avg confidence" fill="#6366f1" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>

      <Card title="Top skills over time">
        {d.topSkillTrends.length === 0 ? (
          <EmptyState message="No snapshots yet." hint="Snapshots are recorded whenever a skill is recalculated." />
        ) : (
          <div style={{ width: '100%', height: 300 }}>
            <ResponsiveContainer>
              <LineChart>
                <CartesianGrid strokeDasharray="3 3" stroke="#8884" />
                <XAxis
                  dataKey="date"
                  type="category"
                  allowDuplicatedCategory={false}
                  fontSize={11}
                />
                <YAxis domain={[0, 100]} fontSize={11} />
                <Tooltip />
                <Legend />
                {d.topSkillTrends.map((trend, i) => {
                  const palette = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']
                  return (
                    <Line
                      key={trend.skillName}
                      data={trend.points.map((p) => ({ date: new Date(p.snapshotAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }), [trend.skillName]: p.confidence }))}
                      dataKey={trend.skillName}
                      stroke={palette[i % palette.length]}
                      strokeWidth={2}
                      dot={false}
                      connectNulls
                    />
                  )
                })}
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>
    </div>
  )
}
