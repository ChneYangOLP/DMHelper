export const API_BASE = 'http://localhost:8080/api'

export const api = {
  async get(endpoint: string) {
    try {
      const response = await fetch(`${API_BASE}${endpoint}`)
      if (!response.ok) throw new Error(`API error: ${response.statusText}`)
      return await response.json()
    } catch (e) {
      console.error(e)
      return null
    }
  },
  
  async post(endpoint: string, data: any) {
    try {
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      })
      if (!response.ok) throw new Error(`API error: ${response.statusText}`)
      return await response.json()
    } catch (e) {
      console.error(e)
      return null
    }
  },

  async put(endpoint: string, data: any) {
    try {
      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      })
      if (!response.ok) throw new Error(`API error: ${response.statusText}`)
      return await response.json()
    } catch (e) {
      console.error(e)
      return null
    }
  }
}

export const CharacterAPI = {
  getAll: () => api.get('/characters'),
  getById: (id: number) => api.get(`/characters/${id}`),
  create: (data: any) => api.post('/characters', data),
  update: (id: number, data: any) => api.put(`/characters/${id}`, data),
  equipItem: (id: number, slot: string, itemKey: string) => api.post(`/characters/${id}/equipment`, { slot, itemKey }),
  manageInventory: (id: number, action: string, itemKey: string, quantity: number) => api.post(`/characters/${id}/inventory`, { action, itemKey, quantity }),
  rest: (id: number, restType: string) => api.post(`/characters/${id}/rest`, { restType }),
  addXp: (id: number, amount: number, reason: string) => api.post(`/characters/${id}/xp`, { amount, reason }),
  levelUp: (id: number) => api.post(`/characters/${id}/level-up`, {}),
  manageSpells: (id: number, action: string, spellKey: string) => api.post(`/characters/${id}/spells`, { action, spellKey })
}

export const ItemAPI = {
  search: (query: string) => api.get(`/items?search=${encodeURIComponent(query)}`)
}

export const SpellAPI = {
  getAvailableSpells: (job: string, level: number, cantrip: boolean) => api.get(`/spells?job=${encodeURIComponent(job)}&level=${level}&cantrip=${cantrip}`)
}
