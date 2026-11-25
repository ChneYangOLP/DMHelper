<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AdventureLogAPI, AtlasAPI } from '../api'
import { readArtwork } from '../combatAssets'
import { explorationStates, locationKinds } from '../atlasTypes'
import type { AdventureLocation, AdventureMap, MapMarker } from '../atlasTypes'
import AtlasViewport from './AtlasViewport.vue'
import { openAiPanel } from '../aiPanel'

interface RelatedEntry { id: number; location_id: number | null; title: string; entry_type: string; quest_status: string; content: string }
const route = useRoute()
const maps = ref<AdventureMap[]>([])
const locations = ref<AdventureLocation[]>([])
const markers = ref<MapMarker[]>([])
const journal = ref<RelatedEntry[]>([])
const activeMap = ref<AdventureMap | null>(null)
const mapId = ref<number | null>(null)
const selectedId = ref<number | null>(null)
const loading = ref(true)
const mapLoading = ref(false)
const busy = ref(false)
const error = ref('')
const search = ref('')
const statusFilter = ref('ALL')
const onlyOnMap = ref(false)
const placing = ref(false)
const locationEditor = ref(false)
const locationEditingId = ref<number | null>(null)
const emptyLocation = () => ({ name: '', kind: 'OTHER' as AdventureLocation['kind'], description: '', status: 'UNKNOWN' as AdventureLocation['status'], is_current: false })
const locationForm = reactive(emptyLocation())
const mapEditor = ref(false)
const mapEditingId = ref<number | null>(null)
const mapForm = reactive({ name: '', image_data: '', filename: '' })
const imageReading = ref(false)
let mapRequest = 0
const selected = computed(() => locations.value.find(location => location.id === selectedId.value))
const selectedMarker = computed(() => markers.value.find(marker => marker.location_id === selectedId.value))
const related = computed(() => journal.value.filter(entry => entry.location_id === selectedId.value))
const filteredLocations = computed(() => locations.value.filter(location => {
  const query = search.value.trim().toLowerCase()
  return (!query || `${location.name} ${location.description}`.toLowerCase().includes(query))
    && (statusFilter.value === 'ALL' || location.status === statusFilter.value)
    && (!onlyOnMap.value || !activeMap.value || markers.value.some(marker => marker.location_id === location.id))
}))
function message(cause: unknown) { return cause instanceof Error ? cause.message : '操作失败，请重试。' }
async function perform(operation: () => Promise<void>) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await operation() } catch (cause) { error.value = message(cause) } finally { busy.value = false }
}
async function load() {
  loading.value = true; error.value = ''
  try {
    const [mapList, locationList, logs] = await Promise.all([AtlasAPI.maps(), AtlasAPI.locations(), AdventureLogAPI.getAll()])
    maps.value = mapList; locations.value = locationList; journal.value = logs
    const requested = Number(route.query.locationId)
    if (locations.value.some(location => location.id === requested)) selectedId.value = requested
    mapId.value = maps.value[0]?.id ?? null
  } catch (cause) { error.value = message(cause) } finally { loading.value = false }
}
async function loadMap(id: number | null) {
  const request = ++mapRequest
  placing.value = false; activeMap.value = null; markers.value = []
  if (id == null) { mapLoading.value = false; return }
  mapLoading.value = true
  try {
    const [map, pins] = await Promise.all([AtlasAPI.map(id), AtlasAPI.markers(id)])
    if (request === mapRequest) { activeMap.value = map; markers.value = pins }
  } catch (cause) { if (request === mapRequest) error.value = message(cause) }
  finally { if (request === mapRequest) mapLoading.value = false }
}
watch(mapId, loadMap)
watch(selectedId, () => { placing.value = false })
function editLocation(location?: AdventureLocation) {
  locationEditingId.value = location?.id ?? null
  Object.assign(locationForm, location ?? emptyLocation())
  locationEditor.value = true
}
async function saveLocation() {
  await perform(async () => {
    const result = locationEditingId.value == null
      ? await AtlasAPI.createLocation(locationForm)
      : await AtlasAPI.updateLocation(locationEditingId.value, locationForm)
    locations.value = await AtlasAPI.locations()
    selectedId.value = result.id
    locationEditor.value = false
  })
}
async function setCurrent() {
  if (!selected.value) return
  const location = selected.value
  await perform(async () => {
    await AtlasAPI.updateLocation(location.id, { ...location, is_current: !location.is_current })
    locations.value = await AtlasAPI.locations()
  })
}
async function deleteLocation() {
  const location = selected.value
  if (!location || !window.confirm(`删除地点“${location.name}”？地图标记会移除，相关日志会保留地点名称。`)) return
  await perform(async () => {
    await AtlasAPI.deleteLocation(location.id)
    locations.value = locations.value.filter(item => item.id !== location.id)
    markers.value = markers.value.filter(item => item.location_id !== location.id)
    selectedId.value = null
    journal.value = await AdventureLogAPI.getAll()
  })
}
function editMap(map?: AdventureMap) {
  mapEditingId.value = map?.id ?? null
  Object.assign(mapForm, { name: map?.name ?? '', image_data: '', filename: '' })
  mapEditor.value = true
}
async function importImage(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  imageReading.value = true; error.value = ''; mapForm.image_data = ''; mapForm.filename = ''
  try {
    mapForm.image_data = await readArtwork(file, false)
    mapForm.filename = file.name
    if (!mapForm.name.trim()) mapForm.name = file.name.replace(/\.[^.]+$/, '').slice(0, 120)
  } catch (cause) { error.value = message(cause) }
  finally { imageReading.value = false; input.value = '' }
}
async function saveMap() {
  await perform(async () => {
    if (mapEditingId.value == null) {
      if (!mapForm.image_data) throw new Error('请先选择地图图片。')
      const created = await AtlasAPI.createMap({ name: mapForm.name, image_data: mapForm.image_data })
      maps.value = await AtlasAPI.maps()
      mapId.value = created.id
    } else {
      await AtlasAPI.renameMap(mapEditingId.value, mapForm.name)
      maps.value = await AtlasAPI.maps()
      if (activeMap.value?.id === mapEditingId.value) activeMap.value.name = mapForm.name.trim()
    }
    mapEditor.value = false
  })
}
async function deleteMap() {
  const map = activeMap.value
  if (!map || !window.confirm(`删除地图“${map.name}”？这张地图上的标记会移除，所有地点与日志会保留。`)) return
  await perform(async () => {
    await AtlasAPI.deleteMap(map.id)
    maps.value = maps.value.filter(item => item.id !== map.id)
    mapId.value = maps.value[0]?.id ?? null
  })
}
async function place(point: { x: number; y: number }) {
  const id = mapId.value, locationId = selectedId.value
  if (id == null || locationId == null || !placing.value) return
  await perform(async () => {
    const marker = await AtlasAPI.saveMarker(id, { location_id: locationId, ...point })
    if (mapId.value === id) {
      markers.value = [...markers.value.filter(item => item.location_id !== locationId), marker]
      placing.value = false
    }
  })
}
async function removeMarker() {
  const id = mapId.value, locationId = selectedId.value
  if (id == null || locationId == null) return
  await perform(async () => {
    await AtlasAPI.deleteMarker(id, locationId)
    if (mapId.value === id) markers.value = markers.value.filter(item => item.location_id !== locationId)
    placing.value = false
  })
}
onMounted(load)
</script>

<template>
  <main class="atlas-page">
    <header class="atlas-header">
      <div><p class="eyebrow">THE ADVENTURER’S ATLAS</p><h1>冒险地图</h1><p>沿着线索出发，为每一个值得记住的地方留下坐标。</p></div>
      <div class="actions"><button :disabled="loading || busy" @click="editMap()">＋ 导入地图</button><button class="primary" :disabled="loading || busy" @click="editLocation()">＋ 新建地点</button></div>
    </header>
    <div v-if="error" role="alert" class="error-banner">{{ error }}<button aria-label="关闭错误提示" @click="error = ''">×</button></div>
    <p v-if="loading" class="empty">正在整理地图档案…</p>
    <div v-else class="atlas-layout">
      <aside class="location-panel panel">
        <div class="panel-heading"><h2>地点档案</h2><span>{{ locations.length }} 处</span></div>
        <input v-model="search" aria-label="搜索地点" placeholder="搜索地点与描述…">
        <select v-model="statusFilter" aria-label="筛选探索状态"><option value="ALL">全部探索状态</option><option v-for="(label, key) in explorationStates" :key="key" :value="key">{{ label }}</option></select>
        <label v-if="activeMap" class="check"><input v-model="onlyOnMap" type="checkbox">只看本图标记</label>
        <div class="location-list">
          <button v-for="location in filteredLocations" :key="location.id" :class="['location-item', { active: selectedId === location.id }]" @click="selectedId = location.id">
            <span class="location-title"><b>{{ location.is_current ? '★ ' : '' }}{{ location.name }}</b><small v-if="markers.some(pin => pin.location_id === location.id)">⌖</small></span>
            <span class="location-meta">{{ locationKinds[location.kind] }}<span :class="location.status.toLowerCase()">{{ explorationStates[location.status] }}</span></span>
          </button>
          <p v-if="!filteredLocations.length" class="muted empty-small">{{ locations.length ? '没有符合条件的地点。' : '先新建一个地点，再将它标在地图上。' }}</p>
        </div>
      </aside>

      <section class="map-column">
        <div class="map-heading">
          <select v-model="mapId" aria-label="选择地图" :disabled="!maps.length || busy"><option v-if="!maps.length" :value="null">还没有地图</option><option v-for="map in maps" :key="map.id" :value="map.id">{{ map.name }}</option></select>
          <div v-if="activeMap" class="actions"><button :disabled="busy" @click="editMap(activeMap)">重命名</button><button class="danger" :disabled="busy" @click="deleteMap">删除地图</button></div>
        </div>
        <div v-if="mapLoading" class="empty map-empty">正在读取地图…</div>
        <AtlasViewport v-else-if="activeMap?.image_data" :key="activeMap.id" :image="activeMap.image_data" :markers="markers" :locations="locations" :selected-id="selectedId" :placing="placing && !busy" @select="selectedId = $event" @place="place" />
        <div v-else class="empty map-empty"><div class="empty-icon">⌖</div><h2>{{ maps.length ? '地图读取失败' : '下一段旅程，从这里展开' }}</h2><p>导入世界、区域或地下城地图，再添加地点标记。</p><button v-if="maps.length" @click="loadMap(mapId)">重新读取</button><button v-else class="primary" @click="editMap()">导入第一张地图</button><small>支持 PNG / JPG / WebP，单张不超过 10 MB</small></div>
        <footer class="map-legend"><span><i class="unknown"></i>未探索</span><span><i class="planned"></i>计划前往</span><span><i class="visited"></i>已探索</span><span>★ 当前位置</span><span class="storage-note">地图与地点自动保存在本机</span></footer>
      </section>

      <aside class="detail-panel panel">
        <template v-if="selected">
          <p class="eyebrow">LOCATION DOSSIER</p><h2 class="detail-name">{{ selected.name }}</h2>
          <div class="badges"><span>{{ locationKinds[selected.kind] }}</span><span>{{ explorationStates[selected.status] }}</span><span v-if="selected.is_current">★ 当前位置</span></div>
          <p class="description">{{ selected.description || '还没有这个地点的详细记录。' }}</p>
          <div class="detail-actions"><button :disabled="busy" @click="editLocation(selected)">编辑地点</button><button :disabled="busy" @click="setCurrent">{{ selected.is_current ? '取消当前位置' : '设为当前位置' }}</button></div>
          <section class="detail-section"><h3>地图标记</h3><p class="muted">{{ !activeMap ? '先导入或选择一张地图。' : selectedMarker ? '已标记在当前地图，可重新选择位置。' : '这个地点还未标记在当前地图。' }}</p>
            <button class="primary" :disabled="!activeMap || busy" @click="placing = !placing">{{ placing ? '取消放置' : selectedMarker ? '重新放置标记' : '在地图上标记' }}</button>
            <button v-if="selectedMarker" :disabled="busy" @click="removeMarker">移除本图标记</button>
          </section>
          <section class="detail-section"><div class="panel-heading"><h3>关联档案</h3><span>{{ related.length }}</span></div>
            <router-link class="journal-link" :to="{ path: '/journal', query: { locationId: selected.id } }">查看 / 添加地点记录 ↗</router-link>
            <button class="journal-link" type="button" @click="openAiPanel('destination', selected.id)">回顾此地的人和事 ↗</button>
            <router-link v-for="entry in related" :key="entry.id" class="related-entry" :to="{ path: '/journal', query: { locationId: selected.id } }"><small>{{ entry.entry_type === 'QUEST' ? '任务' : entry.entry_type === 'NPC' ? '人物' : '见闻' }}{{ entry.entry_type === 'QUEST' ? ` · ${entry.quest_status === 'ACTIVE' ? '进行中' : entry.quest_status === 'COMPLETED' ? '已完成' : '已失败'}` : '' }}</small><b>{{ entry.title }}</b></router-link>
            <p v-if="!related.length" class="muted">在冒险日志中关联这个地点，就能汇集任务、人物和线索。</p>
          </section>
          <button class="danger delete-location" :disabled="busy" @click="deleteLocation">删除地点</button>
        </template>
        <div v-else class="detail-placeholder"><span>✦</span><h2>每个地方，都有故事</h2><p>选择左侧地点或地图标记，查看探索状态、任务与见闻。</p></div>
      </aside>
    </div>

    <div v-if="locationEditor || mapEditor" class="modal-backdrop" @mousedown.self="!busy && !imageReading && (locationEditor = mapEditor = false)" @keydown.esc="!busy && !imageReading && (locationEditor = mapEditor = false)">
      <form v-if="locationEditor" class="editor" aria-label="地点编辑器" @submit.prevent="saveLocation">
        <p class="eyebrow">LOCATION DOSSIER</p><h2>{{ locationEditingId == null ? '新建地点' : '编辑地点' }}</h2>
        <label>地点名称<input v-model="locationForm.name" required maxlength="120" placeholder="例如：灰港、废弃矿井" autofocus></label>
        <div class="form-row"><label>地点类型<select v-model="locationForm.kind" aria-label="地点类型"><option v-for="(label, key) in locationKinds" :key="key" :value="key">{{ label }}</option></select></label><label>探索状态<select v-model="locationForm.status" aria-label="探索状态"><option v-for="(label, key) in explorationStates" :key="key" :value="key">{{ label }}</option></select></label></div>
        <label>地点描述<textarea v-model="locationForm.description" maxlength="20000" rows="7" placeholder="环境、已知风险、重要线索，以及你希望记住的一切…"></textarea></label>
        <label class="check"><input v-model="locationForm.is_current" type="checkbox">设为队伍当前位置（替换原位置）</label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <footer><button type="button" :disabled="busy" @click="locationEditor = false">取消</button><button class="primary" :disabled="busy">{{ busy ? '保存中…' : '保存地点' }}</button></footer>
      </form>
      <form v-else class="editor" aria-label="地图编辑器" @submit.prevent="saveMap">
        <p class="eyebrow">MAP ARCHIVE</p><h2>{{ mapEditingId == null ? '导入地图' : '重命名地图' }}</h2>
        <label>地图名称<input v-model="mapForm.name" required maxlength="120" placeholder="例如：北境区域图"></label>
        <label v-if="mapEditingId == null" class="file-picker">地图图片<input type="file" accept="image/png,image/jpeg,image/webp" :disabled="imageReading || busy" @change="importImage"><small>{{ imageReading ? '正在处理图片…' : mapForm.filename || '支持 PNG / JPG / WebP，最大 10 MB；长边最多保留 2048 像素。' }}</small></label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <footer><button type="button" :disabled="busy || imageReading" @click="mapEditor = false">取消</button><button class="primary" :disabled="busy || imageReading || (mapEditingId == null && !mapForm.image_data)">{{ busy ? '保存中…' : '保存地图' }}</button></footer>
      </form>
    </div>
  </main>
</template>

<style scoped>
.atlas-page{padding:28px clamp(18px,3vw,44px) 36px;color:var(--text-primary);background:radial-gradient(ellipse at 50% 15%,#35463022,transparent 65%);min-height:100%;box-sizing:border-box}.atlas-header{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin-bottom:22px}.eyebrow{font-size:10px;letter-spacing:.22em;color:#bd9c58;margin:0 0 7px}.atlas-header h1{font:700 38px var(--font-heading);color:#eddfbc;margin:0}.atlas-header p:last-child{color:#a7a496;margin:10px 0 0;font-size:13px}.actions{display:flex;gap:8px;flex-wrap:wrap}button,input,select,textarea{font:inherit}button{cursor:pointer;color:#d5c9a8;background:#272b25;border:1px solid #5b5640;border-radius:6px;padding:8px 11px;font-size:12px}button:hover{border-color:#d3b671;color:#fff0cb}button:disabled{opacity:.45;cursor:default}.primary{color:#211d13;background:linear-gradient(135deg,#dfc477,#ae8743);border-color:#b18b48;font-weight:700}.primary:hover{color:#17120a}.danger{color:#d69a8c;border-color:#6a4239;background:#36231f55}input,select,textarea{box-sizing:border-box;background:#121a18;border:1px solid #4a4c3d;border-radius:6px;color:#e1dac5;padding:9px 10px;min-width:0}input:focus,select:focus,textarea:focus{outline:1px solid #c8a763}.atlas-layout{display:grid;grid-template-columns:220px minmax(300px,1fr) 260px;gap:16px;align-items:start}.panel{background:linear-gradient(145deg,#202722e8,#151b18ed);border:1px solid #56533c;border-radius:10px;padding:16px}.panel-heading{display:flex;justify-content:space-between;align-items:center;gap:8px}.panel-heading h2{font-size:16px;margin:0}.panel-heading span{font-size:11px;color:#aaa48b}.location-panel>input,.location-panel>select{display:block;width:100%;margin-top:12px;font-size:12px}.check{display:flex!important;align-items:center;gap:7px;color:#b9b59f;font-size:12px;margin:12px 0}.check input{width:auto!important;margin:0!important;accent-color:#c7a25c}.location-list{display:flex;flex-direction:column;gap:7px;margin-top:15px;max-height:62vh;overflow:auto}.location-item{display:block;text-align:left;background:#10171480;border-color:#414636;padding:12px 10px}.location-item.active{border-color:#cfb36c;background:#75603530}.location-title{display:flex;justify-content:space-between;gap:8px}.location-title b{overflow-wrap:anywhere}.location-title small{color:#bfa46b}.location-meta{display:flex;justify-content:space-between;gap:6px;margin-top:9px;color:#989d8c;font-size:10px}.planned{color:#e0bc64}.visited{color:#91bba0}.unknown{color:#aaa18b}.map-column{min-width:0;display:flex;flex-direction:column}.map-heading{display:flex;justify-content:space-between;align-items:center;gap:8px;margin-bottom:12px;flex-wrap:wrap}.map-heading select{flex:1;font-weight:700;min-width:120px;max-width:100%}.map-column :deep(.map-shell){height:clamp(480px,66vh,850px)}.empty{padding:50px 20px;text-align:center;color:#b4b39f;border:1px dashed #696445;border-radius:12px}.map-empty{min-height:480px;box-sizing:border-box;display:flex;flex-direction:column;align-items:center;justify-content:center;background:radial-gradient(ellipse,#2c3a2c66,#101816aa);gap:12px}.map-empty h2{color:#e3d5af;font-size:21px;margin:0}.map-empty p{font-size:12px;line-height:1.8;margin:0}.map-empty small{font-size:10px;color:#969a88}.empty-icon{color:#b29d64;font-size:48px}.map-legend{display:flex;gap:14px;flex-wrap:wrap;font-size:10px;color:#a7a58f;padding:12px 0}.map-legend>span{display:flex;align-items:center;gap:5px}.map-legend i{width:6px;height:6px;display:inline-block;border:2px solid currentColor;border-radius:50%}.storage-note{margin-left:auto}.detail-name{font:600 25px var(--font-heading);color:#ecd9ac;margin:0 0 12px;overflow-wrap:anywhere}.badges{display:flex;gap:5px;flex-wrap:wrap}.badges span{padding:4px 6px;font-size:10px;background:#b8a56918;border:1px solid #86754a55;border-radius:4px;color:#c9bc95}.description{font-size:13px;line-height:1.85;color:#bbbba7;white-space:pre-wrap;overflow-wrap:anywhere;max-height:230px;overflow:auto;margin:18px 0}.detail-actions{display:flex;gap:6px;flex-wrap:wrap}.detail-section{border-top:1px solid #6e694133;padding-top:16px;margin-top:18px}.detail-section h3{font-size:13px;color:#ddcba0;margin:0 0 9px}.detail-section button{margin:0 5px 7px 0}.muted{font-size:12px;color:#929c8b;line-height:1.8}.empty-small{padding:18px 3px}.journal-link{font-size:12px;color:#d9bd77;text-decoration:none;display:block;margin-bottom:12px}.related-entry{display:flex;flex-direction:column;gap:5px;text-decoration:none;background:#11191699;padding:10px;border-radius:5px;margin-top:6px;color:#d4d1b9}.related-entry small{color:#959c85;font-size:10px}.related-entry b{font-size:12px;overflow-wrap:anywhere}.delete-location{margin-top:24px;font-size:11px}.detail-placeholder{padding:80px 0;text-align:center;color:#9ca58e}.detail-placeholder>span{font-size:34px;color:#b29d64}.detail-placeholder h2{font-size:16px;color:#d5c59d}.detail-placeholder p{font-size:12px;line-height:1.8}.error-banner{display:flex;justify-content:space-between;align-items:center;background:#642e2766;color:#f1b9aa;border:1px solid #914d3f;border-radius:6px;padding:8px 13px;margin-bottom:16px;font-size:13px}.error-banner button{border:0;background:none}.modal-backdrop{position:fixed;inset:0;z-index:100;display:grid;place-items:center;padding:20px;background:#000b;backdrop-filter:blur(5px)}.editor{box-sizing:border-box;width:min(540px,100%);max-height:calc(100vh - 40px);overflow:auto;padding:26px;background:#1c2520;border:1px solid #a48a51;border-radius:12px;box-shadow:0 25px 90px #0009}.editor h2{margin:0 0 20px;color:#eedbb0}.editor label{display:block;font-size:12px;color:#c2bda6;margin-top:14px}.editor input,.editor select,.editor textarea{display:block;width:100%;margin-top:7px}.form-row{display:grid;grid-template-columns:1fr 1fr;gap:12px}.editor textarea{resize:vertical;line-height:1.8}.editor footer{display:flex;justify-content:flex-end;gap:8px;border-top:1px solid #67603c66;margin-top:24px;padding-top:16px}.file-picker small{display:block;color:#a6ac98;margin-top:10px;font-size:11px;line-height:1.7}.form-error{font-size:12px;color:#f1b9aa;line-height:1.6}@media(min-width:1700px){.atlas-layout{grid-template-columns:250px minmax(400px,1fr) 300px}}@media(max-width:1150px){.atlas-layout{grid-template-columns:200px minmax(300px,1fr)}.detail-panel{grid-column:1/-1}.detail-placeholder{padding:18px 0}.detail-section{display:inline-block;vertical-align:top;width:calc(50% - 16px);margin-right:16px;box-sizing:border-box}}@media(max-width:700px){.atlas-header{align-items:flex-start;flex-direction:column}.atlas-layout{grid-template-columns:minmax(0,1fr)}.location-list{max-height:220px}.detail-panel{grid-column:auto}.detail-section{display:block;width:100%}.atlas-header h1{font-size:30px}.storage-note{margin-left:0}.map-column :deep(.map-shell){height:500px}.form-row{grid-template-columns:1fr}}
</style>
