import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CheckCircle2 } from 'lucide-react'
import { api } from '../api/client'
import type { Challenge, SubmissionResult } from '../api/types'
import { Card, EmptyState, ProgressBar, Spinner } from '../components/ui'

export default function ChallengeDetail() {
  const { id } = useParams()
  const qc = useQueryClient()
  const q = useQuery({
    queryKey: ['challenge', id],
    queryFn: () => api<Challenge>(`/challenges/${id}`),
    enabled: !!id,
  })
  const [submission, setSubmission] = useState('')

  const submit = useMutation({
    mutationFn: (content: string) =>
      api<SubmissionResult>(`/challenges/${id}/submit`, {
        method: 'POST',
        body: JSON.stringify({ submissionText: content }),
      }),
    onSuccess: () => qc.invalidateQueries(),
  })

  if (q.isLoading) return <Spinner />
  if (q.isError || !q.data) return <EmptyState message="Challenge not found." />
  const c = q.data

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <Link to="/challenges" className="mb-2 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-indigo-600">
          <ArrowLeft size={14} /> All challenges
        </Link>
        <h1 className="text-2xl font-bold">{c.title}</h1>
        <p className="text-sm text-slate-500">{c.skillName} · {c.difficulty} · ~{c.estMinutes} min</p>
      </div>

      <Card title="Task">
        <p className="whitespace-pre-wrap text-sm leading-relaxed">{c.prompt || 'No prompt provided.'}</p>
        {c.rubric && (
          <details className="mt-4">
            <summary className="cursor-pointer text-xs font-semibold text-indigo-600">View grading rubric</summary>
            <pre className="mt-2 whitespace-pre-wrap rounded-lg bg-slate-50 p-3 text-xs dark:bg-slate-800/60">{c.rubric}</pre>
          </details>
        )}
      </Card>

      <Card title="Your submission">
        <textarea
          className="input min-h-[220px] font-mono text-xs"
          placeholder="Paste your code / solution / writeup here…"
          value={submission}
          onChange={(e) => setSubmission(e.target.value)}
        />
        {submit.isError && <p className="mt-2 text-sm text-red-600">{(submit.error as Error).message}</p>}
        <button
          className="btn-primary mt-3"
          disabled={!submission.trim() || submit.isPending}
          onClick={() => submit.mutate(submission.trim())}
        >
          {submit.isPending ? 'Grading…' : 'Submit for grading'}
        </button>

        {submit.data && (
          <div className="mt-6 space-y-4 rounded-lg border border-emerald-200 bg-emerald-50 p-4 dark:border-emerald-500/20 dark:bg-emerald-500/5">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="text-emerald-600" size={18} />
              <span className="font-bold">Scored {submit.data.score}/100</span>
              <span className="text-xs text-slate-500">
                ({submit.data.checksPassed}/{submit.data.checksTotal} rubric checks passed · evidence added to {c.skillName})
              </span>
            </div>
            {[
              ['Correctness', submit.data.correctness],
              ['Completeness', submit.data.completeness],
              ['Best practices', submit.data.bestPractices],
            ].map(([label, v]) => (
              <div key={label as string}>
                <div className="flex justify-between text-xs text-slate-500">
                  <span>{label}</span><span>{Math.round(v as number)}%</span>
                </div>
                <ProgressBar value={v as number} color="bg-emerald-500" />
              </div>
            ))}
            <pre className="whitespace-pre-wrap text-sm">{submit.data.feedback}</pre>
            <Link to={`/skills`} className="inline-block text-sm font-medium text-indigo-600 hover:underline">
              See updated skill →
            </Link>
          </div>
        )}
      </Card>
    </div>
  )
}
