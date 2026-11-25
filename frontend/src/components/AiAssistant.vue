<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, toRef, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AdventureLogAPI, AdventureRecapAPI, AdventureSessionAPI, AtlasAPI, CharacterAPI } from '../api'
import { assistantModes, buildAdventureContext, recapReferenceParts } from '../aiAssistant'
import { aiPanel, notifyAdventureHistoryChanged, openAiPanel } from '../aiPanel'
import type { AiContextSources } from '../aiAssistant'
import type { AiAssistantMode, AiAssistantResult, AiReply, AiSettingsView } from '../aiTypes'
import type { AdventureRecap, AdventureRecapSummary, AdventureSession } from '../adventureTypes'

const bridge = window.electronAPI?.ai
const route = useRoute()
const mode = toRef(aiPanel, 'mode')
const destinationId = toRef(aiPanel, 'locationId')
const adventureSessionId = toRef(aiPanel, 'adventureSessionId')
const sessions = ref<AdventureSession[]>([])
const history = ref<AdventureRecapSummary[]>([])
const historyId = ref<number | null>(null)
const savingSummary = ref(false), readingSummary = ref(false), editorOpen = ref(false)
const editTitle = ref(''), editContent = ref('')
const sources = ref<AiContextSources>({ characters: [], entries: [], locations: [] })
const characterIds = ref<number[]>([])
const recentEvents = ref<number | 'all'>('all')
const question = ref('')
const settings = ref<AiSettingsView | null>(null)
const loading = ref(false)
const loaded = ref(false)
const generating = ref(false)
const cancelling = ref(false)
const error = ref('')
const notice = ref('')
const launcher = ref<HTMLButtonElement | null>(null)
const closeControl = ref<HTMLButtonElement | null>(null)
const result = ref<(AiAssistantResult & { title: string; question: string; contextDisplay: string; contextSnapshot: string;
  adventureSessionId: number | null; mode: AiAssistantMode; savedId: number | null }) | null>(null)
const referenceParts = computed(() => result.value ? recapReferenceParts(result.value.reply, result.value.contextSnapshot) : [])
const scopedHistory = computed(() => history.value.filter(item => adventureSessionId.value === 'all' || (adventureSessionId.value === 'unassigned' ? item.adventure_session_id == null : item.adventure_session_id === adventureSessionId.value)))
const currentSession = computed(() => sessions.value.find(item => item.status === 'ACTIVE'))
const previousSession = computed(() => sessions.value.find(item => item.status === 'COMPLETED'))
let disposed = false
let initialized = false
const preview = computed(() => buildAdventureContext(sources.value, {
  characterIds: characterIds.value, destinationId: mode.value === 'destination' ? destinationId.value : null,
  recentEvents: recentEvents.value, mode: mode.value, adventureSessionId: adventureSessionId.value
}))
const ready = computed(() => bridge && loaded.value && settings.value?.model && !loading.value && !generating.value && !savingSummary.value && !readingSummary.value && !editorOpen.value
  && (typeof adventureSessionId.value !== 'number' || sessions.value.some(item => item.id === adventureSessionId.value))
  && (mode.value !== 'destination' || destinationId.value != null))
const labels: Record<string, string> = {
  note: '资料说明', currentLocation: '当前位置', destination: '回顾地点', party: '本次队伍', related: '地点关联记录',
  adventureSession: '所选场次', scope: '回顾范围', adventure_session_id: '关联场次编号', session_title: '关联场次', started_at: '开始时间', ended_at: '结束时间', notes: '场次备注',
  quests: '任务记录', events: '冒险经历', npcs: 'NPC 记录', locations: '已知地点', id: '编号', name: '名称',
  kind: '类型', status: '状态', is_current: '当前所在', description: '描述', race: '种族', class: '职业', level: '等级',
  hp: '当前生命', max_hp: '最大生命', ac: '护甲等级', background: '背景', personality: '性格', ideals: '理想',
  bonds: '羁绊', flaws: '缺陷', type: '记录类型', title: '标题', content: '内容', location_id: '关联地点编号',
  location_name: '地点名称', character_id: '关联角色编号', character_name: '关联角色', tags: '标签',
  event_date: '游戏内日期', recorded_at: '录入时间'
}
const values: Record<string, string> = { TOWN: '城镇', DUNGEON: '地下城', CAMP: '营地', LANDMARK: '地标', OTHER: '其他',
  UNKNOWN: '未探索', VISITED: '已探索', PLANNED: '计划前往', EVENT: '事件', QUEST: '任务', NPC: 'NPC',
  ACTIVE: '进行中', COMPLETED: '已完成', FAILED: '已失败' }
function displayContext(text: string): string {
  const format = (value: unknown, depth: number): string => {
    if (value == null || value === '') return '未记录'
    if (Array.isArray(value)) return value.length ? value.map((item, index) => `\n${'  '.repeat(depth)}${index + 1}. ${format(item, depth + 1)}`).join('\n') : '无'
    if (typeof value === 'object') return Object.entries(value).filter(([, item]) => item != null && item !== '')
      .map(([key, item]) => `${'  '.repeat(depth)}${labels[key] || key}：${format(item, depth + 1)}`).join('\n')
    if (typeof value === 'boolean') return value ? '是' : '否'
    return values[String(value)] || String(value)
  }
  return format(JSON.parse(text), 0)
}
const contextDisplay = computed(() => displayContext(preview.value.text))
function unwrap<T>(reply: AiReply<T>): T { if (!reply.ok) throw new Error(reply.error); return reply.value }
function message(cause: unknown) { return cause instanceof Error ? cause.message : '操作失败，请重试。' }
async function load() {
  if (loading.value || generating.value || savingSummary.value) return
  loading.value = true; error.value = ''; notice.value = ''
  try {
    const [characters, entries, locations, config, sessionList, recapList] = await Promise.all([
      CharacterAPI.getAll(), AdventureLogAPI.getAll(), AtlasAPI.locations(), bridge ? bridge.get() : Promise.resolve(null), AdventureSessionAPI.getAll(), AdventureRecapAPI.getAll()
    ])
    if (disposed) return
    settings.value = config ? unwrap(config) : null
    sessions.value = sessionList; history.value = recapList
    sources.value = { characters, entries, locations, sessions: sessionList }
    characterIds.value = initialized ? characterIds.value.filter(id => sources.value.characters.some(item => item.database_id === id))
      : sources.value.characters.map(item => item.database_id)
    if (!sources.value.locations.some(item => item.id === destinationId.value)) destinationId.value = null
    loaded.value = true; initialized = true
  } catch (cause) { error.value = message(cause); loaded.value = false }
  finally { loading.value = false }
}
async function generate() {
  if (!bridge || !ready.value) return
  const snapshot = { mode: mode.value, question: question.value.trim(), context: preview.value.text }
  const display = contextDisplay.value
  const scopeId = typeof adventureSessionId.value === 'number' ? adventureSessionId.value : null
  generating.value = true; cancelling.value = false; error.value = ''; notice.value = ''
  try {
    const reply = unwrap(await bridge.generate(snapshot))
    if (!disposed) {
      result.value = { ...reply, title: assistantModes[snapshot.mode].label, question: snapshot.question, contextDisplay: display,
        contextSnapshot: snapshot.context, adventureSessionId: scopeId, mode: snapshot.mode, savedId: null }
      editorOpen.value = false
    }
  } catch (cause) {
    if (!disposed) {
      if (cancelling.value && message(cause).includes('已取消')) notice.value = '本次总结已取消，已有结果保持不变。'
      else error.value = message(cause)
    }
  } finally { generating.value = false; cancelling.value = false }
}
async function cancel() {
  if (!bridge || cancelling.value) return
  cancelling.value = true
  try { unwrap(await bridge.cancelGeneration()) }
  catch (cause) { error.value = message(cause); cancelling.value = false }
}
async function copy() {
  if (!result.value) return
  try { await navigator.clipboard.writeText(result.value.reply); notice.value = '总结已复制。' }
  catch { error.value = '无法访问剪贴板，请选中文字后复制。' }
}
function editSummary() {
  if (!result.value || editorOpen.value) return
  editTitle.value = result.value.savedId ? result.value.title : `${sessions.value.find(item => item.id === result.value?.adventureSessionId)?.title ?? '冒险回顾'} · ${result.value.title}`.slice(0, 120)
  editContent.value = result.value.reply; editorOpen.value = true
}
function applySummary(value: AdventureRecap) {
  result.value = { title: value.title, reply: value.content, model: value.model, question: value.focus,
    contextSnapshot: value.context_snapshot, contextDisplay: displayContext(value.context_snapshot), adventureSessionId: value.adventure_session_id,
    mode: value.mode, savedId: value.id, latencyMs: 0, truncated: false }
  editorOpen.value = false; historyId.value = value.id
}
async function readSummary(id: number | null) {
  if (id == null || generating.value || savingSummary.value || readingSummary.value) return
  if (editorOpen.value) { notice.value = '请先保存或取消当前编辑，再打开其他总结。'; return }
  readingSummary.value = true; error.value = ''; notice.value = ''
  try { const value = await AdventureRecapAPI.get(id); if (!disposed) applySummary(value) }
  catch (cause) { error.value = message(cause) }
  finally { readingSummary.value = false }
}
async function saveSummary() {
  const target = result.value
  if (!target || savingSummary.value || !editTitle.value.trim() || !editContent.value.trim()) return
  savingSummary.value = true; error.value = ''
  try {
    const value = target.savedId ? await AdventureRecapAPI.update(target.savedId, editTitle.value.trim(), editContent.value.trim())
      : await AdventureRecapAPI.create({ title: editTitle.value.trim(), content: editContent.value.trim(), mode: target.mode,
        model: target.model, focus: target.question, adventure_session_id: target.adventureSessionId, context_snapshot: target.contextSnapshot })
    if (result.value === target) applySummary(value)
    notice.value = '总结已保存至本机，重启后仍可查看。'
    notifyAdventureHistoryChanged()
    try { history.value = await AdventureRecapAPI.getAll() } catch { notice.value += ' 历史列表稍后重新打开时更新。' }
  } catch (cause) { error.value = message(cause) }
  finally { savingSummary.value = false }
}
async function close() {
  aiPanel.open = false
  await nextTick()
  launcher.value?.focus()
}
watch(() => route.query.assistant, value => {
  if (typeof value === 'string' && Object.hasOwn(assistantModes, value))
    openAiPanel(value as AiAssistantMode, Number(route.query.locationId) || null)
}, { immediate: true })
watch(() => aiPanel.open, async open => {
  if (open) {
    void load()
    await nextTick()
    closeControl.value?.focus()
  }
}, { immediate: true })
watch(() => route.fullPath, () => { if (aiPanel.open) void load() })
watch(destinationId, () => { if (aiPanel.open && !loaded.value) void load() })
watch(adventureSessionId, () => { historyId.value = null })
watch(() => aiPanel.recapId, id => {
  if (id == null) return
  if (generating.value || savingSummary.value) notice.value = '请先完成或取消当前操作，再打开历史总结。'
  else void readSummary(id)
  aiPanel.recapId = null
}, { immediate: true })
const refreshHistory = () => { if (aiPanel.open) void load() }
onMounted(() => window.addEventListener('dmhelper:adventure-history-changed', refreshHistory))
onUnmounted(() => { disposed = true; window.removeEventListener('dmhelper:adventure-history-changed', refreshHistory); if (generating.value) void bridge?.cancelGeneration().catch(() => {}) })
</script>

<template>
  <button ref="launcher" class="recap-launcher" type="button" :aria-label="aiPanel.open ? '收起冒险回顾' : '打开冒险回顾'" :aria-expanded="aiPanel.open" aria-controls="adventure-recap-panel" @click="aiPanel.open ? close() : aiPanel.open = true">
    <svg viewBox="0 0 24 24" width="23" height="23" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M12 5c-3-2-6-2-9-1v15c3-1 6-1 9 1 3-2 6-2 9-1V4c-3-1-6-1-9 1Zm0 0v15M6 8l3 1m-3 3 3 1m6-4 3-1m-3 5 3-1"/></svg>
    <span>回顾</span><i v-if="generating" class="working-dot" aria-label="正在整理冒险记录"></i>
  </button>
  <Transition name="recap-panel">
    <section v-show="aiPanel.open" id="adventure-recap-panel" class="recap-panel" role="dialog" aria-label="冒险回顾面板" aria-modal="false" @keydown.esc.stop="close">
      <header class="panel-heading"><div><p class="eyebrow">ADVENTURE CHRONICLE</p><h2>冒险回顾</h2><p class="subtitle">整理已经发生的故事，留住旅途的来龙去脉。</p></div><div class="header-actions"><router-link to="/ai-settings" @click="close">模型配置</router-link><button ref="closeControl" type="button" class="close-button" aria-label="收起回顾面板" @click="close">×</button></div></header>
      <div class="panel-scroll">
        <p v-if="error" class="banner error" role="alert">{{ error }}</p>
        <p v-if="notice" class="banner success" role="status">{{ notice }}</p>
        <p v-if="loading" class="hint" role="status">正在读取冒险记录…</p>
        <p v-else-if="!settings?.model && bridge" class="banner">请先在 <router-link to="/ai-settings" @click="close">AI 配置</router-link> 保存模型连接。</p>
        <form @submit.prevent="generate">
          <fieldset :disabled="loading || generating || savingSummary || readingSummary || !loaded">
            <legend class="sr-only">选择总结方式</legend>
            <label>回顾场次<select v-model="adventureSessionId"><option value="all">全部冒险</option><option value="unassigned">未关联场次的旧记录</option><option v-for="session in sessions" :key="session.id" :value="session.id">{{ session.title }}{{ session.status === 'ACTIVE' ? ' · 进行中' : '' }}</option></select></label>
            <div class="scope-shortcuts"><button v-if="currentSession" type="button" @click="adventureSessionId = currentSession.id">当前场次</button><button v-if="previousSession" type="button" @click="adventureSessionId = previousSession.id">上一次跑团</button><router-link to="/sessions">管理场次 ↗</router-link></div>
            <div class="mode-grid"><button v-for="(option, key) in assistantModes" :key="key" type="button" :class="['mode-option', { selected: mode === key }]" :aria-pressed="mode === key" @click="mode = key">{{ option.label }}</button></div>
            <p class="mode-description">{{ assistantModes[mode].description }}</p>
            <label v-if="mode === 'destination'">回顾地点<select v-model="destinationId" required><option :value="null" disabled>选择一个地点</option><option v-for="place in sources.locations" :key="place.id" :value="place.id">{{ place.name }}</option></select></label>
            <p v-if="mode === 'destination' && !sources.locations.length" class="hint">还没有地点，可先到 <router-link to="/atlas" @click="close">冒险地图</router-link> 添加。</p>
            <label>想重点回顾什么（可选）<textarea v-model="question" maxlength="2000" rows="2" placeholder="例如：整理我们在旧矿井遇到的人和事。"></textarea></label>
            <details class="scope-controls"><summary>资料范围 · {{ preview.counts.events }} 条经历 / {{ preview.counts.quests }} 项任务</summary>
              <label>经历范围<select v-model="recentEvents"><option value="all">全部已记录经历</option><option :value="10">最近 10 条</option><option :value="20">最近 20 条</option><option :value="50">最近 50 条</option><option :value="0">不包含事件记录</option></select></label>
              <label>队伍资料</label><div class="party-picker"><label v-for="character in sources.characters" :key="character.database_id" class="character-choice"><input v-model="characterIds" type="checkbox" :value="character.database_id">{{ character.name }}</label><p v-if="!sources.characters.length" class="hint">暂无角色，仍可总结日志与地点记录。</p></div>
              <p class="hint">同时参考已完成、进行中和失败的任务，以及 NPC 与地点记录。</p>
              <p v-if="preview.omitted || preview.shortened" class="limit-note">篇幅限制：省略 {{ preview.omitted }} 条记录，截短 {{ preview.shortened }} 处长文本；本次总结可能不完整。</p>
              <details><summary>预览将发送的文字资料</summary><pre>{{ contextDisplay }}</pre></details>
            </details>
          </fieldset>
          <p v-if="(preview.omitted || preview.shortened) && loaded" class="limit-note">记录较多，本次资料有省略或截短，可展开“资料范围”查看。</p>
          <p class="privacy-note">生成时会把所选文字资料发送给已配置的模型，可能产生 API 费用。生成本身不会更改角色或任务。</p>
          <div class="request-actions"><button type="button" :disabled="loading || generating || savingSummary || readingSummary" @click="load">更新资料</button><button v-if="generating" type="button" :disabled="cancelling" @click="cancel">{{ cancelling ? '正在取消…' : '取消总结' }}</button><button v-else class="primary" :disabled="!ready" type="submit">总结{{ assistantModes[mode].label }}</button></div>
        </form>
        <article class="result-section" aria-live="polite" :aria-busy="generating">
          <p v-if="generating" class="generating" role="status">正在整理已记录的冒险…<small>可以收起面板或切换页面，完成后再回来查看。</small></p>
          <template v-if="result">
            <div class="result-heading"><h3>{{ result.title }}</h3><button type="button" @click="copy">复制总结</button></div>
            <p class="result-meta">{{ result.model }} · {{ result.savedId ? '已保存至本机' : `${(result.latencyMs / 1000).toFixed(1)} 秒 · 尚未保存` }}</p>
            <p v-if="result.question" class="question-note">回顾重点：{{ result.question }}</p>
            <p v-if="result.truncated" class="banner">总结达到长度上限，可能尚未完成，可缩小资料范围后重试。</p>
            <div class="answer-text"><template v-for="(part, index) in referenceParts" :key="index"><router-link v-if="part.to" :to="part.to" title="查看原始记录">{{ part.text }}</router-link><span v-else>{{ part.text }}</span></template></div>
            <button class="save-recap" type="button" :disabled="generating || readingSummary || savingSummary" @click="editSummary">{{ result.savedId ? '修改已保存的总结' : '编辑并保存总结' }}</button>
            <form v-if="editorOpen" class="recap-editor" @submit.prevent="saveSummary"><label>总结标题<input v-model="editTitle" required maxlength="120"></label><label>总结内容<textarea v-model="editContent" rows="8" required maxlength="200000"></textarea></label><p class="hint">{{ result.adventureSessionId ? `关联场次：${sessions.find(item => item.id === result?.adventureSessionId)?.title ?? '#' + result.adventureSessionId}` : '保存为未指定场次的回顾' }}。保存会保留本次资料快照，后续修改只更新标题与正文。</p><div class="request-actions"><button type="button" :disabled="savingSummary" @click="editorOpen = false">取消编辑</button><button class="primary" :disabled="savingSummary || !editTitle.trim() || !editContent.trim()">{{ savingSummary ? '保存中…' : result.savedId ? '保存修改' : '保存到本机' }}</button></div></form>
            <details class="source-details"><summary>查看这次总结使用的资料</summary><pre>{{ result.contextDisplay }}</pre></details>
          </template>
          <p v-else-if="!generating" class="empty-note">总结会显示在这里。选择“编辑并保存总结”后，重启应用仍可从历史记录中查看。</p>
        </article>
        <details class="history-section"><summary>已保存的总结 · {{ scopedHistory.length }}</summary><p v-if="!scopedHistory.length" class="hint">当前场次范围内还没有保存的总结。</p><template v-else><label>历史总结<select v-model="historyId"><option :value="null" disabled>选择一篇总结</option><option v-for="item in scopedHistory" :key="item.id" :value="item.id">{{ item.title }} · {{ item.created_at.slice(0, 10) }}</option></select></label><button type="button" :disabled="historyId == null || generating || savingSummary || readingSummary" @click="readSummary(historyId)">{{ readingSummary ? '读取中…' : '查看已保存总结' }}</button></template></details>
      </div>
    </section>
  </Transition>
</template>

<style scoped>
.scope-shortcuts{display:flex;align-items:center;gap:7px;flex-wrap:wrap;margin:9px 0 14px}.scope-shortcuts button{font-size:10px;padding:5px 9px}.scope-shortcuts a{font-size:10px}.save-recap{margin-top:15px}.recap-editor{padding:12px;margin-top:12px;background:#0c1710;border:1px solid #5d6944;border-radius:7px}.recap-editor input{display:block;width:100%;margin-top:6px;padding:9px;background:#152119;color:#dcdcc5;border:1px solid #5a6547;border-radius:5px;font:inherit}.recap-editor textarea{max-height:320px}.history-section{border-top:1px solid #59633e;margin-top:20px;padding-top:15px}.history-section>button{margin-top:12px}.answer-text a{color:#d6ba75;text-decoration:underline;text-underline-offset:3px}
.recap-launcher{position:fixed;right:24px;bottom:22px;z-index:120;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;width:58px;height:58px;border:1px solid #b39a57;border-radius:50%;background:linear-gradient(145deg,#354333,#18251c);color:#e3cc8a;box-shadow:0 5px 22px #0008;cursor:pointer}.recap-launcher:hover{border-color:#efd890;background:linear-gradient(145deg,#48553c,#233024)}.recap-launcher span{font-size:10px;letter-spacing:.1em}.working-dot{position:absolute;right:0;top:0;width:10px;height:10px;border:2px solid #17241b;border-radius:50%;background:#d2bd72}.recap-panel{position:fixed;right:24px;bottom:92px;z-index:121;width:470px;max-width:calc(100vw - 32px);max-height:min(780px,calc(100dvh - 180px));display:flex;flex-direction:column;overflow:hidden;border:1px solid #7d714b;border-radius:14px;background:#18221bec;box-shadow:0 16px 65px #0009;color:#e5dec9;backdrop-filter:blur(12px)}.panel-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:10px;padding:19px 19px 15px;border-bottom:1px solid #73805b44;background:#263024}.eyebrow{font-size:9px;letter-spacing:.18em;color:#b6a36b;margin:0 0 6px}h2{font-size:22px;color:#ecdfb9;margin:0}.subtitle{font-size:11px;color:#a5ae98;line-height:1.7;margin:8px 0 0}.header-actions{display:flex;align-items:center;gap:10px;flex-shrink:0}.header-actions a{white-space:nowrap;font-size:10px}.close-button{padding:0;width:26px;height:26px;font-size:22px;border:0;background:transparent;color:#b9c2aa}.panel-scroll{overflow-y:auto;overscroll-behavior:contain;padding:17px 19px 20px;min-height:0}fieldset{border:0;padding:0;margin:0;min-width:0}fieldset:disabled{opacity:.65}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%);white-space:nowrap}.mode-grid{display:grid;grid-template-columns:1fr 1fr;gap:7px}.mode-option{background:#101b15;border-color:#49563b;padding:10px;font-size:12px}.mode-option.selected{color:#e2cd8e;border-color:#baa05c;background:#6f63302b}.mode-description{font-size:11px;color:#9fa991;line-height:1.8;margin:9px 0 0}label{display:block;margin-top:14px;color:#c9ccb5;font-size:11px}select,textarea{box-sizing:border-box;width:100%;padding:9px 10px;margin-top:7px;background:#101a14;border:1px solid #4a593f;border-radius:6px;color:#dedfcb;font:inherit;outline:none}textarea{resize:vertical;line-height:1.8;max-height:150px}select:focus,textarea:focus{border-color:#b6a064}.scope-controls{margin-top:13px;padding:11px;border:1px solid #52613b66;background:#10201644;border-radius:6px}summary{cursor:pointer;font-size:11px;color:#c0b386;line-height:1.7}details details{margin-top:10px}.party-picker{display:flex;gap:8px;flex-wrap:wrap;max-height:120px;overflow:auto;margin-top:7px}.character-choice{display:flex;gap:7px;align-items:center;margin:0;padding:6px 9px;background:#101a13;border:1px solid #46553c;border-radius:5px}.character-choice input{accent-color:#c1a55d}.hint,.privacy-note,.limit-note{font-size:10px;line-height:1.8;color:#9ca68e;margin:9px 0}.privacy-note{margin:13px 0}.limit-note{color:#d4bb7d}.request-actions{display:flex;justify-content:space-between;gap:9px}button{padding:8px 11px;cursor:pointer;color:#d5c9a8;background:#293327;border:1px solid #645f40;border-radius:6px;font:inherit;font-size:11px}button:hover{border-color:#c4a55f}button:focus-visible,a:focus-visible,summary:focus-visible{outline:2px solid #cfb86f;outline-offset:3px}button:disabled{opacity:.45;cursor:default}.primary{color:#201d12;background:linear-gradient(135deg,#dfc477,#ae8743);font-weight:700;border-color:#b18b48}a{color:#cdbb80;text-decoration:none}.result-section{margin-top:20px;border-top:1px solid #71815744;padding-top:17px}.result-heading{display:flex;justify-content:space-between;align-items:center;gap:10px}h3{font-size:17px;color:#dfd4b0;margin:0}.result-meta{font-size:10px;color:#8e9b82;margin:9px 0}.question-note{font-size:11px;line-height:1.8;white-space:pre-wrap;color:#b4ba9f;border-left:2px solid #8f8150;padding-left:10px;margin:12px 0}.answer-text{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.95;color:#d8dcc8;font-size:13px;user-select:text}.source-details{margin-top:18px}.empty-note{font-size:11px;color:#939f87;line-height:1.9;margin:0}.generating{color:#c9ce9c;font-size:12px;line-height:1.8;padding:12px;background:#46533244;border-radius:6px;margin-bottom:14px}.generating small{display:block;color:#a0a78e;font-size:10px}pre{white-space:pre-wrap;overflow-wrap:anywhere;font:10px/1.9 var(--font-body);color:#a7ae99;max-height:260px;overflow:auto;background:#0c1610;padding:10px;border-radius:5px;margin-top:10px}.banner{padding:10px 12px;margin:0 0 13px;border:1px solid #7b704d;border-radius:6px;font-size:11px;line-height:1.8;background:#453b2355;color:#d8c59c}.error{color:#efb1a0;background:#6a332355;border-color:#8e4c38}.success{color:#c3dab2;background:#344b2d66;border-color:#677f4b}.recap-panel-enter-active,.recap-panel-leave-active{transition:opacity .15s,transform .15s}.recap-panel-enter-from,.recap-panel-leave-to{opacity:0;transform:translateY(10px)}@media(max-width:600px){.recap-launcher{right:16px;bottom:16px;width:54px;height:54px}.recap-panel{right:12px;bottom:82px;max-width:calc(100vw - 24px);max-height:calc(100dvh - 160px)}.panel-heading{padding:15px}.panel-scroll{padding:15px}.subtitle{max-width:235px}.header-actions{gap:5px}}@media(prefers-reduced-motion:reduce){.recap-panel-enter-active,.recap-panel-leave-active{transition:none}}
</style>
