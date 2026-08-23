import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/client'
import type { DueReview } from '../api/types'
import { Card, EmptyState, Spinner, fmtDate } from '../components/ui'

export default function Reviews() {
  const qc = useQueryClient()
  const [done, setDone] = useState<Record<number, boolean>>({})
  const due = useQuery({ queryKey: ['reviews', 'due'], queryFn: () => api<DueReview[]>('/reviews/today') })

  const complete = useMutation({
    mutationFn: (reviewId: number) =>
      api(`/reviews/${reviewId}/complete`, { method: 'POST', body: JSON.stringify({ score: 80 }) }),
    onSuccess: (_d, reviewId) => {
      setDone((d) => ({ ...d, [reviewId]: true }))
      qc.invalidateQueries()
    },
  })

  if (due.isLoading) return <Spinner />

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Spaced reviews</h1>
      <p className="text-sm text-slate-500">
        Reviews are scheduled by the memory model: stronger skills wait longer between reviews. Completing one on time
        extends the next interval; skipping shrinks it.
      </p>

      {!due.data || due.data.length === 0 ? (
        <EmptyState message="Nothing due right now." hint="New reviews appear after assessments and as retention decays." />
      ) : (
        <Card title={`Due now (${due.data.length})`}>
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {due.data.map((r) => (
              <li key={r.reviewId} className="flex items-center justify-between py-3">
                <div>
                  <p className="font-medium">{r.skillName}</p>
                  <p className="text-xs text-slate-400">due {fmtDate(r.dueAt)}</p>
                </div>
                {done[r.reviewId] ? (
                  <span className="rounded-md bg-emerald-100 px-2 py-1 text-xs font-semibold text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400">
                    Done — interval extended
                  </span>
                ) : (
                  <button
                    className="btn-primary !px-3 !py-1.5 text-xs"
                    disabled={complete.isPending}
                    onClick={() => complete.mutate(r.reviewId)}
                  >
                    Mark reviewed
                  </button>
                )}
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
