import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { KeyRound, ChevronDown, MailCheck } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import type { AiSetup } from '../api/types'
import { ErrorBanner } from '../components/ui'
import { PitchCard } from '../components/PitchCard'

const PROVIDERS = [
  { value: '', label: 'No AI key (deterministic grading only)' },
  { value: 'OPENAI', label: 'OpenAI', base: 'https://api.openai.com/v1', model: 'gpt-4o-mini', needsKey: true },
  { value: 'GROQ', label: 'Groq (fast + generous free tier)', base: 'https://api.groq.com/openai/v1', model: 'llama-3.3-70b-versatile', needsKey: true },
  { value: 'OPENROUTER', label: 'OpenRouter', base: 'https://openrouter.ai/api/v1', model: 'openai/gpt-4o-mini', needsKey: true },
  { value: 'OLLAMA', label: 'Ollama (local, free)', base: 'http://localhost:11434/v1', model: 'llama3.2', needsKey: false },
  { value: 'CUSTOM', label: 'Custom OpenAI-compatible endpoint', base: '', model: '', needsKey: false },
]

export default function Register() {
  const { register } = useAuth()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showAi, setShowAi] = useState(false)
  const [provider, setProvider] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [model, setModel] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [sentTo, setSentTo] = useState<string | null>(null)

  const preset = PROVIDERS.find((p) => p.value === provider)

  function pickProvider(v: string) {
    setProvider(v)
    const p = PROVIDERS.find((x) => x.value === v)
    if (p) {
      setBaseUrl(p.base ?? '')
      setModel(p.model ?? '')
    }
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const ai: AiSetup | undefined = showAi && provider
        ? { provider, apiKey: apiKey || null, baseUrl: baseUrl || null, model: model || null }
        : undefined
      await register(name, email, password, ai)
      setSentTo(email)
    } catch (err: any) {
      setError(err.message || 'Registration failed')
    } finally {
      setBusy(false)
    }
  }

  if (sentTo) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <div className="card w-full max-w-sm space-y-4 text-center">
          <MailCheck size={40} className="mx-auto text-indigo-500" />
          <h1 className="text-xl font-bold">Check your inbox</h1>
          <p className="text-sm text-slate-500">
            We sent a verification link to <span className="font-medium text-slate-700 dark:text-slate-200">{sentTo}</span>.
            Click it to activate your account, then sign in.
          </p>
          <p className="text-xs text-slate-400">The link expires in 24 hours.</p>
          <Link className="btn-primary block w-full" to="/login">Go to sign in</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <h1 className="mb-8 text-center text-2xl font-bold text-indigo-600">Create your SkillProof account</h1>
        <form onSubmit={submit} className="card space-y-4">
          <ErrorBanner message={error} />
          <div>
            <label className="mb-1 block text-sm font-medium">Name</label>
            <input className="input" required minLength={2} value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Email</label>
            <input className="input" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Password</label>
            <input className="input" type="password" required minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} />
            <p className="mt-1 text-xs text-slate-400">Min 8 chars with upper, lower and a digit.</p>
          </div>

          <div className="rounded-lg border border-slate-200 dark:border-slate-700">
            <button
              type="button"
              onClick={() => setShowAi(!showAi)}
              className="flex w-full items-center justify-between px-3 py-2.5 text-left"
            >
              <span className="flex items-center gap-2 text-sm font-medium">
                <KeyRound size={15} className="text-indigo-500" /> Bring your own AI key
              </span>
              <ChevronDown size={16} className={`text-slate-400 transition ${showAi ? 'rotate-180' : ''}`} />
            </button>
            {showAi && (
              <div className="space-y-3 border-t border-slate-200 p-3 dark:border-slate-700">
                <p className="text-xs text-slate-500">
                  Optional. Your key is stored encrypted and used only for your AI features — grading and
                  auto-generating questions for any skill. You pay your provider directly; SkillProof pays nothing.
                </p>
                <select className="input" value={provider} onChange={(e) => pickProvider(e.target.value)}>
                  {PROVIDERS.map((p) => (
                    <option key={p.value} value={p.value}>{p.label}</option>
                  ))}
                </select>
                {provider && preset?.needsKey && (
                  <div>
                    <label className="mb-1 block text-xs font-medium">API key</label>
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
                    <input
                      className="input"
                      placeholder="https://your-host/v1"
                      value={baseUrl}
                      onChange={(e) => setBaseUrl(e.target.value)}
                    />
                  </div>
                )}
                {(provider === 'CUSTOM' || provider === 'OLLAMA') && (
                  <div>
                    <label className="mb-1 block text-xs font-medium">Model</label>
                    <input className="input" placeholder="model name" value={model} onChange={(e) => setModel(e.target.value)} />
                  </div>
                )}
              </div>
            )}
          </div>

          <button className="btn-primary w-full" disabled={busy}>
            {busy ? 'Creating…' : 'Create account'}
          </button>
          <p className="text-center text-sm text-slate-500">
            Already registered? <Link className="font-medium text-indigo-600 hover:underline" to="/login">Sign in</Link>
          </p>
        </form>
        <PitchCard />
      </div>
    </div>
  )
}
