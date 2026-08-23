import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, clearTokens, getAccess, setAuthFailureHandler, setTokens } from '../api/client'
import type { AiSetup, AuthTokens, Profile, RegisterResponse } from '../api/types'

interface AuthContextValue {
  user: Profile | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string, ai?: AiSetup) => Promise<RegisterResponse>
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue>(null!)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<Profile | null>(null)
  const [loading, setLoading] = useState(true)

  const loadProfile = useCallback(async () => {
    if (!getAccess()) {
      setUser(null)
      setLoading(false)
      return
    }
    try {
      const p = await api<Profile>('/profile')
      setUser(p)
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    setAuthFailureHandler(() => {
      setUser(null)
      window.location.href = '/login'
    })
    loadProfile()
  }, [loadProfile])

  const login = useCallback(async (email: string, password: string) => {
    const t = await api<AuthTokens>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    setTokens(t.accessToken, t.refreshToken)
    await loadProfile()
  }, [loadProfile])

  const register = useCallback(async (name: string, email: string, password: string, ai?: AiSetup) => {
    const r = await api<RegisterResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password, ai: ai && ai.provider ? ai : null }),
    })
    return r
  }, [])

  const logout = useCallback(async () => {
    try {
      await api('/auth/logout', { method: 'POST' })
    } catch {}
    clearTokens()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser: loadProfile }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
