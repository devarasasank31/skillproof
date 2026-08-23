import { useEffect, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ListChecks, Send } from 'lucide-react'
import { api } from '../api/client'
import type {
  InterviewAnswerResult, InterviewReport, InterviewStart, SkillRow,
} from '../api/types'
import { Card, EmptyState, ProgressBar, Spinner } from '../components/ui'

export default function Interview() {
  const skills = useQuery({ queryKey: ['skills'], queryFn: () => api<SkillRow[]>('/skills') })
  const [selected, setSelected] = useState<Set<number>>(new Set())
  const [targetRole, setTargetRole] = useState('')
  const [session, setSession] = useState<InterviewStart | null>(null)
  const [texts, setTexts] = useState<Record<number, string>>({})
  const [results, setResults] = useState<Record<number, InterviewAnswerResult>>({})
  const [report, setReport] = useState<InterviewReport | null>(null)

  // Interview is resume-driven: every skill extracted from your resume is selected by default.
  useEffect(() => {
    if (skills.data && skills.data.length > 0) {
      setSelected(new Set(skills.data.map((s) => s.skillId)))
    }
  }, [skills.data])

  const start = useMutation({
    mutationFn: () =>
      api<InterviewStart>('/interviews', {
        method: 'POST',
        body: JSON.stringify({ targetRole, skillIds: Array.from(selected) }),
      }),
    onSuccess: (data) => {
      setSession(data)
      setTexts({})
      setResults({})
      setReport(null)
    },
  })

  const answerOne = useMutation({
    mutationFn: async (qid: number) => {
      if (!session || !texts[qid]?.trim()) return
      const r = await api<InterviewAnswerResult>(`/interviews/${session.sessionId}/answer`, {
        method: 'POST',
        body: JSON.stringify({ questionId: qid, answerText: texts[qid].trim() }),
      })
      setResults((prev) => ({ ...prev, [qid]: r }))
    },
  })

  const finish = useMutation({
    mutationFn: () => {
      if (!session) return Promise.reject(new Error('No session'))
      return api<InterviewReport>(`/interviews/${session.sessionId}/complete`, { method: 'POST' })
    },
    onSuccess: (r) => {
      setReport(r)
    },
  })

  if (report) {
    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <h1 className="text-2xl font-bold">Interview report</h1>
        <Card title="Overall score">
          <p className="text-4xl font-bold">{Math.round(report.overallScore)}%</p>
          <div className="mt-3"><ProgressBar value={report.overallScore} /></div>
        </Card>
        <Card title="Per-skill performance">
          <ul className="space-y-3 text-sm">
            {report.perSkill.map((p) => (
              <li key={p.skillName}>
                <div className="flex justify-between text-xs text-slate-500">
                  <span>{p.skillName}</span><span>{p.score}%</span>
                </div>
                <ProgressBar value={p.score} color={p.score >= 70 ? 'bg-emerald-500' : p.score >= 40 ? 'bg-amber-500' : 'bg-red-500'} />
              </li>
            ))}
          </ul>
        </Card>
        {report.weakest && (
          <Card title="Weakest area">
            <p className="text-sm">{report.weakest}</p>
          </Card>
        )}
        <Card title="Study plan">
          <ol className="list-decimal space-y-2 pl-5 text-sm">
            {report.plan.map((step, i) => <li key={i}>{step}</li>)}
          </ol>
        </Card>
        <button
          className="btn-secondary"
          onClick={() => { setReport(null); setSession(null); setSelected(new Set()); setTargetRole('') }}
        >
          Run another interview
        </button>
      </div>
    )
  }

  if (!session) {
    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <h1 className="text-2xl font-bold">Mock interview</h1>
        <EmptyState
          message="Practice answering interview questions on your tracked skills."
          hint="Answers are graded by keyword matching (or AI when configured), scored 0-100, and feed evidence back into your profile."
        />
        <Card title="Setup">
          <label className="mb-1 block text-sm font-medium">Target role</label>
          <input
            className="input"
            placeholder="e.g. Backend Engineer"
            value={targetRole}
            onChange={(e) => setTargetRole(e.target.value)}
          />
          <p className="mb-1 mt-4 text-sm font-medium">Interview me on:</p>
          {skills.isLoading ? (
            <Spinner />
          ) : (skills.data || []).length === 0 ? (
            <EmptyState message="Claim some skills first." hint="Interviews draw questions from your tracked skills." />
          ) : (
            <div className="flex flex-wrap gap-2">
              {(skills.data || []).map((s) => (
                <button
                  key={s.skillId}
                  onClick={() =>
                    setSelected((prev) => {
                      const next = new Set(prev)
                      if (next.has(s.skillId)) next.delete(s.skillId)
                      else next.add(s.skillId)
                      return next
                    })
                  }
                  className={`rounded-full px-3 py-1 text-xs font-medium transition ${
                    selected.has(s.skillId)
                      ? 'bg-indigo-600 text-white'
                      : 'bg-slate-200 text-slate-600 hover:bg-slate-300 dark:bg-slate-800 dark:text-slate-300'
                  }`}
                >
                  {s.name}
                </button>
              ))}
            </div>
          )}
          {start.isError && <p className="mt-3 text-sm text-red-600">{(start.error as Error).message}</p>}
          <button
            className="btn-primary mt-4"
            disabled={!targetRole.trim() || selected.size === 0 || start.isPending}
            onClick={() => start.mutate()}
          >
            <ListChecks size={16} /> {start.isPending ? "Preparing…" : `Start interview (${selected.size} skills)`}
          </button>
        </Card>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <h1 className="text-2xl font-bold">Mock interview · session #{session.sessionId}</h1>
      <p className="text-sm text-slate-500">Answer in your own words — each answer is graded and you get instant feedback.</p>

      {session.skippedSkills && session.skippedSkills.length > 0 && (
        <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-700 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-300">
          Skipped (no question content yet): {session.skippedSkills.join(', ')}. Add an AI key in Settings to
          auto-generate questions for them.
        </div>
      )}

      {session.questions.map((q, i) => {
        const r = results[q.id]
        return (
          <Card key={q.id} title={`Q${i + 1} · ${q.skillName}${q.category !== q.skillName ? ` (${q.category})` : ''}`}
            action={r && (
              <span className={`text-xs font-bold ${r.score >= 70 ? 'text-emerald-600' : r.score >= 40 ? 'text-amber-600' : 'text-red-600'}`}>
                {r.score}% · {r.evaluationSource}
              </span>
            )}
          >
            <p className="font-medium">{q.prompt}</p>
            {!r ? (
              <div className="mt-3 space-y-2">
                <textarea
                  className="input min-h-[100px]"
                  placeholder="Type your answer…"
                  value={texts[q.id] || ''}
                  onChange={(e) => setTexts((t) => ({ ...t, [q.id]: e.target.value }))}
                />
                <button
                  className="btn-primary !px-3 !py-1.5 text-xs"
                  disabled={!texts[q.id]?.trim() || answerOne.isPending}
                  onClick={() => answerOne.mutate(q.id)}
                >
                  <Send size={13} /> Submit answer
                </button>
              </div>
            ) : (
              <div className="mt-3 rounded-lg bg-slate-50 p-3 dark:bg-slate-800/60">
                <pre className="whitespace-pre-wrap font-sans text-sm">{texts[q.id]}</pre>
                <p className="mt-2 border-t border-slate-200 pt-2 text-sm dark:border-slate-700">{r.feedback}</p>
                {r.missingConcepts.length > 0 && (
                  <p className="mt-1 text-xs text-amber-600">Missing concepts: {r.missingConcepts.join(', ')}</p>
                )}
              </div>
            )}
          </Card>
        )
      })}

      <button
        className="btn-primary"
        disabled={finish.isPending || Object.keys(results).length === 0}
        onClick={() => finish.mutate()}
      >
        {finish.isPending ? 'Compiling report…' : `Finish interview (${Object.keys(results).length}/${session.questions.length} answered)`}
      </button>
      {finish.isError && <p className="text-sm text-red-600">{(finish.error as Error).message}</p>}
    </div>
  )
}
