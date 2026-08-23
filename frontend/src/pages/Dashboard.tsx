import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, KeyRound, Sparkles } from 'lucide-react'
import { api } from '../api/client'
import type { AiStatus, DashboardData } from '../api/types'
import { Card, EmptyState, ProgressBar, Spinner, StateBadge } from '../components/ui'

export default function Dashboard() {
  const nav = useNavigate()
  const q = useQuery({ queryKey: ['dashboard'], queryFn: () => api<DashboardData>('/dashboard') })
  const ai = useQuery({ queryKey: ['ai'], queryFn: () => api<AiStatus>('/profile/ai') })

  if (q.isLoading) return <Spinner />
  if (q.isError) return <EmptyState message="Failed to load dashboard" hint={String((q.error as Error).message)} />
  const d = q.data!

  // Server runs in UTC, so the greeting must be computed with the viewer's own clock.
  const hour = new Date().getHours()
  const greeting = hour < 5 ? 'Burning the midnight oil'
    : hour < 12 ? 'Good morning'
    : hour < 17 ? 'Good afternoon'
    : hour < 21 ? 'Good evening'
    : 'Late-night grind'

  function act(actionType: string, skillId?: number) {
    switch (actionType) {
      case 'REVIEW': return '/reviews'
      case 'ASSESSMENT': return `/skills/${skillId}`
      case 'CHALLENGE': return '/challenges'
      case 'INTERVIEW': return '/interview'
      default: return '/skills'
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{greeting}</h1>
        <p className="text-sm text-slate-500">{d.date}</p>
      </div>

      {ai.data && !ai.data.provider && (
        <Link
          to="/settings"
          className="flex items-center justify-between gap-3 rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-3 transition hover:border-indigo-400 dark:border-indigo-800 dark:bg-indigo-950/40"
        >
          <span className="flex items-center gap-3 text-sm">
            <KeyRound size={18} className="shrink-0 text-indigo-500" />
            <span>
              <span className="font-semibold text-indigo-700 dark:text-indigo-300">Add your AI API key</span>
              <span className="text-slate-600 dark:text-slate-300">
                {' '}— unlock AI grading and auto-generated questions for any skill. Your key, your provider, your bill.
              </span>
            </span>
          </span>
          <ArrowRight size={16} className="shrink-0 text-indigo-500" />
        </Link>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card title="Job readiness">
          <p className="text-3xl font-bold">{Math.round(d.readiness)}%</p>
          <div className="mt-3"><ProgressBar value={d.readiness} /></div>
        </Card>
        <Card title="Average retention">
          <p className="text-3xl font-bold">{Math.round(d.retentionAvg)}%</p>
          <p className="mt-1 text-xs text-slate-400">Memory-model predicted recall</p>
        </Card>
        <Card title="Tracked skills">
          <p className="text-3xl font-bold">{d.totalSkills}</p>
          <Link to="/skills" className="mt-1 inline-flex items-center gap-1 text-xs font-medium text-indigo-600 hover:underline">
            Manage skills <ArrowRight size={12} />
          </Link>
        </Card>
        <Card title="Due reviews">
          <p className="text-3xl font-bold">{d.dueReviews}</p>
          <Link to="/reviews" className="mt-1 inline-flex items-center gap-1 text-xs font-medium text-indigo-600 hover:underline">
            Review now <ArrowRight size={12} />
          </Link>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card title="At-risk skills">
          {d.atRisk.length === 0 ? (
            <EmptyState message="Nothing at risk right now." />
          ) : (
            <div className="space-y-3">
              {d.atRisk.map((s) => (
                <Link
                  key={s.id}
                  to={`/skills/${s.id}`}
                  className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 transition hover:border-indigo-300 dark:border-slate-800"
                >
                  <div>
                    <p className="font-medium">{s.name}</p>
                    <p className="text-xs text-slate-500">confidence {s.confidence}% · {s.trend}</p>
                  </div>
                  <StateBadge state={s.state} />
                </Link>
              ))}
            </div>
          )}
        </Card>

        <Card title="Next best action">
          {!d.nextBestAction ? (
            <EmptyState message="All caught up. Add a skill or take an assessment." hint="Recommendations appear as evidence changes." />
          ) : (
            <div className="space-y-4">
              <div className="flex items-start gap-3 rounded-lg bg-indigo-50 p-4 dark:bg-indigo-500/10">
                <Sparkles className="mt-0.5 shrink-0 text-indigo-600 dark:text-indigo-400" size={18} />
                <div>
                  <p className="font-semibold">{d.nextBestAction.title}</p>
                  <p className="mt-0.5 text-sm text-slate-600 dark:text-slate-300">{d.nextBestAction.reason}</p>
                  {d.nextBestAction.effortMinutes != null && (
                    <p className="mt-1 text-xs text-slate-500">~{d.nextBestAction.effortMinutes} min</p>
                  )}
                </div>
              </div>
              <button
                className="btn-primary"
                onClick={() => nav(act(d.nextBestAction!.actionType))}
              >
                Do it now <ArrowRight size={14} />
              </button>
            </div>
          )}
        </Card>
      </div>

      {d.dueToday.length > 0 && (
        <Card title={`Due today (${d.dueToday.length})`}>
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {d.dueToday.map((r) => (
              <li key={r.reviewId} className="flex items-center justify-between py-2 text-sm">
                <span>{r.skillName}</span>
                <Link to="/reviews" className="text-xs font-medium text-indigo-600 hover:underline">Review now</Link>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
