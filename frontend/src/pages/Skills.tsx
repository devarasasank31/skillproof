import { FormEvent, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Upload } from 'lucide-react'
import { api, apiUpload } from '../api/client'
import type {
  CatalogItem, ResumeAnalyze, ResumeConfirm, SkillRow,
} from '../api/types'
import { Card, EmptyState, ProgressBar, Spinner, StateBadge, STATE_EXPLANATION, fmtDate } from '../components/ui'

export default function Skills() {
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [stateFilter, setStateFilter] = useState('ALL')
  const [claimName, setClaimName] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)
  const [detected, setDetected] = useState<ResumeAnalyze | null>(null)

  const skills = useQuery({ queryKey: ['skills'], queryFn: () => api<SkillRow[]>('/skills') })
  const catalog = useQuery({ queryKey: ['catalog'], queryFn: () => api<CatalogItem[]>('/skills/catalog') })

  function invalidate() {
    qc.invalidateQueries({ queryKey: ['skills'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const claim = useMutation({
    mutationFn: (name: string) => api('/skills', { method: 'POST', body: JSON.stringify({ skillName: name }) }),
    onSuccess: () => {
      setClaimName('')
      invalidate()
    },
  })

  const remove = useMutation({
    mutationFn: (id: number) => api(`/skills/${id}`, { method: 'DELETE' }),
    onSuccess: invalidate,
  })

  const analyzeResume = useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('file', file)
      return apiUpload<ResumeAnalyze>('/resume/analyze', form)
    },
    onSuccess: setDetected,
  })

  const confirmResume = useMutation({
    mutationFn: (names: string[]) =>
      api<ResumeConfirm>('/resume/confirm', { method: 'POST', body: JSON.stringify({ skillNames: names }) }),
    onSuccess: () => {
      setDetected(null)
      if (fileRef.current) fileRef.current.value = ''
      invalidate()
    },
  })

  function submitClaim(e: FormEvent) {
    e.preventDefault()
    if (claimName.trim()) claim.mutate(claimName.trim())
  }

  const rows = (skills.data || []).filter(
    (s) =>
      (stateFilter === 'ALL' || s.state === stateFilter) &&
      (!search || s.name.toLowerCase().includes(search.toLowerCase()) || s.category.toLowerCase().includes(search.toLowerCase())),
  )
  const states = ['ALL', 'NEW', 'LEARNING', 'STRONG', 'MASTERED', 'AT_RISK', 'STALE', 'WEAK', 'OVERCLAIMED']

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Skills</h1>
        <form onSubmit={submitClaim} className="flex gap-2">
          <input
            className="input w-56"
            placeholder="Add a skill you claim…"
            list="catalog-list"
            value={claimName}
            onChange={(e) => setClaimName(e.target.value)}
          />
          <datalist id="catalog-list">
            {(catalog.data || []).map((c) => (
              <option key={c.id} value={c.name} />
            ))}
          </datalist>
          <button className="btn-primary" disabled={claim.isPending}>
            {claim.isPending ? 'Adding…' : 'Claim'}
          </button>
        </form>
      </div>
      {(claim.isError || remove.isError || confirmResume.isError) && (
        <p className="text-sm text-red-600">
          {((claim.error || remove.error || confirmResume.error) as Error)?.message}
        </p>
      )}

      <Card title="Import skills from resume (PDF)">
        <div className="flex flex-wrap items-center gap-3">
          <label className="btn-secondary cursor-pointer">
            <Upload size={15} /> Choose PDF
            <input
              ref={fileRef}
              type="file"
              accept="application/pdf"
              className="hidden"
              onChange={(e) => {
                const f = e.target.files?.[0]
                if (f) analyzeResume.mutate(f)
              }}
            />
          </label>
          {analyzeResume.isPending && <span className="text-sm text-slate-500">Extracting…</span>}
          {analyzeResume.isError && <span className="text-sm text-red-600">{(analyzeResume.error as Error).message}</span>}
          {detected && (
            <span className="text-sm text-slate-500">
              Found in {detected.fileName} ({detected.pages}p):{' '}
              {detected.detected.length === 0 ? 'nothing recognizable' : detected.detected.map((d) => d.name).join(', ')}
            </span>
          )}
        </div>
        {detected && detected.detected.length > 0 && (
          <form
            className="mt-3"
            onSubmit={(e) => {
              e.preventDefault()
              confirmResume.mutate(detected.detected.map((d) => d.name))
            }}
          >
            <button className="btn-primary" disabled={confirmResume.isPending}>
              {confirmResume.isPending ? 'Adding…' : `Claim all ${detected.detected.length} as RESUME claims`}
            </button>
          </form>
        )}
        {confirmResume.data && (
          <p className="mt-2 text-sm text-emerald-600">
            Added {confirmResume.data.added}: {confirmResume.data.addedNames.join(', ')}
            {confirmResume.data.skipped.length > 0 && ` · skipped: ${confirmResume.data.skipped.join(', ')}`}
          </p>
        )}
      </Card>

      <div className="flex flex-wrap gap-2">
        {states.map((s) => (
          <button
            key={s}
            onClick={() => setStateFilter(s)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${
              stateFilter === s
                ? 'bg-indigo-600 text-white'
                : 'bg-slate-200 text-slate-600 hover:bg-slate-300 dark:bg-slate-800 dark:text-slate-300'
            }`}
          >
            {s.replace('_', ' ')}
          </button>
        ))}
      </div>

      <input className="input max-w-md" placeholder="Search skills…" value={search} onChange={(e) => setSearch(e.target.value)} />

      {skills.isLoading ? (
        <Spinner />
      ) : rows.length === 0 ? (
        <EmptyState message="No skills match." hint="Claim a skill above or import from a resume — then take an assessment to calibrate it." />
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {rows.map((s) => (
            <div key={s.id} className="card group relative transition hover:border-indigo-300 dark:hover:border-indigo-700">
              <Link to={`/skills/${s.id}`}>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-semibold">{s.name}</p>
                    <p className="text-xs text-slate-500">{s.category} · via {s.claimSource.replace('_', ' ').toLowerCase()}</p>
                  </div>
                  <StateBadge state={s.state} />
                </div>
                <p className="mt-3 mb-1 text-xs text-slate-500">confidence {s.confidence}%</p>
                <ProgressBar value={s.confidence} />
                <p className="mt-2 line-clamp-2 text-xs text-slate-400">{STATE_EXPLANATION[s.state]}</p>
                <div className="mt-3 flex justify-between text-xs text-slate-400">
                  <span>retention ~{Math.round(s.retention * 100)}%</span>
                  <span>next review {fmtDate(s.nextReviewAt)}</span>
                </div>
              </Link>
              <button
                onClick={() => remove.mutate(s.id)}
                title="Remove skill"
                className="absolute right-3 bottom-3 text-xs text-slate-300 opacity-0 transition hover:text-red-500 group-hover:opacity-100"
              >
                remove
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
