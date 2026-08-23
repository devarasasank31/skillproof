import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import {
  AreaChart, Area, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { ArrowLeft, CheckCircle2, XCircle } from 'lucide-react'
import { api } from '../api/client'
import type {
  AnswerResult, AssessmentQuestion, CompletedResult, SkillDetail as SkillDetailData,
  StartedAssessment,
} from '../api/types'
import { Card, EmptyState, ProgressBar, Spinner, StateBadge, STATE_EXPLANATION, fmtDate } from '../components/ui'

const BAR_COLORS: Record<string, string> = {
  Knowledge: 'bg-sky-500',
  Practical: 'bg-emerald-500',
  Activity: 'bg-violet-500',
  Market: 'bg-amber-500',
}

export default function SkillDetail() {
  const { id } = useParams()
  const qc = useQueryClient()
  const q = useQuery({
    queryKey: ['skill', id],
    queryFn: () => api<SkillDetailData>(`/skills/${id}`),
    enabled: !!id,
  })

  const [assessmentId, setAssessmentId] = useState<number | null>(null)
  const [questions, setQuestions] = useState<AssessmentQuestion[]>([])
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [results, setResults] = useState<Record<number, AnswerResult>>({})
  const [submitting, setSubmitting] = useState(false)
  const [completed, setCompleted] = useState<CompletedResult | null>(null)
  const [count, setCount] = useState(5)

  const start = useMutation({
    mutationFn: (source?: 'MCQ' | 'MIXED') =>
      api<StartedAssessment>(`/skills/${id}/assess`, {
        method: 'POST',
        body: JSON.stringify({ count, ...(source ? { source } : {}) }),
      }),
    onSuccess: (data) => {
      setAssessmentId(data.assessmentId)
      setQuestions(data.questions)
      setAnswers({})
      setResults({})
      setCompleted(null)
    },
  })

  async function submitAll() {
    if (!assessmentId) return
    setSubmitting(true)
    try {
      for (const question of questions) {
        const text = answers[question.id]
        if (!text) continue
        const r = await api<AnswerResult>(`/assessments/${assessmentId}/answers`, {
          method: 'POST',
          body: JSON.stringify({ questionId: question.id, answerText: text }),
        })
        setResults((prev) => ({ ...prev, [question.id]: r }))
      }
      const done = await api<CompletedResult>(`/assessments/${assessmentId}/complete`, { method: 'POST' })
      setCompleted(done)
      qc.invalidateQueries()
    } finally {
      setSubmitting(false)
    }
  }

  if (q.isLoading) return <Spinner />
  if (q.isError || !q.data) return <EmptyState message="Skill not found." />
  const s = q.data.skill

  return (
    <div className="space-y-6">
      <div>
        <Link to="/skills" className="mb-2 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-indigo-600">
          <ArrowLeft size={14} /> All skills
        </Link>
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-bold">{s.name}</h1>
          <StateBadge state={s.state} />
          <span className="text-sm text-slate-500">{s.category}</span>
          <span className="text-xs text-slate-400">claim source: {s.claimSource.replace('_', ' ').toLowerCase()}</span>
        </div>
        <p className="mt-1 text-sm text-slate-500">{STATE_EXPLANATION[s.state]}</p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-5">
        <Card title="Overall confidence">
          <p className="text-4xl font-bold">{s.confidence}%</p>
          <div className="mt-3"><ProgressBar value={s.confidence} /></div>
          <dl className="mt-4 space-y-2 text-sm">
            {[
              ['Knowledge', s.knowledge],
              ['Practical', s.practical],
              ['Activity', s.activity],
              ['Market', s.market],
            ].map(([label, v]) => (
              <div key={label as string}>
                <div className="flex justify-between text-xs text-slate-500">
                  <dt>{label}</dt><dd>{v}%</dd>
                </div>
                <ProgressBar value={v as number} color={BAR_COLORS[label as string]} />
              </div>
            ))}
          </dl>
          <p className="mt-4 text-xs text-slate-400">retention ~{Math.round(s.retention * 100)}% · next review {fmtDate(s.nextReviewAt)}</p>
        </Card>

        <div className="space-y-4 lg:col-span-4">
          <Card title="Confidence over time">
            {(!q.data.snapshots || q.data.snapshots.length === 0) ? (
              <EmptyState message="No history yet — take an assessment to create your first snapshot." />
            ) : (
              <div style={{ width: '100%', height: 220 }}>
                <ResponsiveContainer>
                  <AreaChart data={q.data.snapshots.map((sn) => ({ ...sn, date: fmtDate(sn.snapshotAt) }))}>
                    <defs>
                      <linearGradient id="confGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#6366f1" stopOpacity={0.35} />
                        <stop offset="100%" stopColor="#6366f1" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#8884" />
                    <XAxis dataKey="date" fontSize={11} />
                    <YAxis domain={[0, 100]} fontSize={11} />
                    <Tooltip />
                    <Area type="monotone" dataKey="confidence" stroke="#6366f1" fill="url(#confGrad)" strokeWidth={2} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            )}
          </Card>

          {completed && (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700 dark:border-emerald-500/20 dark:bg-emerald-500/5 dark:text-emerald-400">
              Assessment complete: scored {completed.score}% ({completed.answered}/{completed.total} answered). Evidence recorded and confidence recalculated.
            </div>
          )}

          <Card title="Calibrate this skill" action={
            <div className="flex items-center gap-2">
              <select
                className="input !w-auto !px-2 !py-1.5 text-xs"
                value={count}
                onChange={(e) => setCount(Number(e.target.value))}
                title="How many questions to attempt"
              >
                {[3, 5, 8, 10, 15].map((n) => (
                  <option key={n} value={n}>{n} questions</option>
                ))}
              </select>
              <button className="btn-primary !px-3 !py-1.5 text-xs" onClick={() => start.mutate('MCQ')} disabled={start.isPending}>
                {start.isPending ? 'Starting…' : 'Start assessment'}
              </button>
              <button className="btn-secondary !px-3 !py-1.5 text-xs" onClick={() => start.mutate()} disabled={start.isPending}>
                Default mix
              </button>
            </div>
          }>
            {start.isError && <p className="mb-2 text-sm text-red-600">{(start.error as Error).message}</p>}
            {!assessmentId ? (
              <p className="text-sm text-slate-500">Run a quick assessment to measure your knowledge score. MCQs are graded instantly; open questions are keyword/AI-evaluated.</p>
            ) : (
              <div className="space-y-4">
                {questions.map((question, idx) => {
                  const r = results[question.id]
                  return (
                    <div key={question.id} className="rounded-lg border border-slate-200 p-4 dark:border-slate-800">
                      <div className="mb-2 flex items-center justify-between text-xs text-slate-400">
                        <span>Q{idx + 1} · {question.difficulty} · {question.type}</span>
                        {r && (
                          <span className={`inline-flex items-center gap-1 font-semibold ${r.correct === false ? 'text-red-600' : 'text-emerald-600'}`}>
                            {r.correct === false ? <XCircle size={13} /> : <CheckCircle2 size={13} />}
                            {r.score}/10 · via {r.evaluationSource}
                          </span>
                        )}
                      </div>
                      <p className="font-medium">{question.prompt}</p>

                      {question.options && question.options.length > 0 ? (
                        <div className="mt-3 grid gap-2 sm:grid-cols-2">
                          {question.options.map((opt, oi) => {
                            const isPicked = answers[question.id] === opt
                            const isCorrect = r?.answerKey != null && r.answerKey === opt
                            return (
                              <button
                                key={oi}
                                disabled={!!r}
                                onClick={() => setAnswers((a) => ({ ...a, [question.id]: opt }))}
                                className={`rounded-lg border px-3 py-2 text-left text-sm transition ${
                                  r
                                    ? isCorrect
                                      ? 'border-emerald-400 bg-emerald-50 font-medium dark:bg-emerald-500/10'
                                      : isPicked
                                        ? 'border-red-300 bg-red-50 dark:bg-red-500/10'
                                        : 'border-slate-200 opacity-70 dark:border-slate-800'
                                    : isPicked
                                      ? 'border-indigo-500 bg-indigo-50 dark:bg-indigo-500/10'
                                      : 'border-slate-200 hover:border-indigo-300 dark:border-slate-700'
                                }`}
                              >
                                {opt}
                                {r && isCorrect && <span className="ml-2 text-xs font-semibold text-emerald-600">✓ correct</span>}
                              </button>
                            )
                          })}
                        </div>
                      ) : (
                        <textarea
                          className="input mt-3 min-h-[90px]"
                          placeholder="Type your answer…"
                          disabled={!!r}
                          value={answers[question.id] || ''}
                          onChange={(e) => setAnswers((a) => ({ ...a, [question.id]: e.target.value }))}
                        />
                      )}

                      {r && (
                        <div className="mt-3 space-y-1.5 rounded-lg bg-slate-50 p-3 text-sm dark:bg-slate-800/60">
                          {r.answerKey && question.type === 'MCQ' && (
                            <p className="font-medium text-emerald-700 dark:text-emerald-400">
                              Correct answer: {r.answerKey}
                            </p>
                          )}
                          {r.explanation && (
                            <p className="text-slate-600 dark:text-slate-300">
                              <span className="font-medium">Why:</span> {r.explanation}
                            </p>
                          )}
                          <p>{r.feedback}</p>
                          {r.missingConcepts.length > 0 && (
                            <p className="text-xs text-amber-600">
                              To improve, cover these concepts: {r.missingConcepts.join(', ')}
                            </p>
                          )}
                        </div>
                      )}
                    </div>
                  )
                })}
                {!completed && (
                  <button
                    className="btn-primary"
                    disabled={submitting || Object.keys(answers).length === 0}
                    onClick={submitAll}
                  >
                    {submitting ? 'Scoring…' : `Submit ${Object.keys(answers).length}/${questions.length} answered`}
                  </button>
                )}
              </div>
            )}
          </Card>

          <Card title="Evidence timeline">
            {(!q.data.evidence || q.data.evidence.length === 0) ? (
              <EmptyState message="No evidence recorded yet." hint="Assessments, challenges, GitHub analysis and activity add evidence here." />
            ) : (
              <ul className="divide-y divide-slate-100 text-sm dark:divide-slate-800">
                {q.data.evidence.map((ev, i) => (
                  <li key={i} className="flex items-center justify-between py-2">
                    <div>
                      <span className="mr-2 rounded bg-slate-200 px-1.5 py-0.5 text-[10px] font-semibold uppercase dark:bg-slate-800">{ev.type}</span>
                      {ev.description}
                    </div>
                    <div className="text-right">
                      <span className="font-semibold text-emerald-600">+{ev.points}</span>
                      <p className="text-xs text-slate-400">{fmtDate(ev.occurredAt)}</p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card title="Review schedule">
            {(!q.data.reviews || q.data.reviews.length === 0) ? (
              <EmptyState message="No reviews scheduled yet." />
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-slate-400">
                    <th className="pb-2">Due</th>
                    <th className="pb-2">Status</th>
                    <th className="pb-2">Score</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {q.data.reviews.map((r) => (
                    <tr key={r.reviewId}>
                      <td className="py-2">{fmtDate(r.dueAt)}</td>
                      <td>{r.status}</td>
                      <td>{r.score ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}
