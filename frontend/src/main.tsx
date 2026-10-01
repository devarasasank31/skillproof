import React, { Suspense, lazy } from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './hooks/useAuth'
import Layout from './components/Layout'
import './index.css'

// Route-level code splitting keeps the initial download small; the shell (auth + layout)
// ships up front and each page loads on demand.
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))
const Verify = lazy(() => import('./pages/Verify'))
const Dashboard = lazy(() => import('./pages/Dashboard'))
const Skills = lazy(() => import('./pages/Skills'))
const SkillDetail = lazy(() => import('./pages/SkillDetail'))
const Reviews = lazy(() => import('./pages/Reviews'))
const Challenges = lazy(() => import('./pages/Challenges'))
const ChallengeDetail = lazy(() => import('./pages/ChallengeDetail'))
const Jobs = lazy(() => import('./pages/Jobs'))
const Interview = lazy(() => import('./pages/Interview'))
const Analytics = lazy(() => import('./pages/Analytics'))
const GitHubPage = lazy(() => import('./pages/GitHubPage'))
const Profile = lazy(() => import('./pages/Profile'))
const Settings = lazy(() => import('./pages/Settings'))

const qc = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      // Reuse cached data briefly so navigating between pages feels instant.
      staleTime: 30_000,
    },
  },
})

function FullPageLoader() {
  return (
    <div className="app-loader" role="status" aria-live="polite">
      Loading…
    </div>
  )
}

function Protected({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <FullPageLoader />
  if (!user) return <Navigate to="/login" replace />
  return children
}

function PublicOnly({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <FullPageLoader />
  if (user) return <Navigate to="/dashboard" replace />
  return children
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={qc}>
      <BrowserRouter>
        <AuthProvider>
          <Suspense fallback={<FullPageLoader />}>
            <Routes>
              <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
              <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />
              <Route path="/verify" element={<Verify />} />
              <Route element={<Protected><Layout /></Protected>}>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/skills" element={<Skills />} />
                <Route path="/skills/:id" element={<SkillDetail />} />
                <Route path="/reviews" element={<Reviews />} />
                <Route path="/challenges" element={<Challenges />} />
                <Route path="/challenges/:id" element={<ChallengeDetail />} />
                <Route path="/jobs" element={<Jobs />} />
                <Route path="/interview" element={<Interview />} />
                <Route path="/analytics" element={<Analytics />} />
                <Route path="/github" element={<GitHubPage />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/settings" element={<Settings />} />
              </Route>
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </Suspense>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
)