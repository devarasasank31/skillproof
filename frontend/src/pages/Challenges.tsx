import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { Challenge } from '../api/types'
import { Card, EmptyState, Spinner } from '../components/ui'

const DIFF_COLOR: Record<string, string> = {
  EASY: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400',
  MEDIUM: 'bg-amber-100 text-amber-700 dark:bg-amber-500/10 dark:text-amber-400',
  HARD: 'bg-red-100 text-red-700 dark:bg-red-500/10 dark:text-red-400',
}

export default function Challenges() {
  const q = useQuery({ queryKey: ['challenges'], queryFn: () => api<Challenge[]>('/challenges') })

  if (q.isLoading) return <Spinner />
  if (q.isError) return <EmptyState message="Failed to load challenges" hint={(q.error as Error).message} />

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Challenges</h1>
      <p className="text-sm text-slate-500">
        Practical tasks matched to YOUR resume skills — complete them to build real evidence.
      </p>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        {(q.data || []).map((c) => (
          <Link key={c.id} to={`/challenges/${c.id}`} className="card transition hover:border-indigo-300 dark:hover:border-indigo-700">
            <div className="flex items-start justify-between gap-2">
              <div>
                <p className="font-semibold">{c.title}</p>
                <p className="text-xs text-slate-500">{c.skillName}</p>
              </div>
              <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${DIFF_COLOR[c.difficulty] || ''}`}>
                {c.difficulty}
              </span>
            </div>
            <div className="mt-4 flex justify-between text-xs text-slate-400">
              <span>{c.type.replace('_', ' ')}</span>
              <span>~{c.estMinutes} min</span>
            </div>
          </Link>
        ))}
      </div>
      {!q.isLoading && (q.data || []).length === 0 && (
        <Card title="">
          <EmptyState
            message="No challenges for your skills yet."
            hint="Claim more skills from your resume analysis, or ask an admin to add challenges for your domain."
          />
        </Card>
      )}
    </div>
  )
}
