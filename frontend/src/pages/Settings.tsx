import { FormEvent, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Trash2 } from 'lucide-react'
import { api } from '../api/client'
import type { AiSetup, AiStatus } from '../api/types'
import { useAuth } from '../hooks/useAuth'
import { useTheme } from '../components/Layout'
import { Card, Spinner } from '../components/ui'

const PROVIDERS = [
  { value: 'OPENAI', label: 'OpenAI', base: 'https://api.openai.com/v1', model: 'gpt-4o-mini', needsKey: true },
  { value: 'GROQ', label: 'Groq (fast + generous free tier)', base: 'https://api.groq.com/openai/v1', model: 'llama-3.3-70b-versatile', needsKey: true },
  { value: 'OPENROUTER', label: 'OpenRouter', base: 'https://openrouter.ai/api/v1', model: 'openai/gpt-4o-mini', needsKey: true },
  { value: 'OLLAMA', label: 'Ollama (local, free)', base: 'http://localhost:11434/v1', model: 'llama3.2', needsKey: false },
  { value: 'CUSTOM', label: 'Custom OpenAI-compatible endpoint', base: '', model: '', needsKey: false },
]

export default function Settings() {
  const { dark, toggle } = useTheme()
  const { user } = useAuth()
  const qc = useQueryClient()

  const ai = useQuery({ queryKey: ['ai'], queryFn: () => api<AiStatus>('/profile/ai') })

  const [provider, setProvider] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [model, setModel] = useState('')

  const preset = PROVIDERS.find((p) => p.value === provider)

  function pickProvider(v: string) {
    setProvider(v)
    const p = PROVIDERS.find((x) => x.value === v)
    if (p) {
      setBaseUrl(p.base)
      setModel(p.model)
    }
  }

  const save = useMutation({
    mutationFn: (setup: AiSetup) => api<AiStatus>('/profile/ai', { method: 'PUT', body: JSON.stringify(setup) }),
    onSuccess: () => {
      setApiKey('')
      qc.invalidateQueries({ queryKey: ['ai'] })
    },
  })

  const remove = useMutation({
    mutationFn: () => api<AiStatus>('/profile/ai', { method: 'DELETE' }),
    onSuccess: () => {
      setProvider('')
      qc.invalidateQueries({ queryKey: ['ai'] })
    },
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    save.mutate({ provider, apiKey: apiKey || null, baseUrl: baseUrl || null, model: model || null })
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold">Settings</h1>

      <Card title="AI key (bring your own)">
        {ai.isLoading ? (
          <Spinner />
        ) : (
          <>
            <div className="mb-4 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2 text-sm">
                <KeyRound size={15} className="text-indigo-500" />
                {ai.data?.provider ? (
                  <span>
                    <span className="font-medium">{ai.data.provider}</span>
                    {ai.data.maskedKey && <span className="text-slate-500"> · key {ai.data.maskedKey}</span>}
                    {ai.data.baseUrl && <span className="text-slate-500"> · {ai.data.baseUrl}</span>}
                    {ai.data.model && <span className="text-slate-500"> · {ai.data.model}</span>}
                  </span>
                ) : (
                  <span className="text-slate-500">
                    Not configured — deterministic grading works without it; AI grading and auto-generated
                    questions for any skill need a key.
                  </span>
                )}
              </div>
              {ai.data?.provider && (
                <button
                  onClick={() => remove.mutate()}
                  disabled={remove.isPending}
                  className="btn-secondary shrink-0 text-red-600"
                  title="Remove AI configuration"
                >
                  <Trash2 size={14} /> Remove
                </button>
              )}
            </div>

            <form onSubmit={submit} className="space-y-3 border-t border-slate-200 pt-4 dark:border-slate-700">
              <p className="text-xs text-slate-500">
                Stored encrypted. Calls go straight to your provider on your bill — SkillProof never proxies or
                sees usage beyond success/failure.
              </p>
              <select className="input" value={provider} onChange={(e) => pickProvider(e.target.value)} required>
                <option value="" disabled>Choose a provider…</option>
                {PROVIDERS.map((p) => (
                  <option key={p.value} value={p.value}>{p.label}</option>
                ))}
              </select>
              {preset?.needsKey && (
                <div>
                  <label className="mb-1 block text-xs font-medium">
                    API key {ai.data?.maskedKey && <span className="font-normal text-slate-400">(leave blank to keep current)</span>}
                  </label>
                  <input
                    className="input"
                    type="password"
                    placeholder="sk-…"
                    value={apiKey}
                    onChange={(e) => setApiKey(e.target.value)}
                  />
                </div>
              )}
              {provider === 'CUSTOM' && (
                <div>
                  <label className="mb-1 block text-xs font-medium">Base URL</label>
                  <input className="input" placeholder="https://your-host/v1" value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} />
                </div>
              )}
              {(provider === 'CUSTOM' || provider === 'OLLAMA') && (
                <div>
                  <label className="mb-1 block text-xs font-medium">Model</label>
                  <input className="input" placeholder="model name" value={model} onChange={(e) => setModel(e.target.value)} />
                </div>
              )}
              <button className="btn-primary" disabled={!provider || save.isPending}>
                {save.isPending ? 'Saving…' : 'Save AI configuration'}
              </button>
              {(save.isError || remove.isError) && (
                <p className="text-sm text-red-600">{((save.error || remove.error) as Error)?.message}</p>
              )}
              {save.isSuccess && !save.isPending && (
                <p className="text-sm text-emerald-600">Saved.</p>
              )}
            </form>
          </>
        )}
      </Card>

      <Card title="Appearance">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium">Dark mode</p>
            <p className="text-xs text-slate-500">Easier on the eyes for late-night grinding.</p>
          </div>
          <button onClick={toggle} className={`relative h-6 w-11 rounded-full transition ${dark ? 'bg-indigo-600' : 'bg-slate-300'}`}>
            <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition-all ${dark ? 'left-[22px]' : 'left-0.5'}`} />
          </button>
        </div>
      </Card>

      <Card title="Account">
        <dl className="space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-500">Signed in as</dt>
            <dd className="font-medium">{user?.email}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Profile visibility</dt>
            <dd className="font-medium">{user?.visibility}</dd>
          </div>
        </dl>
      </Card>

      <Card title="About SkillProof">
        <p className="text-sm text-slate-600 dark:text-slate-300">
          SkillProof measures whether you actually know what your resume claims — through spaced assessments,
          practical challenges, activity evidence, and a memory model that decays confidence over time.
        </p>
        <p className="mt-2 text-xs text-slate-400">Confidence = 10% claim + 30% knowledge + 30% practical + 20% activity + 10% market</p>
      </Card>
    </div>
  )
}
