<script setup lang="ts">
import { nextTick, ref } from 'vue'
import TitleBar from './components/TitleBar.vue'
import AiAssistant from './components/AiAssistant.vue'
import StartupSplash from './components/StartupSplash.vue'

const showingStartup = ref(true)
const navigation = ref<HTMLElement | null>(null)

async function finishStartup(skipped: boolean) {
  showingStartup.value = false
  if (skipped) {
    await nextTick()
    navigation.value?.querySelector<HTMLElement>('.router-link-active')?.focus()
  }
}
</script>

<template>
  <div class="app-shell" :inert="showingStartup">
    <TitleBar />
    <nav ref="navigation" class="app-nav" aria-label="主导航">
      <router-link to="/roster" class="nav-item">角色酒馆</router-link>
      <router-link to="/combat" class="nav-item">战斗场地</router-link>
      <router-link to="/sessions" class="nav-item">跑团场次</router-link>
      <router-link to="/journal" class="nav-item">冒险日志</router-link>
      <router-link to="/atlas" class="nav-item">冒险地图</router-link>
      <router-link to="/ai-settings" class="nav-item">AI 配置</router-link>
    </nav>
    <main class="app-content">
      <router-view />
    </main>
    <AiAssistant />
  </div>
  <StartupSplash v-if="showingStartup" @complete="finishStartup" />
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.app-nav {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--page-gutter);
  background: var(--surface-panel);
}

.nav-item {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: .875rem;
  font-weight: 600;
  padding: var(--space-sm) 12px;
  border-radius: var(--radius-sm);
  transition: background-color var(--transition-fast);
}

.nav-item:hover {
  color: var(--text-secondary);
  background: var(--surface-hover);
}

.nav-item.router-link-active {
  color: var(--antique-gold);
  background: var(--surface-selected);
}

.app-content {
  flex: 1;
  overflow-y: auto;
}
</style>
