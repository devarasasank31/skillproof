import { FormEvent, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { MailCheck } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { api } from '../api/client'
import { ErrorBanner } from '../components/ui'
import { PitchCard } from '../components/PitchCard'

const API_BASE = `${import.meta.env.VITE_API_URL ?? ''}/api`
// Captured when the app bundle starts executing, so we can show honest page-load time.
const BOOT_AT = performance.now()

type ServerStatus = 'probing' | 'ready'

export default function Login() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [code, setCode] = useState<string | null>(null)
  const [resent, setResent] = useState(false)
  const [busy, setBusy] = useState(false)

  // ---- load timing UI ----
  const [serverStatus, setServerStatus] = useState<ServerStatus>('probing')
  const [wakeSeconds, setWakeSeconds] = useState(0)
  const [serverMs, setServerMs] = useState<number | null>(null)
  const [pageMs, setPageMs] = useState<number | null>(null)
  const [busySeconds, setBusySeconds] = useState(0)
  const wakeStartRef = useRef<number>(Date.now())

  // Probe the backend immediately. If Render put it to sleep, keep showing a live timer
  // until it answers instead of appearing broken.
  useEffect(() => {
    let cancelled = false
    wakeStartRef.current = Date.now()
    setPageMs(performance.now() - BOOT_AT)
    const ticker = window.setInterval(() => {
      if (!cancelled) setWakeSeconds((Date.now() - wakeStartRef.current) / 1000)
    }, 100)

    async function probe() {
      while (!cancelled) {
        const t0 = performance.now()
        try {
          const res = await fetch(`${API_BASE}/health`, { cache: 'no-store' })
          // 502/503/504 mean the gateway answered but the app is still booting.
          if (res.status === 502 || res.status === 503 || res.status === 504) throw new Error('waking')
          if (cancelled) return
          setServerMs(performance.now() - t0)
          setServerStatus('ready')
          window.clearInterval(ticker)
          return
        } catch {
          if (cancelled) return
          await new Promise(r => setTimeout(r, 1500))
        }
      }
    }
    probe()
    return () => {
      cancelled = true
      window.clearInterval(ticker)
    }
  }, [])

  // Live timer on the sign-in button while the request is in flight.
  useEffect(() => {
    if (!busy) {
      setBusySeconds(0)
      return
    }
    const t0 = Date.now()
    const id = window.setInterval(() => setBusySeconds((Date.now() - t0) / 1000), 100)
    return () => window.clearInterval(id)
  }, [busy])

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setCode(null)
    setResent(false)
    try {
      await login(email, password)
    } catch (err: any) {
      setError(err.message || 'Login failed')
      setCode(err.code || null)
    } finally {
      setBusy(false)
    }
  }

  async function resend() {
    setBusy(true)
    try {
      await api('/auth/resend-verification', {
        method: 'POST',
        body: JSON.stringify({ email }),
      })
      setResent(true)
      setError(null)
    } catch (err: any) {
      setError(err.message || 'Could not send email')
    } finally {
      setBusy(false)
    }
  }

  const slowWake = serverStatus === 'probing' && wakeSeconds > 3

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-bold text-indigo-600">SkillProof</h1>
          <p className="mt-1 text-sm text-slate-500">Do you actually know it?</p>
        </div>

        {/* Live connection status + timers */}
        <div
          className={`mb-4 rounded-lg border p-3 text-center text-sm ${
            serverStatus === 'ready'
              ? 'border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
              : 'border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
          }`}
          role="status"
          aria-live="polite"
        >
          {serverStatus === 'ready' ? (
            <>
              <p className="font-medium">
                Connected — server responded in{' '}
                <b>{serverMs !== null ? (serverMs >= 1000 ? `${(serverMs / 1000).toFixed(1)}s` : `${Math.round(serverMs)}ms`) : '…'}</b>
              </p>
              <p className="mt-0.5 text-xs opacity-80">
                Page loaded in <b>{pageMs !== null ? `${(pageMs / 1000).toFixed(2)}s` : '…'}</b>
                {' · '}total <b>{((pageMs ?? 0) + (serverMs ?? 0) >= 1000 ? `${(((pageMs ?? 0) + (serverMs ?? 0)) / 1000).toFixed(2)}s` : `${Math.round((pageMs ?? 0) + (serverMs ?? 0))}ms`)}</b>
              </p>
            </>
          ) : (
            <>
              <p className="font-medium">
                Waking server… <b>{wakeSeconds.toFixed(1)}s</b>
              </p>
              <p className="mt-0.5 text-xs opacity-80">
                {slowWake
                  ? 'Free hosting sleeps after idle periods — it boots once, then everything is fast.'
                  : 'Checking connection…'}
              </p>
            </>
          )}
        </div>

        <form onSubmit={submit} className="card space-y-4">
          <ErrorBanner message={error} />
          {code === 'EMAIL_NOT_VERIFIED' && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm dark:border-amber-700 dark:bg-amber-900/30">
              <div className="flex items-start gap-2">
                <MailCheck size={18} className="mt-0.5 shrink-0 text-amber-600" />
                <div>
                  <p className="font-medium">Verify your email to sign in</p>
                  <p className="mt-0.5 text-slate-500 dark:text-slate-400">
                    We emailed you a link when you registered.
                  </p>
                  {resent ? (
                    <p className="mt-2 font-medium text-emerald-600">Sent! Check your inbox.</p>
                  ) : (
                    <button type="button" onClick={resend} disabled={busy}
                      className="mt-2 font-medium text-indigo-600 hover:underline disabled:opacity-50">
                      Resend verification email
                    </button>
                  )}
                </div>
              </div>
            </div>
          )}
          <div>
            <label className="mb-1 block text-sm font-medium">Email</label>
            <input className="input" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Password</label>
            <input className="input" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <button className="btn-primary w-full" disabled={busy}>
            {busy ? `Signing in… ${busySeconds.toFixed(1)}s` : 'Sign in'}
          </button>
          <button
            type="button"
            className="btn-secondary w-full"
            onClick={() => {
              setEmail('demo@skillproof.dev')
              setPassword('Demo1234!')
            }}
          >
            Fill demo credentials
          </button>
          <p className="text-center text-sm text-slate-500">
            No account? <Link className="font-medium text-indigo-600 hover:underline" to="/register">Register</Link>
          </p>
        </form>
        <PitchCard />
      </div>
    </div>
  )
}