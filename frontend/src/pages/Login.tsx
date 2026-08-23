import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ErrorBanner } from '../components/ui'

export default function Login() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
    } catch (err: any) {
      setError(err.message || 'Login failed')
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
