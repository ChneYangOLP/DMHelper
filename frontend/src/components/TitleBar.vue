<script setup lang="ts">
const minimize = () => {
  ;(window as any).electronAPI?.minimizeWindow()
}

const maximize = () => {
  ;(window as any).electronAPI?.maximizeWindow()
}

const close = () => {
  ;(window as any).electronAPI?.closeWindow()
}
</script>

<template>
  <header class="title-bar">
    <div class="title-bar__left">
      <span class="title-bar__icon">⚔️</span>
      <span class="title-bar__title">DMHelper</span>
    </div>
    <div class="title-bar__right">
      <button
        class="title-bar__btn"
        aria-label="最小化"
        @click="minimize"
      >
        <span class="title-bar__btn-icon">─</span>
      </button>
      <button
        class="title-bar__btn"
        aria-label="最大化"
        @click="maximize"
      >
        <span class="title-bar__btn-icon">□</span>
      </button>
      <button
        class="title-bar__btn title-bar__btn--close"
        aria-label="关闭"
        @click="close"
      >
        <span class="title-bar__btn-icon">✕</span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  width: 100%;
  padding: 0 var(--space-sm);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border-bottom: 1px solid transparent;
  border-image: linear-gradient(
    90deg,
    transparent 0%,
    var(--antique-gold-dim) 30%,
    var(--antique-gold) 50%,
    var(--antique-gold-dim) 70%,
    transparent 100%
  ) 1;
  -webkit-app-region: drag;
  user-select: none;
  position: relative;
  z-index: 1000;
  flex-shrink: 0;
}

.title-bar__left {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.title-bar__icon {
  font-size: 1rem;
  line-height: 1;
  animation: subtleFloat 3s ease-in-out infinite;
}

.title-bar__title {
  font-family: var(--font-display);
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--antique-gold);
  letter-spacing: 0.06em;
  text-shadow:
    0 1px 3px rgba(0, 0, 0, 0.6),
    0 0 10px rgba(201, 168, 76, 0.15);
}

.title-bar__right {
  display: flex;
  align-items: center;
  gap: 2px;
  -webkit-app-region: no-drag;
}

.title-bar__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 28px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
  padding: 0;
  -webkit-app-region: no-drag;
}

.title-bar__btn:hover {
  background: var(--parchment-light);
  color: var(--text-primary);
  animation: none;
  box-shadow: none;
}

.title-bar__btn:active {
  background: var(--parchment-lighter);
  transform: none;
}

.title-bar__btn--close:hover {
  background: var(--dragon-blood);
  color: #ffffff;
}

.title-bar__btn--close:active {
  background: var(--dragon-blood-bright);
}

.title-bar__btn-icon {
  font-size: 0.75rem;
  font-weight: 400;
  line-height: 1;
}
</style>
