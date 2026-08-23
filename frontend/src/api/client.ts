const BASE = '/api'

export function getAccess() {
  return localStorage.getItem('sp.access')
}

export function setTokens(access: string, refresh: string) {
  localStorage.setItem('sp.access', access)
  localStorage.setItem('sp.refresh', refresh)
}

export function clearTokens() {
  localStorage.removeItem('sp.access')
  localStorage.removeItem('sp.refresh')
}

let onAuthFailure: (() => void) | null = null
export function setAuthFailureHandler(fn: () => void) {
  onAuthFailure = fn
}

async function tryRefresh(): Promise<boolean> {
  const refresh = localStorage.getItem('sp.refresh')
  if (!refresh) return false
  const res = await fetch(`${BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: refresh }),
  })
  if (!res.ok) return false
  const data = await res.json()
  setTokens(data.accessToken, data.refreshToken)
  return true
}

export async function api<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  }
  const access = getAccess()
  if (access) headers['Authorization'] = `Bearer ${access}`

  const res = await fetch(`${BASE}${path}`, { ...options, headers })

  if (res.status === 401 && retry && !path.startsWith('/auth/')) {
    const refreshed = await tryRefresh()
    if (refreshed) return api<T>(path, options, false)
    clearTokens()
    onAuthFailure?.()
  }

  if (!res.ok) {
    let message = res.statusText
    let code = 'HTTP_' + res.status
    try {
      const err = await res.json()
      message = err.message || message
      code = err.code || code
    } catch {}
    const e = new Error(message) as Error & { code?: string; status?: number }
    e.code = code
    e.status = res.status
    throw e
  }
  if (res.status === 204) return undefined as T
  return res.json()
}

export async function apiUpload<T>(path: string, form: FormData): Promise<T> {
  const access = getAccess()
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: access ? { Authorization: `Bearer ${access}` } : {},
    body: form,
  })
  if (!res.ok) {
    let message = res.statusText
    try {
      const err = await res.json()
      message = err.message || message
    } catch {}
    throw new Error(message)
  }
  return res.json()
}
