export const API_BASE = 'http://127.0.0.1:8080/api'

const requestHeaders = () => ({
  'Content-Type': 'application/json',
  'X-DMHelper-Token': (window as any).electronAPI?.apiToken || ''
})

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

const parseResponse = async (response: Response) => {
  if (!response.ok) {
    const details = await response.text()
    throw new ApiError(
      response.status,
      details || response.statusText || `HTTP ${response.status}`
    )
  }
  if (response.status === 204) return null
  return response.json()
}

export const api = {
  async get(endpoint: string) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      headers: requestHeaders()
    })
    return parseResponse(response)
  },
  
  async post(endpoint: string, data: any) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      method: 'POST',
      headers: requestHeaders(),
      body: JSON.stringify(data)
    })
    return parseResponse(response)
  },

  async put(endpoint: string, data: any) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      method: 'PUT',
      headers: requestHeaders(),
      body: JSON.stringify(data)
    })
    return parseResponse(response)
  },

  async delete(endpoint: string) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      method: 'DELETE',
      headers: requestHeaders()
    })
    return parseResponse(response)
  }
}

export const CharacterAPI = {
  getAll: () => api.get('/characters'),
  getById: (id: number) => api.get(`/characters/${id}`),
  create: (data: any) => api.post('/characters', data),
  update: (id: number, data: any) => api.put(`/characters/${id}`, data),
  equipItem: (id: number, slot: string, itemKey: string) => api.post(`/characters/${id}/equipment`, { slot, itemKey }),
  manageInventory: (id: number, action: string, itemKey: string, quantity: number) => api.post(`/characters/${id}/inventory`, { action, itemKey, quantity }),
  rest: (id: number, restType: string, spentHitDice = 0) => api.post(`/characters/${id}/rest`, { restType, spentHitDice }),
  addXp: (id: number, amount: number, reason: string) => api.post(`/characters/${id}/xp`, { amount, reason }),
  levelUp: (id: number) => api.post(`/characters/${id}/level-up`, {}),
  applyAdvancementChoice: (id: number, type: string, value?: string, values?: string[]) =>
    api.post(`/characters/${id}/advancement-choice`, { type, value, values }),
  manageSpells: (id: number, action: string, spellKey: string) => api.post(`/characters/${id}/spells`, { action, spellKey })
}

export const ItemAPI = {
  search: (query: string) => api.get(`/items?search=${encodeURIComponent(query)}`)
}

export const SpellAPI = {
  getAvailableSpells: (characterId: number, cantrip: boolean) => api.get(`/spells?characterId=${characterId}&cantrip=${cantrip}`)
}

export const CombatAPI = {
  getCurrent: () => api.get('/combat/current'),
  clearCurrent: () => api.delete('/combat/current'),
  getSetup: () => api.get('/combat/setup'),
  start: (characterIds: number[], monsterKeys: string[], adventureSessionId: number | null = null, locationId: number | null = null) =>
    api.post('/combat/sessions', { characterIds, monsterKeys, adventureSessionId, locationId }),
  archivePreview: (sessionId: string) => api.get(`/combat/sessions/${sessionId}/archive`),
  archive: (sessionId: string, entry: import('../adventureTypes').CombatArchiveEntry) => api.post(`/combat/sessions/${sessionId}/archive`, entry),
  getState: (sessionId: string) => api.get(`/combat/sessions/${sessionId}`),
  deploy: (sessionId: string, combatantId: string, x: number, y: number, speedFt?: number, obstacle = false, footprint?: number, terrain?: string) =>
    api.post(`/combat/sessions/${sessionId}/deployment`, { combatantId, x, y, speedFt, obstacle, footprint, terrain }),
  begin: (sessionId: string) => api.post(`/combat/sessions/${sessionId}/begin`, {}),
  move: (sessionId: string, combatantId: string, x: number, y: number) =>
    api.post(`/combat/sessions/${sessionId}/move`, { combatantId, x, y }),
  act: (sessionId: string, optionIndex: number, targetId: string, itemKey = '', aim?: {x:number;y:number}, combatantId?:string) =>
    api.post(`/combat/sessions/${sessionId}/actions`, { optionIndex, targetId, itemKey, aimX:aim?.x, aimY:aim?.y, combatantId }),
  previewArea: (sessionId:string, optionIndex:number, x:number, y:number, combatantId:string) =>
    api.post(`/combat/sessions/${sessionId}/area-preview`, {optionIndex,aimX:x,aimY:y,combatantId}),
  undoMove: (sessionId:string, combatantId:string) => api.post(`/combat/sessions/${sessionId}/undo-move`, {combatantId}),
  tactical: (sessionId:string, action:string, combatantId:string) => api.post(`/combat/sessions/${sessionId}/tactical`, {action,combatantId}),
  reaction: (sessionId:string, offerId:string, accept:boolean) => api.post(`/combat/sessions/${sessionId}/reaction`, {offerId,accept}),
  skip: (sessionId: string, combatantId?:string) => api.post(`/combat/sessions/${sessionId}/skip`, {combatantId}),
  assignLoot: (sessionId: string, receiverCharacterId: number, lootKeys: string[]) =>
    api.post(`/combat/sessions/${sessionId}/loot`, { receiverCharacterId, lootKeys })
}

export const AdventureLogAPI = {
  getAll: () => api.get('/adventure-logs'),
  create: (data: any) => api.post('/adventure-logs', data),
  update: (id: number, data: any) => api.put(`/adventure-logs/${id}`, data),
  delete: (id: number) => api.delete(`/adventure-logs/${id}`)
}

export const AdventureSessionAPI = {
  getAll: (): Promise<import('../adventureTypes').AdventureSession[]> => api.get('/adventure-sessions'),
  start: (title: string, notes: string) => api.post('/adventure-sessions', { title, notes }),
  update: (id: number, title: string, notes: string) => api.put(`/adventure-sessions/${id}`, { title, notes }),
  finish: (id: number) => api.post(`/adventure-sessions/${id}/finish`, {})
}

export const AdventureRecapAPI = {
  getAll: (): Promise<import('../adventureTypes').AdventureRecapSummary[]> => api.get('/adventure-recaps'),
  get: (id: number): Promise<import('../adventureTypes').AdventureRecap> => api.get(`/adventure-recaps/${id}`),
  create: (data: {title:string;content:string;mode:import('../aiTypes').AiAssistantMode;model:string;focus:string;adventure_session_id:number|null;context_snapshot:string}): Promise<import('../adventureTypes').AdventureRecap> => api.post('/adventure-recaps', data),
  update: (id: number, title: string, content: string): Promise<import('../adventureTypes').AdventureRecap> => api.put(`/adventure-recaps/${id}`, { title, content })
}

export const AtlasAPI = {
  locations: () => api.get('/locations'),
  createLocation: (data: unknown) => api.post('/locations', data),
  updateLocation: (id: number, data: unknown) => api.put(`/locations/${id}`, data),
  deleteLocation: (id: number) => api.delete(`/locations/${id}`),
  maps: () => api.get('/maps'),
  map: (id: number) => api.get(`/maps/${id}`),
  createMap: (data: unknown) => api.post('/maps', data),
  renameMap: (id: number, name: string) => api.put(`/maps/${id}`, { name }),
  deleteMap: (id: number) => api.delete(`/maps/${id}`),
  markers: (id: number) => api.get(`/maps/${id}/markers`),
  saveMarker: (id: number, data: unknown) => api.put(`/maps/${id}/markers`, data),
  deleteMarker: (id: number, locationId: number) => api.delete(`/maps/${id}/markers/${locationId}`)
}
