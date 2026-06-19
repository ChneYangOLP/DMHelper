<script setup lang="ts">
import TitleBar from './components/TitleBar.vue'
</script>

<template>
  <div class="app-shell">
    <TitleBar />
    <nav class="app-nav">
      <router-link to="/roster" class="nav-item">角色一览</router-link>
      <router-link to="/combat" class="nav-item">战斗追踪器</router-link>
    </nav>
    <main class="app-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(61, 48, 36, 0.4) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(139, 26, 26, 0.06) 0%, transparent 40%),
    radial-gradient(ellipse at 50% 100%, rgba(42, 33, 24, 0.8) 0%, transparent 60%),
    linear-gradient(180deg, var(--parchment-dark) 0%, var(--parchment-darkest) 100%);
}

.app-nav {
  display: flex;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-lg);
  background: rgba(15, 12, 8, 0.6);
  border-bottom: 1px solid rgba(201, 168, 76, 0.2);
}

.nav-item {
  color: var(--text-secondary);
  text-decoration: none;
  font-family: var(--font-heading);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.nav-item:hover, .nav-item.router-link-active {
  color: var(--antique-gold-bright);
  background: rgba(201, 168, 76, 0.1);
}

.app-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-base), transform var(--transition-base);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
