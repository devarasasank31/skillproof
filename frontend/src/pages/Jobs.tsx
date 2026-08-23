import { FormEvent, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { JobAnalyzeResult, JobSummary, MarketResponse } from '../api/types'
import { Card, EmptyState, ProgressBar, Spinner } from '../components/ui'

export default function Jobs() {
  const qc = useQueryClient()
  const [jd, setJd] = useState('')
  const [title, setTitle] = useState('')
  const [result, setResult] = useState<JobAnalyzeResult | null>(null)

  const history = useQuery({ queryKey: ['jobs'], queryFn: () => api<JobSummary[]>('/jobs') })
  const market = useQuery({ queryKey: ['market'], queryFn: () => api<MarketResponse>('/jobs/market') })

  const analyze = useMutation({
    mutationFn: () =>
      api<JobAnalyzeResult>('/jobs/analyze', {
        method: 'POST',
        body: JSON.stringify({ title: title.trim() || 'Untitled role', text: jd.trim() }),
      }),
    onSuccess: (data) => {
      setResult(data)
      qc.invalidateQueries({ queryKey: ['jobs'] })
      qc.invalidateQueries({ queryKey: ['market'] })
    },
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    if (jd.trim()) analyze.mutate()
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Job readiness</h1>

      <Card title="Paste a job description">
        <form onSubmit={submit} className="space-y-3">
          <input
            className="input"
            placeholder="Job title (e.g. Senior Backend Engineer)"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
          <textarea
            className="input min-h-[160px]"
            placeholder="Paste the full job posting here — responsibilities, requirements, nice-to-haves…"
            value={jd}
            onChange={(e) => setJd(e.target.value)}
          />
          {analyze.isError && <p className="text-sm text-red-600">{(analyze.error as Error).message}</p>}
          <button className="btn-primary" disabled={!jd.trim() || analyze.isPending}>
            {analyze.isPending ? 'Analyzing…' : 'Analyze against my profile'}
          </button>
        </form>
      </Card>

      {result && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <Card title={`Readiness · ${result.title || 'Role'}`}>
            <p className="text-4xl font-bold">{Math.round(result.readiness)}%</p>
            <div className="mt-3"><ProgressBar value={result.readiness} /></div>
          </Card>
          <div className="lg:col-span-2 space-y-4">
            <Card title="Matched skills">
              {result.skills.length === 0 ? (
                <EmptyState message="No known skills detected in this posting." />
              ) : (
                <ul className="space-y-2 text-sm">
                  {result.skills.map((s) => (
                    <li key={s.name} className="flex items-center justify-between">
                      <span>
                        {s.name} {s.required && <span className="ml-1 rounded bg-slate-200 px-1 text-[10px] font-semibold dark:bg-slate-800">required</span>}
                      </span>
                      {s.confidence != null ? (
                        <span className="flex w-40 items-center gap-2">
                          <ProgressBar value={s.confidence} />
                          <span className="w-9 text-right">{s.confidence}%</span>
                        </span>
                      ) : (
                        <span className="text-xs text-slate-400">not tracked</span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </Card>
            {result.gaps.length > 0 && (
              <Card title="Gaps to close">
                <div className="flex flex-wrap gap-2">
                  {result.gaps.map((g) => (
                    <span key={g.name} className="rounded-full bg-rose-100 px-3 py-1 text-xs font-medium text-rose-700 dark:bg-rose-500/10 dark:text-rose-400">
                      {g.name}{g.required ? '' : ' (nice to have)'}
                    </span>
                  ))}
                </div>
              </Card>
            )}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card title="Market demand (from analyzed postings)">
          {!market.data || market.data.rows.length === 0 ? (
            <EmptyState message="Analyze a few job descriptions to build market signal." />
          ) : (
            <>
              <ul className="space-y-3 text-sm">
                {market.data.rows.map((row) => (
                  <li key={row.name}>
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-medium">{row.name}</span>
                      <span className="text-slate-400">
                        in {row.frequency}/{row.totalJobs} postings
                        {row.yourConfidence != null ? ` · you: ${row.yourConfidence}%` : ' · not tracked'}
                      </span>
                    </div>
                    <ProgressBar
                      value={(row.frequency / Math.max(1, row.totalJobs)) * 100}
                      color={row.yourConfidence != null && row.yourConfidence >= 60 ? 'bg-emerald-500' : 'bg-slate-400'}
                    />
                  </li>
                ))}
              </ul>
              <p className="mt-3 text-xs text-slate-400">{market.data.totalJobs} posting(s) analyzed so far.</p>
            </>
          )}
        </Card>

        <Card title="Previously analyzed jobs">
          {history.isLoading ? (
            <Spinner />
          ) : !history.data || history.data.length === 0 ? (
            <EmptyState message="No analyses yet." />
          ) : (
            <ul className="divide-y divide-slate-100 text-sm dark:divide-slate-800">
              {history.data.map((j) => (
                <li key={j.jobId} className="flex items-center justify-between py-2">
                  <div>
                    <p className="font-medium">{j.title}</p>
                    <p className="text-xs text-slate-400">{j.company || 'Unknown company'} · {new Date(j.createdAt).toLocaleDateString()}</p>
                  </div>
                  {j.readiness != null && <span className="font-semibold">{Math.round(j.readiness)}%</span>}
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  )
}
