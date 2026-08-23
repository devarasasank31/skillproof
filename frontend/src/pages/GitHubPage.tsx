import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Github } from 'lucide-react'
import { api } from '../api/client'
import type { GitHubAnalyze } from '../api/types'
import { Card, EmptyState, ProgressBar } from '../components/ui'

export default function GitHubPage() {
  const [username, setUsername] = useState('')
  const [result, setResult] = useState<GitHubAnalyze | null>(null)

  const analyze = useMutation({
    mutationFn: (u: string) =>
      api<GitHubAnalyze>('/github/analyze', { method: 'POST', body: JSON.stringify({ username: u }) }),
    onSuccess: setResult,
  })

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-bold">GitHub analysis</h1>

      <Card title="Analyze a GitHub profile">
        <form
          className="flex gap-2"
          onSubmit={(e) => {
            e.preventDefault()
            if (username.trim()) analyze.mutate(username.trim())
          }}
        >
          <input
            className="input flex-1"
            placeholder="GitHub username or profile URL…"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <button className="btn-primary" disabled={!username.trim() || analyze.isPending}>
            <Github size={15} /> {analyze.isPending ? 'Analyzing…' : 'Analyze'}
          </button>
        </form>
        {analyze.isError && (
          <p className="mt-2 text-sm text-red-600">{(analyze.error as Error).message}</p>
        )}
        <p className="mt-2 text-xs text-slate-400">
          Uses the public GitHub API. Languages and topics are mapped to skill names; each mapped repo adds activity
          evidence to matching skills.
        </p>
      </Card>

      {result && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Card title={`@${result.username}`}>
              <p className="text-3xl font-bold">{result.publicRepos}</p>
              <p className="text-xs text-slate-400">public repositories</p>
            </Card>
            <Card title="Mapped skills">
              {result.mappedSkills.length === 0 ? (
                <p className="text-sm text-slate-500">No recognizable skills found in this profile's repos.</p>
              ) : (
                <ul className="space-y-2 text-sm">
                  {result.mappedSkills.map((m) => (
                    <li key={m.skillName}>
                      <div className="flex justify-between text-xs text-slate-500">
                        <span>{m.skillName}</span><span>{m.repoCount} repos</span>
                      </div>
                      <ProgressBar value={(m.repoCount / Math.max(1, result.publicRepos)) * 100} color="bg-emerald-500" />
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>

          <Card title={`Repositories (${result.repos.length})`}>
            {result.repos.length === 0 ? (
              <EmptyState message="No public repos." />
            ) : (
              <ul className="divide-y divide-slate-100 text-sm dark:divide-slate-800">
                {result.repos.map((r) => (
                  <li key={r.name} className="py-3">
                    <div className="flex items-center justify-between">
                      <span className="font-medium">{r.name}</span>
                      {r.primaryLanguage && (
                        <span className="rounded bg-indigo-100 px-2 py-0.5 text-[10px] font-semibold text-indigo-700 dark:bg-indigo-500/10 dark:text-indigo-400">
                          {r.primaryLanguage}
                        </span>
                      )}
                    </div>
                    {r.description && <p className="mt-0.5 text-xs text-slate-500">{r.description}</p>}
                    {(r.languages.length > 0 || r.topics.length > 0) && (
                      <p className="mt-1 flex flex-wrap gap-1 text-[10px] text-slate-400">
                        {[...r.languages, ...r.topics].map((t) => (
                          <span key={t} className="rounded bg-slate-100 px-1 dark:bg-slate-800">{t}</span>
                        ))}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </>
      )}
    </div>
  )
}
