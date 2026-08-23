import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react'
import { api } from '../api/client'

type State = 'verifying' | 'ok' | 'error'

export default function Verify() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [state, setState] = useState<State>('verifying')
  const [message, setMessage] = useState('')
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return
    ran.current = true
    if (!token) {
      setState('error')
      setMessage('No verification token in the link.')
      return
    }
    api<{ message: string }>('/auth/verify', {
      method: 'POST',
      body: JSON.stringify({ token }),
    })
      .then((r) => {
        setState('ok')
        setMessage(r.message)
      })
      .catch((err: Error) => {
        setState('error')
        setMessage(err.message)
      })
  }, [token])

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="card w-full max-w-sm space-y-4 text-center">
        {state === 'verifying' && (
          <>
            <Loader2 size={40} className="mx-auto animate-spin text-indigo-500" />
            <h1 className="text-xl font-bold">Verifying your email…</h1>
          </>
        )}
        {state === 'ok' && (
          <>
            <CheckCircle2 size={40} className="mx-auto text-emerald-500" />
            <h1 className="text-xl font-bold">Email verified!</h1>
            <p className="text-sm text-slate-500">{message}</p>
            <Link className="btn-primary block w-full" to="/login">Sign in</Link>
          </>
        )}
        {state === 'error' && (
          <>
            <XCircle size={40} className="mx-auto text-rose-500" />
            <h1 className="text-xl font-bold">Verification failed</h1>
            <p className="text-sm text-slate-500">{message}</p>
            <Link className="btn-primary block w-full" to="/login">Back to sign in</Link>
          </>
        )}
      </div>
    </div>
  )
}
