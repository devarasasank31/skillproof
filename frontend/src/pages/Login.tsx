import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { MailCheck } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { api } from '../api/client'
import { ErrorBanner } from '../components/ui'

export default function Login() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [code, setCode] = useState<string | null>(null)
  const [resent, setResent] = useState(false)
  const [busy, setBusy] = useState(false)

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

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-bold text-indigo-600">SkillProof</h1>
          <p className="mt-1 text-sm text-slate-500">Do you actually know it?</p>
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
            {busy ? 'Signing in…' : 'Sign in'}
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
      </div>
    </div>
  )
}
