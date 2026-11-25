<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AdventureLogAPI, AdventureSessionAPI, ApiError, AtlasAPI, CharacterAPI } from '../api'
import type { AdventureLocation } from '../atlasTypes'
import type { AdventureSession } from '../adventureTypes'

type EntryType = 'EVENT' | 'QUEST' | 'NPC'
type QuestStatus = 'ACTIVE' | 'COMPLETED' | 'FAILED'

interface JournalEntry {
  id: number
  entry_type: EntryType
  title: string
  content: string
  character_id: number | null
  character_name: string | null
  location: string
  location_id: number | null
  tags: string
  quest_status: QuestStatus | ''
  event_date: string
  pinned: boolean
  created_at: string
  updated_at: string
  adventure_session_id: number | null
  session_title: string | null
  combat_source_id: string | null
}

interface CharacterOption { database_id: number; name: string }

const typeMeta: Record<EntryType, { label: string; icon: string; hint: string }> = {
  EVENT: { label: '见闻', icon: '✦', hint: '地点、线索、决定与冒险经过' },
  QUEST: { label: '任务', icon: '◆', hint: '委托、目标、奖励与进展' },
  NPC: { label: '人物', icon: '♟', hint: '身份、关系、特征与情报' }
}
const statusLabels: Record<QuestStatus, string> = { ACTIVE: '进行中', COMPLETED: '已完成', FAILED: '已失败' }

const entries = ref<JournalEntry[]>([])
const route = useRoute()
const locations = ref<AdventureLocation[]>([])
const locationFilter = ref<number | 'ALL'>(Number(route.query.locationId) || 'ALL')
const sessions = ref<AdventureSession[]>([])
const sessionFilter = ref<number | 'ALL' | 'unassigned'>('ALL')
const focusedEntryId = computed(() => Number(route.query.entryId) || null)
watch(() => route.query.adventureSessionId, value => { sessionFilter.value = value === 'unassigned' ? 'unassigned' : Number(value) || 'ALL' }, { immediate: true })
watch(() => route.query.locationId, value => { locationFilter.value = Number(value) || 'ALL' })
const characters = ref<CharacterOption[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const search = ref('')
const typeFilter = ref<'ALL' | EntryType>('ALL')
const characterFilter = ref<number | 'ALL'>('ALL')
const editorOpen = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  entry_type: 'EVENT' as EntryType,
  title: '',
  content: '',
  character_id: null as number | null,
  location: '',
  location_id: null as number | null,
  tags: '',
  quest_status: 'ACTIVE' as QuestStatus,
  event_date: '',
  pinned: false,
  adventure_session_id: null as number | null
})
const form = reactive(emptyForm())

const filteredEntries = computed(() => {
  const query = search.value.trim().toLowerCase()
  return entries.value.filter(entry => {
    if (focusedEntryId.value) return entry.id === focusedEntryId.value
    if (sessionFilter.value === 'unassigned' && entry.adventure_session_id != null) return false
    if (typeof sessionFilter.value === 'number' && entry.adventure_session_id !== sessionFilter.value) return false
    if (typeFilter.value !== 'ALL' && entry.entry_type !== typeFilter.value) return false
    if (characterFilter.value !== 'ALL' && entry.character_id !== characterFilter.value) return false
    if (locationFilter.value !== 'ALL' && entry.location_id !== locationFilter.value) return false
    if (!query) return true
    return `${entry.title} ${entry.content} ${entry.location} ${entry.tags} ${entry.character_name || ''}`.toLowerCase().includes(query)
  })
})

const counts = computed(() => ({
  ALL: entries.value.length,
  EVENT: entries.value.filter(entry => entry.entry_type === 'EVENT').length,
  QUEST: entries.value.filter(entry => entry.entry_type === 'QUEST').length,
  NPC: entries.value.filter(entry => entry.entry_type === 'NPC').length
}))

function message(cause: unknown) {
  return cause instanceof ApiError ? cause.message : cause instanceof Error ? cause.message : '操作失败'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [logs, roster, placeList, sessionList] = await Promise.all([AdventureLogAPI.getAll(), CharacterAPI.getAll(), AtlasAPI.locations(), AdventureSessionAPI.getAll()])
    entries.value = logs || []
    characters.value = roster || []
    locations.value = placeList || []
    sessions.value = sessionList
  } catch (cause) {
    error.value = message(cause)
  } finally {
    loading.value = false
  }
}

function openCreate(type: EntryType = 'EVENT') {
  editingId.value = null
  Object.assign(form, emptyForm(), { entry_type: type })
  form.adventure_session_id = typeof sessionFilter.value === 'number' ? sessionFilter.value
    : sessionFilter.value === 'unassigned' ? null : sessions.value.find(item => item.status === 'ACTIVE')?.id ?? null
  if (locationFilter.value !== 'ALL' && locations.value.some(location => location.id === locationFilter.value)) form.location_id = locationFilter.value
  editorOpen.value = true
}

function openEdit(entry: JournalEntry) {
  editingId.value = entry.id
  Object.assign(form, {
    entry_type: entry.entry_type,
    title: entry.title,
    content: entry.content,
    character_id: entry.character_id,
    location: entry.location,
    location_id: entry.location_id,
    tags: entry.tags,
    quest_status: entry.quest_status || 'ACTIVE',
    event_date: entry.event_date,
    pinned: entry.pinned,
    adventure_session_id: entry.adventure_session_id ?? null
  })
  editorOpen.value = true
}

function closeEditor() {
  if (saving.value) return
  editorOpen.value = false
}

async function saveEntry() {
  if (!form.title.trim()) {
    error.value = '请填写日志标题。'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const payload = { ...form, quest_status: form.entry_type === 'QUEST' ? form.quest_status : '' }
    if (editingId.value == null) await AdventureLogAPI.create(payload)
    else await AdventureLogAPI.update(editingId.value, payload)
    editorOpen.value = false
    await load()
  } catch (cause) {
    error.value = message(cause)
  } finally {
    saving.value = false
  }
}

async function togglePin(entry: JournalEntry) {
  try {
    await AdventureLogAPI.update(entry.id, { ...entry, pinned: !entry.pinned })
    await load()
  } catch (cause) {
    error.value = message(cause)
  }
}

async function cycleQuest(entry: JournalEntry) {
  const next: Record<QuestStatus, QuestStatus> = { ACTIVE: 'COMPLETED', COMPLETED: 'FAILED', FAILED: 'ACTIVE' }
  try {
    await AdventureLogAPI.update(entry.id, { ...entry, quest_status: next[(entry.quest_status || 'ACTIVE') as QuestStatus] })
    await load()
  } catch (cause) {
    error.value = message(cause)
  }
}

async function removeEntry(entry: JournalEntry) {
  if (!window.confirm(`确定删除“${entry.title}”吗？此操作无法撤销。`)) return
  try {
    await AdventureLogAPI.delete(entry.id)
    entries.value = entries.value.filter(item => item.id !== entry.id)
  } catch (cause) {
    error.value = message(cause)
  }
}

function tagsOf(entry: JournalEntry) {
  return entry.tags.split(/[,，]/).map(tag => tag.trim()).filter(Boolean)
}

function formatTime(value: string) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T') + (value.includes('Z') ? '' : 'Z'))
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(load)
</script>

<template>
  <section class="workspace-page">
    <header class="workspace-heading">
      <h1>冒险日志</h1>
      <button class="btn-primary" @click="openCreate()">＋ 新建记录</button>
    </header>

    <div v-if="error" class="error-banner" role="alert">{{ error }}<button aria-label="关闭错误提示" @click="error = ''">×</button></div>

    <section class="toolbar">
      <div class="type-tabs">
        <button :aria-pressed="typeFilter === 'ALL'" :class="{ active: typeFilter === 'ALL' }" @click="typeFilter = 'ALL'">全部 <b>{{ counts.ALL }}</b></button>
        <button v-for="(meta, key) in typeMeta" :key="key" :aria-pressed="typeFilter === key" :class="{ active: typeFilter === key }" @click="typeFilter = key as EntryType">
          {{ meta.icon }} {{ meta.label }} <b>{{ counts[key] }}</b>
        </button>
      </div>
      <div class="filters">
        <input v-model="search" aria-label="搜索冒险日志" placeholder="搜索标题、正文、地点或标签…">
        <select v-model="characterFilter" aria-label="筛选关联角色">
          <option value="ALL">全部角色</option>
          <option v-for="character in characters" :key="character.database_id" :value="character.database_id">{{ character.name }}</option>
        </select>
      </div>
    </section>

    <div class="location-filter-bar">
      <label>关联场次 <select v-model="sessionFilter" aria-label="筛选关联场次"><option value="ALL">全部场次</option><option value="unassigned">未关联场次</option><option v-for="session in sessions" :key="session.id" :value="session.id">{{ session.title }}{{ session.status === 'ACTIVE' ? ' · 进行中' : '' }}</option></select></label>
      <label>关联地点 <select v-model="locationFilter" aria-label="筛选关联地点"><option value="ALL">全部地点</option><option v-for="location in locations" :key="location.id" :value="location.id">{{ location.name }}</option></select></label>
      <router-link v-if="locationFilter !== 'ALL'" :to="{ path: '/atlas', query: { locationId: locationFilter } }">查看地点档案 ↗</router-link>
    </div>
    <p v-if="focusedEntryId" class="focused-note">正在查看记录 #{{ focusedEntryId }}。<router-link to="/journal">返回全部记录 ↗</router-link></p>

    <div v-if="loading" class="empty-state">正在翻阅冒险档案…</div>
    <section v-else-if="filteredEntries.length" class="entry-grid">
      <article v-for="entry in filteredEntries" :key="entry.id" :class="['entry-card', entry.entry_type.toLowerCase(), { pinned: entry.pinned, focused: entry.id === focusedEntryId }]">
        <div class="card-top">
          <span class="type-badge">{{ typeMeta[entry.entry_type].icon }} {{ typeMeta[entry.entry_type].label }}</span>
          <div class="card-actions">
            <button :title="entry.pinned ? '取消置顶' : '置顶'" :aria-pressed="entry.pinned" @click="togglePin(entry)">{{ entry.pinned ? '★' : '☆' }}</button>
            <button title="编辑" @click="openEdit(entry)">✎</button>
            <button class="delete-entry" title="删除" @click="removeEntry(entry)">×</button>
          </div>
        </div>
        <h2>{{ entry.title }}</h2>
        <div class="metadata">
          <router-link v-if="entry.adventure_session_id" :to="{path:'/sessions',query:{sessionId:entry.adventure_session_id}}">▣ {{ entry.session_title }}</router-link>
          <span v-if="entry.combat_source_id">战斗归档</span>
          <span v-if="entry.event_date">◷ {{ entry.event_date }}</span>
          <router-link v-if="entry.location_id" :to="{ path: '/atlas', query: { locationId: entry.location_id } }">⌖ {{ entry.location }}</router-link>
          <span v-else-if="entry.location">⌖ {{ entry.location }}</span>
          <span v-if="entry.character_name">♙ {{ entry.character_name }}</span>
        </div>
        <p class="content">{{ entry.content || '尚未填写详细内容。' }}</p>
        <button v-if="entry.entry_type === 'QUEST'" :class="['quest-status', entry.quest_status.toLowerCase()]" @click="cycleQuest(entry)">
          {{ statusLabels[entry.quest_status as QuestStatus] }} · 点击切换
        </button>
        <div class="card-footer">
          <div class="tags"><span v-for="tag in tagsOf(entry)" :key="tag">#{{ tag }}</span></div>
          <time>更新于 {{ formatTime(entry.updated_at) }}</time>
        </div>
      </article>
    </section>
    <section v-else class="empty-state">
      <h2>{{ focusedEntryId ? '这条原始记录已不存在' : entries.length ? '没有符合筛选条件的记录' : '还没有冒险日志' }}</h2>
      <p>{{ focusedEntryId ? '记录可能已被删除，已保存总结中的资料快照仍可查看。' : entries.length ? '试试更换类型或搜索词。' : '从一段见闻、一个任务或一位人物开始。' }}</p>
      <div v-if="!entries.length" class="empty-actions">
        <button @click="openCreate('EVENT')">记录见闻</button><button @click="openCreate('QUEST')">添加任务</button><button @click="openCreate('NPC')">记下人物</button>
      </div>
    </section>

    <div v-if="editorOpen" class="modal-backdrop" @mousedown.self="closeEditor">
      <form class="editor" @submit.prevent="saveEntry">
        <div class="editor-header"><div><h2>{{ editingId == null ? '写入冒险档案' : '修改日志记录' }}</h2></div><button type="button" aria-label="关闭日志编辑器" @click="closeEditor">×</button></div>
        <div class="type-picker">
          <button v-for="(meta, key) in typeMeta" :key="key" type="button" :aria-pressed="form.entry_type === key" :class="{ active: form.entry_type === key }" @click="form.entry_type = key as EntryType">
            <b>{{ meta.icon }} {{ meta.label }}</b><small>{{ meta.hint }}</small>
          </button>
        </div>
        <label class="full">标题<input v-model="form.title" maxlength="120" autofocus placeholder="给这段记录一个容易找到的名字" required></label>
        <div class="form-grid">
          <label class="full">关联场次<select v-model="form.adventure_session_id"><option :value="null">未关联场次</option><option v-for="session in sessions" :key="session.id" :value="session.id">{{ session.title }}{{ session.status === 'ACTIVE' ? ' · 进行中' : '' }}</option></select></label>
          <label>关联角色<select v-model="form.character_id"><option :value="null">全队 / 未指定</option><option v-for="character in characters" :key="character.database_id" :value="character.database_id">{{ character.name }}</option></select></label>
          <label>冒险日期<input v-model="form.event_date" placeholder="例如：霜落月 12 日"></label>
          <label>关联地点<select v-model="form.location_id" aria-label="关联地点"><option :value="null">未关联 / 手动填写</option><option v-for="location in locations" :key="location.id" :value="location.id">{{ location.name }}</option></select></label>
          <label v-if="form.location_id == null">地点名称<input v-model="form.location" placeholder="城市、地下城或区域"></label>
          <label v-if="form.entry_type === 'QUEST'">任务状态<select v-model="form.quest_status"><option value="ACTIVE">进行中</option><option value="COMPLETED">已完成</option><option value="FAILED">已失败</option></select></label>
          <label :class="{ full: form.entry_type !== 'QUEST' }">标签<input v-model="form.tags" placeholder="用逗号分隔，例如：主线, 龙巢, 可疑"></label>
        </div>
        <label class="full">详细记录<textarea v-model="form.content" rows="10" placeholder="发生了什么？获得了哪些线索？这个人物有什么特征？"></textarea></label>
        <label class="pin-check"><input v-model="form.pinned" type="checkbox"> 将这条记录置顶</label>
        <div class="editor-footer"><button type="button" class="btn-secondary" @click="closeEditor">取消</button><button class="btn-primary" :disabled="saving">{{ saving ? '保存中…' : '保存记录' }}</button></div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.focused-note { color: var(--text-secondary); font-size: 12px; margin: 0 0 15px; }
.focused-note a { color: var(--antique-gold); }
.entry-card.focused .content { display: block; -webkit-line-clamp: unset; }
.location-filter-bar { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin: 0 0 18px; color: var(--text-secondary); font-size: 12px; }
.location-filter-bar select { margin-left: 8px; padding: 7px 10px; color: var(--text-primary); border-radius: 6px; }
.location-filter-bar a, .metadata a { color: var(--antique-gold); text-decoration: none; }
.error-banner { display: flex; justify-content: space-between; padding: 12px 15px; margin-bottom: 15px; color: var(--text-danger); background: var(--surface-danger); border-radius: 8px; }
.error-banner button { color: inherit; cursor: pointer; --control-bg: transparent; }
.toolbar { padding: 14px; margin-bottom: 18px; background: var(--surface-panel); border-radius: 11px; }
.type-tabs { display: flex; gap: 5px; flex-wrap: wrap; }
.type-tabs button { padding: 8px 13px; color: var(--text-secondary); border-radius: 6px; cursor: pointer; --control-bg: var(--surface-card); --control-hover: var(--surface-hover); }
.type-tabs button b { display: inline-grid; place-items: center; min-width: 20px; margin-left: 5px; color: var(--text-dim); background: var(--surface-inset); border-radius: 9px; font-size: .7rem; }
.type-tabs button.active { color: var(--antique-gold); --control-bg: var(--surface-selected); }
.filters { display: grid; grid-template-columns: 1fr 190px; gap: 8px; margin-top: 12px; }
.filters input, .filters select, .editor input, .editor select, .editor textarea { box-sizing: border-box; width: 100%; padding: 10px 11px; color: var(--text-primary); border-radius: 6px; }
.entry-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr)); gap: 14px; }
.entry-card { position: relative; display: flex; flex-direction: column; min-height: 210px; padding: 17px; background: var(--surface-card); border-radius: 10px; min-width: 0; }
.card-top { display: flex; justify-content: space-between; align-items: center; }
.type-badge { color: var(--text-secondary); font-size: .72rem; letter-spacing: .08em; }
.card-actions { display: flex; gap: 3px; }
.card-actions button { width: 29px; height: 29px; color: var(--text-secondary); border-radius: 5px; cursor: pointer; --control-bg: transparent; --control-hover: var(--surface-hover); padding: 0; }
.entry-card h2 { margin: 10px 0 7px; color: var(--text-primary); font: 600 1.25rem var(--font-heading); overflow-wrap: anywhere; }
.metadata { display: flex; gap: 10px; flex-wrap: wrap; min-height: 18px; color: var(--text-dim); font-size: .72rem; }
.content { display: -webkit-box; overflow: hidden; margin: 13px 0; white-space: pre-line; color: var(--text-secondary); line-height: 1.6; -webkit-line-clamp: 5; -webkit-box-orient: vertical; }
.quest-status { align-self: flex-start; padding: 5px 9px; color: var(--antique-gold); border-radius: 12px; cursor: pointer; font-size: .7rem; --control-bg: var(--surface-selected); --control-hover: var(--surface-hover); }
.quest-status.completed { color: var(--text-success); --control-bg: var(--surface-success); }
.quest-status.failed { color: var(--text-danger); --control-bg: var(--surface-danger); --control-hover: var(--surface-danger-hover); }
.card-footer { display: flex; justify-content: space-between; align-items: end; gap: 8px; margin-top: auto; padding-top: 14px; flex-wrap: wrap; }
.tags { display: flex; gap: 5px; flex-wrap: wrap; }
.tags span { color: var(--antique-gold); font-size: .68rem; }
.card-footer time { flex: 0 0 auto; color: var(--text-dim); font-size: .65rem; }
.empty-state { padding: 70px 20px; text-align: center; color: var(--text-secondary); background: var(--surface-panel); border-radius: 12px; }
.empty-actions { display: flex; justify-content: center; gap: 8px; margin-top: 18px; flex-wrap: wrap; }
.empty-actions button { padding: 8px 12px; border-radius: 6px; cursor: pointer; }
.modal-backdrop { position: fixed; inset: 0; z-index: 100; display: grid; place-items: center; padding: 24px; background: var(--scrim); }
.editor { width: min(760px,100%); max-height: calc(100vh - 70px); overflow: auto; padding: 22px; background: var(--surface-overlay); border-radius: 13px; box-shadow: var(--overlay-shadow); }
.editor-header { display: flex; justify-content: space-between; }
.editor-header h2 { margin: 4px 0 0; }
.editor-header>button { color: var(--text-secondary); font-size: 1.5rem; cursor: pointer; --control-bg: transparent; padding: 0 8px; }
.type-picker { display: grid; grid-template-columns: repeat(3,1fr); gap: 7px; margin: 18px 0; }
.type-picker button { display: flex; flex-direction: column; gap: 4px; padding: 10px; text-align: left; color: var(--text-secondary); border-radius: 7px; cursor: pointer; --control-bg: var(--surface-card); --control-hover: var(--surface-hover); }
.type-picker button.active { color: var(--antique-gold); --control-bg: var(--surface-selected); }
.type-picker small { color: var(--text-dim); font-size: .67rem; }
.editor label { display: block; color: var(--text-secondary); font-size: .76rem; }
.editor label input, .editor label select, .editor label textarea { margin-top: 5px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 12px 0; }
.form-grid .full, .full { grid-column: 1/-1; }
.editor textarea { resize: vertical; font-family: var(--font-body); line-height: 1.55; }
.pin-check { display: flex; align-items: center; gap: 7px; margin-top: 11px; }
.pin-check input { width: auto; margin: 0; accent-color: var(--antique-gold); }
.editor-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; padding-top: 15px; }
.card-actions button[aria-pressed="true"] { color: var(--antique-gold); }
.card-actions .delete-entry { --control-hover: var(--surface-danger); color: var(--text-danger); }
@media (max-width:700px) {
  .filters, .form-grid { grid-template-columns: 1fr; }
  .form-grid .full, .full { grid-column: 1; }
  .entry-grid { grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr)); }
  .type-picker button small { display: none; }
  .modal-backdrop { padding: 8px; background: var(--scrim); }
  .editor { max-height: calc(100vh - 20px); box-shadow: var(--overlay-shadow); background: var(--surface-overlay); }
}
</style>
