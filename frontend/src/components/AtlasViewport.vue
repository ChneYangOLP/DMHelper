<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import type { AdventureLocation, MapMarker } from '../atlasTypes'

const props = defineProps<{
  image: string
  markers: MapMarker[]
  locations: AdventureLocation[]
  selectedId: number | null
  placing: boolean
}>()
const emit = defineEmits<{ select: [id: number]; place: [point: { x: number; y: number }] }>()
const viewport = ref<HTMLDivElement>()
const natural = ref({ width: 1, height: 1 })
const size = ref({ width: 1, height: 1 })
const loaded = ref(false)
const imageError = ref(false)
const zoom = ref(1)
const offset = ref({ x: 0, y: 0 })
const dragging = ref(false)
let gesture: { id: number; x: number; y: number; offsetX: number; offsetY: number } | null = null
let observer: ResizeObserver | undefined
const fit = computed(() => Math.min(size.value.width / natural.value.width, size.value.height / natural.value.height) * .9)
const dimensions = computed(() => ({ width: natural.value.width * fit.value * zoom.value, height: natural.value.height * fit.value * zoom.value }))
const planeStyle = computed(() => ({ width: `${dimensions.value.width}px`, height: `${dimensions.value.height}px`, transform: `translate(${offset.value.x}px,${offset.value.y}px)` }))
const pins = computed(() => props.markers.flatMap(marker => {
  const location = props.locations.find(location => location.id === marker.location_id)
  return location ? [{ ...marker, location }] : []
}))
function reset() {
  zoom.value = 1
  offset.value = { x: (size.value.width - dimensions.value.width) / 2, y: (size.value.height - dimensions.value.height) / 2 }
}
function imageLoaded(event: Event) {
  const image = event.target as HTMLImageElement
  natural.value = { width: image.naturalWidth, height: image.naturalHeight }
  loaded.value = true
  reset()
}
function zoomAt(factor: number, x = size.value.width / 2, y = size.value.height / 2) {
  const next = Math.min(5, Math.max(.5, zoom.value * factor))
  const ratio = next / zoom.value
  offset.value = { x: x - (x - offset.value.x) * ratio, y: y - (y - offset.value.y) * ratio }
  zoom.value = next
}
function wheel(event: WheelEvent) {
  const rect = viewport.value!.getBoundingClientRect()
  zoomAt(event.deltaY < 0 ? 1.12 : 1 / 1.12, event.clientX - rect.left, event.clientY - rect.top)
}
function pointerDown(event: PointerEvent) {
  if (event.button !== 0 || (event.target as Element).closest('button') || !loaded.value) return
  gesture = { id: event.pointerId, x: event.clientX, y: event.clientY, offsetX: offset.value.x, offsetY: offset.value.y }
  viewport.value!.setPointerCapture(event.pointerId)
}
function pointerMove(event: PointerEvent) {
  if (!gesture || gesture.id !== event.pointerId) return
  const dx = event.clientX - gesture.x, dy = event.clientY - gesture.y
  if (Math.hypot(dx, dy) > 4) dragging.value = true
  if (dragging.value) offset.value = { x: gesture.offsetX + dx, y: gesture.offsetY + dy }
}
function pointerUp(event: PointerEvent) {
  if (!gesture || gesture.id !== event.pointerId) return
  if (!dragging.value && props.placing && event.type === 'pointerup') {
    const rect = viewport.value!.getBoundingClientRect()
    const x = (event.clientX - rect.left - offset.value.x) / dimensions.value.width
    const y = (event.clientY - rect.top - offset.value.y) / dimensions.value.height
    if (x >= 0 && x <= 1 && y >= 0 && y <= 1) emit('place', { x, y })
  }
  gesture = null
  dragging.value = false
  if (viewport.value?.hasPointerCapture(event.pointerId)) viewport.value.releasePointerCapture(event.pointerId)
}
watch(() => props.image, () => { loaded.value = false; imageError.value = false })
onMounted(() => {
  observer = new ResizeObserver(entries => {
    const rect = entries[0]?.contentRect
    if (rect) { size.value = { width: rect.width, height: rect.height }; reset() }
  })
  observer.observe(viewport.value!)
})
onUnmounted(() => observer?.disconnect())
</script>

<template>
  <div class="map-shell">
    <div class="map-tools">
      <span>{{ placing ? '点击地图放置标记 · 拖动仍可浏览' : '拖动浏览 · 滚轮缩放 · 点击标记查看地点' }}</span>
      <div><button aria-label="缩小地图" @click="zoomAt(1 / 1.25)">−</button><output>{{ Math.round(zoom * 100) }}%</output><button aria-label="放大地图" @click="zoomAt(1.25)">＋</button><button @click="reset">适应窗口</button></div>
    </div>
    <div ref="viewport" :class="['viewport', { placing, dragging }]" aria-label="冒险地图画布"
      @wheel.prevent="wheel" @pointerdown="pointerDown" @pointermove="pointerMove" @pointerup="pointerUp" @pointercancel="pointerUp" @lostpointercapture="gesture = null; dragging = false">
      <div class="plane" :style="planeStyle">
        <img :src="image" alt="冒险地图" draggable="false" @load="imageLoaded" @error="imageError = true">
        <button v-for="pin in pins" v-show="loaded" :key="pin.location_id"
          :class="['pin', pin.location.status.toLowerCase(), { selected: pin.location_id === selectedId, current: pin.location.is_current }]"
          :style="{ left: `${pin.x * 100}%`, top: `${pin.y * 100}%` }"
          :aria-label="`查看地点：${pin.location.name}`" :aria-pressed="pin.location_id === selectedId" @click.stop="emit('select', pin.location_id)">
          <span class="pin-dot">{{ pin.location.is_current ? '★' : '◆' }}</span><span class="pin-label">{{ pin.location.name }}</span>
        </button>
      </div>
      <p v-if="imageError" class="canvas-message">地图图片无法读取，请重新导入。</p>
      <p v-else-if="!loaded" class="canvas-message">正在展开地图…</p>
      <div class="compass" aria-hidden="true">N<br>↑</div>
    </div>
  </div>
</template>

<style scoped>
.map-shell { height: 100%; min-height: 480px; display: flex; flex-direction: column; background: var(--surface-overlay); border-radius: 12px; overflow: hidden; }
.map-tools { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 10px 12px; background: var(--surface-card); color: var(--text-secondary); font-size: 12px; flex-wrap: wrap; }
.map-tools>div { display: flex; align-items: center; gap: 6px; }
.map-tools button { border-radius: 5px; padding: 5px 9px; cursor: pointer; }
.map-tools output { width: 42px; text-align: center; font-variant-numeric: tabular-nums; }
.viewport { position: relative; flex: 1; min-height: 430px; overflow: hidden; touch-action: none; cursor: grab; background: var(--surface-inset); }
.viewport.placing { cursor: crosshair; }
.viewport.dragging { cursor: grabbing; }
.plane { position: absolute; top: 0; left: 0; transform-origin: 0 0; }
.plane img { display: block; width: 100%; height: 100%; user-select: none; pointer-events: none; }
.pin { position: absolute; display: flex; align-items: center; flex-direction: column; transform: translate(-50%,-15px); padding: 0; cursor: pointer; color: var(--text-primary); z-index: 1; --control-bg: transparent; --control-hover: transparent; }
.pin-dot { display: grid; place-items: center; width: 28px; height: 28px; background: var(--surface-card); border-radius: 50%; font-size: 13px; box-shadow: var(--overlay-shadow); }
.pin.visited .pin-dot { background: var(--surface-success); color: var(--text-success); }
.pin.planned .pin-dot { background: var(--surface-selected); color: var(--antique-gold); }
.pin.selected { z-index: 3; }
.pin.selected .pin-dot { outline-offset: 2px; outline: 2px solid var(--antique-gold); }
.pin.current .pin-dot { color: var(--antique-gold); }
.pin-label { margin-top: 5px; padding: 3px 7px; white-space: nowrap; border-radius: 4px; background: var(--surface-overlay); color: var(--text-primary); font-size: 12px; max-width: 160px; overflow: hidden; text-overflow: ellipsis; }
.canvas-message { position: absolute; inset: 45% 0 auto; text-align: center; color: var(--antique-gold); }
.compass { position: absolute; right: 18px; bottom: 16px; color: var(--text-secondary); text-align: center; pointer-events: none; font: 18px Georgia; }
.pin:not(.selected):hover .pin-dot { background: var(--surface-hover); }
</style>
