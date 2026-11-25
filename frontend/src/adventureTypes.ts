import type { AiAssistantMode } from './aiTypes'

export interface AdventureSession {
  id: number
  title: string
  notes: string
  status: 'ACTIVE' | 'COMPLETED'
  started_at: string
  ended_at: string | null
  log_count: number
  recap_count: number
}
export type AdventureSessionScope = number | 'all' | 'unassigned'
export interface AdventureRecapSummary {
  id: number
  title: string
  mode: AiAssistantMode
  model: string
  focus: string
  adventure_session_id: number | null
  session_title: string | null
  created_at: string
  updated_at: string
}
export interface AdventureRecap extends AdventureRecapSummary {
  content: string
  context_snapshot: string
}
export interface CombatArchiveEntry {
  id?: number
  title: string
  content: string
  adventure_session_id: number | null
  location_id: number | null
}
