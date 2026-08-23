import { Bot, FileSearch, BrainCircuit, TrendingDown } from 'lucide-react'

export function PitchCard() {
  return (
    <div className="card mt-4 space-y-3 border-indigo-100 bg-gradient-to-br from-indigo-50 via-white to-violet-50 dark:border-indigo-900/50 dark:from-indigo-950/40 dark:via-slate-900 dark:to-violet-950/30">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-indigo-600 text-white shadow-lg shadow-indigo-300 dark:shadow-none">
          <Bot size={22} />
        </div>
        <div>
          <p className="text-sm font-semibold">What is SkillProof?</p>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Your AI-powered proof-of-skill engine. Not just a badge — evidence.
          </p>
        </div>
      </div>
      <ul className="space-y-1.5 text-xs leading-relaxed text-slate-600 dark:text-slate-300">
        <li className="flex gap-2">
          <FileSearch size={14} className="mt-0.5 shrink-0 text-indigo-500" />
          Upload your resume — AI finds every skill you have, even ones hiding inside projects.
        </li>
        <li className="flex gap-2">
          <BrainCircuit size={14} className="mt-0.5 shrink-0 text-indigo-500" />
          Prove each skill with AI mock interviews, timed quizzes and real-world challenges.
        </li>
        <li className="flex gap-2">
          <TrendingDown size={14} className="mt-0.5 shrink-0 text-indigo-500" />
          Confidence decays if you stop practising — keep proving it or lose it. Like Duolingo, but for your career.
        </li>
      </ul>
    </div>
  )
}
