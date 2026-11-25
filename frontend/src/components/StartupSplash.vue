<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import SplashArtwork from './SplashArtwork.vue'

const emit = defineEmits<{ complete: [skipped: boolean] }>()
const visible = ref(true)
const splash = ref<HTMLElement | null>(null)
let timer: ReturnType<typeof setTimeout> | undefined
let skipped = false

function dismiss(wasSkipped = false) {
  if (!visible.value) return
  skipped = wasSkipped
  clearTimeout(timer)
  visible.value = false
}

onMounted(() => {
  splash.value?.focus({ preventScroll: true })
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  timer = setTimeout(dismiss, reducedMotion ? 1000 : 4350)
})

onUnmounted(() => clearTimeout(timer))
</script>

<template>
  <Transition name="startup" @after-leave="emit('complete', skipped)">
    <section
      v-if="visible"
      ref="splash"
      class="startup-splash"
      role="dialog"
      aria-modal="true"
      aria-label="DMHelper 启动画面"
      tabindex="-1"
      @keydown.esc.prevent="dismiss(true)"
    >
      <div class="splash-scene">
        <SplashArtwork />
      </div>
      <div class="splash-wash" aria-hidden="true"></div>
      <p class="splash-title">DMHelper</p>
      <button class="splash-skip" type="button" @click="dismiss(true)">跳过</button>
    </section>
  </Transition>
</template>

<style scoped>
.startup-splash {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
  isolation: isolate;
  overflow: hidden;
  background: var(--background-gradient);
}

.startup-splash:focus { outline: none; }

.splash-scene,
.splash-wash,
.splash-title {
  grid-area: 1 / 1;
  pointer-events: none;
}

.splash-wash {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 65%, #ffe1a494, #d5974a51 55%, #a86a3733 100%);
  animation: warm-light 1860ms 1350ms ease-in-out both;
}

.splash-title {
  margin: 0;
  padding-inline: var(--space-md);
  color: var(--antique-gold);
  font-family: var(--font-display);
  font-size: clamp(2.85rem, 9vw, 7.5rem);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: .04em;
  text-align: center;
  animation: title-reveal 580ms 2700ms ease-out both;
}

.splash-skip {
  position: absolute;
  top: max(var(--space-md), env(safe-area-inset-top));
  right: max(var(--page-gutter), env(safe-area-inset-right));
  min-width: 60px;
  min-height: 40px;
  --control-bg: var(--surface-inset);
  --control-hover: var(--surface-hover);
  color: var(--text-secondary);
  font-weight: 500;
}

.startup-leave-active {
  transition: opacity 420ms ease;
}

.startup-leave-to { opacity: 0; }

@keyframes warm-light {
  0%, 100% { opacity: 0; }
  51% { opacity: .92; }
}

@keyframes title-reveal {
  from { opacity: 0; text-shadow: 0 0 26px #e0a65862; }
  to { opacity: 1; text-shadow: 0 0 20px #c8a56c33; }
}

@media (prefers-reduced-motion: reduce) {
  .splash-scene,
  .splash-wash { display: none; }

  .splash-title { animation: none; }

  .startup-leave-active { transition: none; }
}
</style>
