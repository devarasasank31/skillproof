export interface AuthTokens {
  userId: number
  name: string
  email: string
  hasAiKey?: boolean
  accessToken: string
  refreshToken: string
}

export interface RegisterResponse {
  needsVerification: boolean
  message: string
}

export interface AiStatus {
  provider: string | null
  maskedKey: string | null
  baseUrl: string | null
  model: string | null
}

export interface AiSetup {
  provider?: string | null
  apiKey?: string | null
  baseUrl?: string | null
  model?: string | null
}

export interface Profile {
  id: number
  name: string
  email: string
  headline: string | null
  bio: string | null
  visibility: 'PRIVATE' | 'PUBLIC'
  stats: { skills: number; assessments: number; challenges: number; repos: number }
}

export interface SkillRow {
  id: number
  skillId: number
  name: string
  category: string
  claimSource: string
  state: string
  confidence: number
  knowledge: number
  practical: number
  activity: number
  market: number
  retention: number
  nextReviewAt: string | null
  lastActivityAt: string | null
}

export interface EvidenceRow {
  type: string
  description: string
  points: number
  occurredAt: string
}

export interface SnapshotRow {
  snapshotAt: string
  confidence: number
  knowledge: number
  practical: number
  activity: number
  market: number
}

export interface ReviewRow {
  reviewId: number
  dueAt: string
  status: string
  score: number | null
}

export interface SkillDetail {
  skill: SkillRow
  evidence: EvidenceRow[]
  snapshots: SnapshotRow[]
  reviews: ReviewRow[]
}

export interface CatalogItem {
  id: number
  name: string
  category: string
}

export interface AssessmentQuestion {
  id: number
  type: string
  difficulty: string
  prompt: string
  options: string[] | null
}

export interface StartedAssessment {
  assessmentId: number
  source: string
  questions: AssessmentQuestion[]
}

export interface AnswerResult {
  score: number
  correct: boolean | null
  evaluationSource: string
  feedback: string
  missingConcepts: string[]
}

export interface CompletedResult {
  assessmentId: number
  score: number
  answered: number
  total: number
}

export interface DueReview {
  reviewId: number
  skillId: number
  skillName: string
  dueAt: string
}

export interface Challenge {
  id: number
  slug: string
  title: string
  skillName: string
  type: string
  difficulty: string
  estMinutes: number
  prompt?: string
  rubric?: string
}

export interface SubmissionResult {
  id: number
  score: number
  correctness: number
  completeness: number
  bestPractices: number
  checksPassed: number
  checksTotal: number
  feedback: string
}

export interface JobSkillRow {
  name: string
  required: boolean
  confidence: number | null
}

export interface JobAnalyzeResult {
  jobId: number
  title: string
  readiness: number
  skills: JobSkillRow[]
  gaps: JobSkillRow[]
}

export interface JobSummary {
  jobId: number
  title: string
  company: string | null
  readiness: number | null
  createdAt: string
}

export interface MarketResponse {
  totalJobs: number
  rows: { name: string; frequency: number; totalJobs: number; yourConfidence: number | null }[]
}

export interface DashboardData {
  greeting: string
  date: string
  readiness: number
  retentionAvg: number
  atRisk: { id: number; name: string; confidence: number; state: string; trend: string }[]
  nextBestAction: {
    actionType: string
    title: string
    reason: string
    effortMinutes: number | null
    skillName: string | null
  } | null
  totalSkills: number
  dueReviews: number
  openRecommendations: number
  dueToday: { reviewId: number; skillId: number; skillName: string }[]
}

export interface RecommendationDto {
  id: number
  actionType: string
  title: string
  reason: string
  priority: number
  effortMinutes: number | null
  skillName: string | null
  status: string
}

export interface AnalyticsData {
  categories: { category: string; avgConfidence: number; count: number }[]
  topSkillTrends: { skillName: string; points: { snapshotAt: string; confidence: number }[] }[]
  readiness: number
  strongest: number
  weakestName: string | null
  weakestConfidence: number
}

export interface InterviewQuestionRow {
  id: number
  category: string
  skillName: string
  prompt: string
  score: number | null
}

export interface InterviewStart {
  sessionId: number
  status: string
  questions: InterviewQuestionRow[]
  skippedSkills?: string[] | null
}

export interface InterviewAnswerResult {
  score: number
  evaluationSource: string
  feedback: string
  missingConcepts: string[]
}

export interface InterviewReport {
  sessionId: number
  overallScore: number
  perSkill: { skillName: string; score: number }[]
  weakest: string | null
  plan: string[]
}

export interface GitHubRepo {
  name: string
  description: string
  primaryLanguage: string
  languages: string[]
  topics: string[]
  pushedAt: string | null
}

export interface GitHubAnalyze {
  username: string
  publicRepos: number
  repos: GitHubRepo[]
  mappedSkills: { skillName: string; repoCount: number }[]
}

export interface ResumeAnalyze {
  fileName: string
  pages: number
  detected: { name: string; skillId: number | null }[]
}

export interface ResumeConfirm {
  added: number
  addedNames: string[]
  skipped: string[]
}
