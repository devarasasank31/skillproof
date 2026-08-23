import type { ReactNode } from 'react'

export function Card({ title, children, action }: { title?: string; children: ReactNode; action?: ReactNode }) {
  return (
    <div className="card">
      {(title || action) && (
        <div className="mb-3 flex items-center justify-between">
          {title && <h3 className="text-sm font-semibold text-slate-500">{title}</h3>}
          {action}
        </div>
      )}
      {children}
    </div>
  )
}

export function StatCard({ label, value, sub }: { label: string; value: ReactNode; sub?: string }) {
  return (
    <div className="card">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-3xl font-bold">{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
    </div>
  )
}

export function ProgressBar({ value, color = 'bg-indigo-500' }: { value: number; color?: string }) {
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
      <div
        className={`h-full rounded-full transition-all ${color}`}
        style={{ width: `${Math.max(0, Math.min(100, value))}%` }}
      />
    </div>
  )
}

const STATE_STYLES: Record<string, string> = {
  STRONG: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400',
  MASTERED: 'bg-violet-100 text-violet-700 dark:bg-violet-500/10 dark:text-violet-400',
  LEARNING: 'bg-blue-100 text-blue-700 dark:bg-blue-500/10 dark:text-blue-400',
  STALE: 'bg-amber-100 text-amber-700 dark:bg-amber-500/10 dark:text-amber-400',
  AT_RISK: 'bg-orange-100 text-orange-700 dark:bg-orange-500/10 dark:text-orange-400',
  WEAK: 'bg-red-100 text-red-700 dark:bg-red-500/10 dark:text-red-400',
  OVERCLAIMED: 'bg-rose-100 text-rose-700 dark:bg-rose-500/10 dark:text-rose-400',
  NEW: 'bg-slate-200 text-slate-600 dark:bg-slate-500/10 dark:text-slate-400',
}

export function StateBadge({ state }: { state: string }) {
  return (
    <span className={`inline-flex rounded-md px-2 py-0.5 text-xs font-semibold ${STATE_STYLES[state] || STATE_STYLES.NEW}`}>
      {state.replace('_', ' ')}
    </span>
  )
}

export const STATE_EXPLANATION: Record<string, string> = {
  STRONG: 'High confidence backed by assessments and recent usage.',
  MASTERED: 'Consistently strong knowledge with practical proof.',
  LEARNING: 'Actively building evidence for this skill.',
  STALE: 'Predicted retention has dropped below 60% - time to review.',
  AT_RISK: 'Retention or recent usage is slipping.',
  WEAK: 'Low overall confidence across all evidence sources.',
  OVERCLAIMED:
    'Low-evidence claim: you claim this skill but measured knowledge is below the threshold with no practical proof. Build proof or remove it from your resume until you can defend it.',
  NEW: 'Claimed but not yet measured. Take an assessment to calibrate.',
}

export function EmptyState({ message, hint }: { message: string; hint?: string }) {
  return (
    <div className="card flex flex-col items-center justify-center py-12 text-center">
      <p className="text-sm font-medium text-slate-600 dark:text-slate-300">{message}</p>
      {hint && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
    </div>
  )
}

export function Spinner() {
  return (
    <div className="flex items-center justify-center py-16">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-500 border-t-transparent" />
    </div>
  )
}

export function ErrorBanner({ message }: { message: string | null | undefined }) {
  if (!message) return null
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-400">
      {message}
    </div>
  )
}

export function fmtDate(iso: string | null | undefined) {
  if (!iso) return '-'
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}
