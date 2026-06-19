import { createRouter, createWebHashHistory } from 'vue-router'
import CombatTracker from '../components/CombatTracker.vue'

// We will create these components shortly
const CharacterRoster = () => import('../components/CharacterRoster.vue')
const CharacterCreator = () => import('../components/CharacterCreator.vue')
const CharacterManager = () => import('../components/CharacterManager.vue')

const routes = [
  { path: '/', redirect: '/roster' },
  { path: '/roster', component: CharacterRoster },
  { path: '/create', component: CharacterCreator },
  { path: '/manage/:id', component: CharacterManager, props: true },
  { path: '/combat', component: CombatTracker }
]

export const router = createRouter({
  history: createWebHashHistory(), // Hash history works better for Electron file:// protocol
  routes
})
