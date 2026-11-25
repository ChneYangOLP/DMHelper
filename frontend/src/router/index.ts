import { createRouter, createWebHashHistory } from 'vue-router'
import CombatTracker from '../components/CombatTracker.vue'

// We will create these components shortly
const CharacterRoster = () => import('../components/CharacterRoster.vue')
const CharacterCreator = () => import('../components/CharacterCreator.vue')
const CharacterManager = () => import('../components/CharacterManager.vue')
const AdventureJournal = () => import('../components/AdventureJournal.vue')

const routes = [
  { path: '/', redirect: '/roster' },
  { path: '/roster', component: CharacterRoster },
  { path: '/create', component: CharacterCreator },
  { path: '/manage/:id', component: CharacterManager, props: true },
  { path: '/combat', component: CombatTracker },
  { path: '/journal', component: AdventureJournal },
  { path: '/sessions', component: () => import('../components/AdventureSessions.vue') },
  { path: '/atlas', component: () => import('../components/AdventureAtlas.vue') },
  { path: '/ai', redirect: (to: { query: Record<string, unknown> }) => ({ path: '/journal', query: {
    assistant: to.query.mode === 'destination' ? 'destination' : 'recap', locationId: String(to.query.locationId || '')
  } }) },
  { path: '/ai-settings', component: () => import('../components/AiSettings.vue') }
]

export const router = createRouter({
  history: createWebHashHistory(), // Hash history works better for Electron file:// protocol
  routes
})
