import { FormEvent, useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { DashboardData, Profile, RecommendationDto } from '../api/types'
import { Card, EmptyState, Spinner, StateBadge } from '../components/ui'

export default function Profile() {
  const profile = useQuery({ queryKey: ['profile'], queryFn: () => api<Profile>('/profile') })
  const recs = useQuery({ queryKey: ['recommendations'], queryFn: () => api<RecommendationDto[]>('/recommendations') })
  const dash = useQuery({ queryKey: ['dashboard'], queryFn: () => api<DashboardData>('/dashboard') })

  const [name, setName] = useState('')
  const [headline, setHeadline] = useState('')
  const [bio, setBio] = useState('')
  const [visibility, setVisibility] = useState('PRIVATE')
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const p = profile.data
  useEffect(() => {
    if (p) {
      setName(p.name)
      setHeadline(p.headline || '')
      setBio(p.bio || '')
      setVisibility(p.visibility)
    }
  }, [p])

  async function save(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await api('/profile', {
        method: 'PUT',
        body: JSON.stringify({ name, headline, bio, visibility }),
      })
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } catch (err: any) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (profile.isLoading) return <Spinner />

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-bold">Profile</h1>

      <Card title="Your details">
        <form onSubmit={save} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium">Name</label>
              <input className="input" required value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium">Email</label>
              <input className="input" value={p?.email || ''} disabled />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Headline</label>
            <input className="input" placeholder="e.g. Backend engineer levelling up on distributed systems" value={headline} onChange={(e) => setHeadline(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Bio</label>
            <textarea className="input min-h-[80px]" value={bio} onChange={(e) => setBio(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Profile visibility</label>
            <select className="input" value={visibility} onChange={(e) => setVisibility(e.target.value)}>
              <option value="PRIVATE">Private — only you can see your scores</option>
              <option value="PUBLIC">Public — shareable verified skill profile</option>
            </select>
            <p className="mt-1 text-xs text-slate-400">
              Public mode shows confidence badges without exposing raw assessment content.
            </p>
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button className="btn-primary" disabled={saving}>
            {saving ? 'Saving…' : saved ? 'Saved ✓' : 'Save changes'}
          </button>
        </form>
      </Card>

      {p && (
        <Card title="Activity stats">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {([
              ['Skills', p.stats.skills],
              ['Assessments', p.stats.assessments],
              ['Challenges', p.stats.challenges],
              ['Repos analyzed', p.stats.repos],
            ] as [string, number][]).map(([label, v]) => (
              <div key={label as string} className="rounded-lg bg-slate-50 p-3 text-center dark:bg-slate-800/60">
                <p className="text-2xl font-bold">{v}</p>
                <p className="text-xs text-slate-500">{label}</p>
              </div>
            ))}
          </div>
        </Card>
      )}

      {dash.data && (
        <Card title="Next best action">
          {!dash.data.nextBestAction ? (
            <EmptyState message="All caught up." />
          ) : (
            <div className="rounded-lg bg-indigo-50 p-4 dark:bg-indigo-500/10">
              <p className="font-semibold">{dash.data.nextBestAction.title}</p>
              <p className="mt-0.5 text-sm text-slate-600 dark:text-slate-300">{dash.data.nextBestAction.reason}</p>
            </div>
          )}
        </Card>
      )}

      <Card title="Open recommendations">
        {recs.isLoading ? (
          <Spinner />
        ) : !recs.data || recs.data.length === 0 ? (
          <EmptyState message="No open recommendations." hint="Recommendations are generated as your evidence changes." />
        ) : (
          <ul className="divide-y divide-slate-100 text-sm dark:divide-slate-800">
            {recs.data.map((r) => (
              <li key={r.id} className="flex items-start justify-between gap-3 py-3">
                <div>
                  <p className="font-medium">
                    {r.title}
                    {r.skillName && <span className="ml-2 text-xs text-indigo-600">{r.skillName}</span>}
                  </p>
                  <p className="mt-0.5 text-xs text-slate-500">{r.reason}</p>
                  {r.effortMinutes != null && <p className="mt-0.5 text-xs text-slate-400">~{r.effortMinutes} min</p>}
                </div>
                <span className="shrink-0 rounded-md bg-slate-200 px-2 py-0.5 text-[10px] font-bold dark:bg-slate-800">
                  P{r.priority}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {dash.data && dash.data.dueToday.length > 0 && (
        <Card title={`Reviews due today (${dash.data.dueToday.length})`}>
          <ul className="divide-y divide-slate-100 text-sm dark:divide-slate-800">
            {dash.data.dueToday.map((r) => (
              <li key={r.reviewId} className="flex items-center justify-between py-2">
                <span>{r.skillName}</span>
                <StateBadge state="STALE" />
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
