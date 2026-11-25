export interface AdventureLocation {
  id: number
  name: string
  kind: 'TOWN' | 'DUNGEON' | 'CAMP' | 'LANDMARK' | 'OTHER'
  description: string
  status: 'UNKNOWN' | 'VISITED' | 'PLANNED'
  is_current: boolean
}
export interface AdventureMap { id: number; name: string; image_data?: string | null }
export interface MapMarker { location_id: number; x: number; y: number }
export const locationKinds = { TOWN: '城镇', DUNGEON: '地下城', CAMP: '营地', LANDMARK: '地标', OTHER: '其他' }
export const explorationStates = { UNKNOWN: '未探索', PLANNED: '计划前往', VISITED: '已探索' }
