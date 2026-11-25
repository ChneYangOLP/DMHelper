import { reactive } from 'vue'
import type { AiAssistantMode } from './aiTypes'
import type { AdventureSessionScope } from './adventureTypes'

export const aiPanel = reactive({ open: false, mode: 'recap' as AiAssistantMode, locationId: null as number | null,
  adventureSessionId: 'all' as AdventureSessionScope, recapId: null as number | null })

export function openAiPanel(mode: AiAssistantMode = 'recap', locationId: number | null = null, adventureSessionId?: AdventureSessionScope) {
  aiPanel.mode = mode
  aiPanel.locationId = locationId
  if (adventureSessionId !== undefined) aiPanel.adventureSessionId = adventureSessionId
  aiPanel.recapId = null
  aiPanel.open = true
}

export function openSavedRecap(id: number) { aiPanel.recapId = id; aiPanel.open = true }
export function notifyAdventureHistoryChanged() { window.dispatchEvent(new Event('dmhelper:adventure-history-changed')) }
