<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AdventureLogAPI, AdventureRecapAPI, AdventureSessionAPI } from '../api'
import { openAiPanel, openSavedRecap } from '../aiPanel'
import type { AdventureRecapSummary, AdventureSession } from '../adventureTypes'
import type { AiJournalSource } from '../aiAssistant'

const sessions = ref<AdventureSession[]>([])
const entries = ref<AiJournalSource[]>([])
const recaps = ref<AdventureRecapSummary[]>([])
const route = useRoute()
const selectedId = ref<number | null>(Number(route.query.sessionId) || null)
const loading = ref(true), busy = ref(false), error = ref('')
const title = ref(''), notes = ref(''), editTitle = ref(''), editNotes = ref('')
const active = computed(() => sessions.value.find(item => item.status === 'ACTIVE'))
const selected = computed(() => sessions.value.find(item => item.id === selectedId.value))
const related = computed(() => entries.value.filter(item => item.adventure_session_id === selectedId.value))
const summaries = computed(() => recaps.value.filter(item => item.adventure_session_id === selectedId.value))
function date(value: string | null) { return value ? new Date(value).toLocaleString('zh-CN') : '尚未结束' }
function select(value: AdventureSession) { selectedId.value = value.id; editTitle.value = value.title; editNotes.value = value.notes }
watch(() => route.query.sessionId, value => { const item = sessions.value.find(session => session.id === Number(value)); if (item) select(item) })
async function load() {
  loading.value = true; error.value = ''
  try {
    const old = selected.value
    const preserveEdits = old && (editTitle.value !== old.title || editNotes.value !== old.notes)
    const [list, logs, history] = await Promise.all([AdventureSessionAPI.getAll(), AdventureLogAPI.getAll(), AdventureRecapAPI.getAll()])
    sessions.value = list; entries.value = logs; recaps.value = history
    const choice = list.find(item => item.id === selectedId.value) ?? list.find(item => item.status === 'ACTIVE') ?? list[0]
    if (choice && (!preserveEdits || choice.id !== selectedId.value)) select(choice)
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '场次读取失败。' }
  finally { loading.value = false }
}
async function perform(operation: () => Promise<void>) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await operation() } catch (cause) { error.value = cause instanceof Error ? cause.message : '操作失败。' }
  finally { busy.value = false }
}
function start() {
  void perform(async () => {
    const created = await AdventureSessionAPI.start(title.value.trim(), notes.value.trim())
    selectedId.value = created.id; title.value = ''; notes.value = ''; await load()
  })
}
function finish(value: AdventureSession) {
  if (!window.confirm(`结束“${value.title}”？之后仍可以查看并补充该场次的记录。`)) return
  void perform(async () => { await AdventureSessionAPI.finish(value.id); await load() })
}
function save() {
  const current = selected.value
  if (current) void perform(async () => { await AdventureSessionAPI.update(current.id, editTitle.value.trim(), editNotes.value.trim()); await load() })
}
const refreshHistory = () => { void load() }
onMounted(() => { void load(); window.addEventListener('dmhelper:adventure-history-changed', refreshHistory) })
onUnmounted(() => window.removeEventListener('dmhelper:adventure-history-changed', refreshHistory))
</script>

<template>
  <section class="sessions-page">
    <header><p class="eyebrow">SESSION CHRONICLE</p><h1>跑团场次</h1><p>把每一次相聚的见闻、战斗与回顾放在一起。</p></header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div v-if="active" class="active-session"><div><small>正在进行</small><h2>{{ active.title }}</h2><p>开始于 {{ date(active.started_at) }}</p></div><div class="actions"><button :disabled="busy" @click="select(active)">查看本场</button><button @click="openAiPanel('recap', null, active.id)">回顾本场</button><button :disabled="busy" @click="finish(active)">结束场次</button></div></div>
    <form v-else class="start-form" @submit.prevent="start"><h2>开始新的场次</h2><label>场次名称<input v-model="title" maxlength="120" required placeholder="例如：第三次冒险 · 旧矿井调查"></label><label>本场备注<textarea v-model="notes" maxlength="20000" rows="2" placeholder="记录本场背景、游戏内日期或需要留意的事。"></textarea></label><button class="primary" :disabled="busy || loading || !title.trim()">开始本场冒险</button></form>
    <p v-if="loading" class="muted">正在读取场次档案…</p>
    <div v-else class="session-layout">
      <aside><h2>场次档案</h2><button v-for="item in sessions" :key="item.id" :class="['session-item', { selected: item.id === selectedId }]" @click="select(item)"><b>{{ item.title }}</b><small>{{ item.status === 'ACTIVE' ? '进行中' : '已结束' }} · {{ item.log_count }} 条记录 · {{ item.recap_count }} 篇总结</small></button><p v-if="!sessions.length" class="muted">开始第一场冒险后，这里会留下场次档案。</p><router-link to="/journal?adventureSessionId=unassigned">查看未关联场次的旧记录 ↗</router-link></aside>
      <main v-if="selected" class="session-detail">
        <div class="detail-heading"><h2>{{ selected.title }}</h2><span>{{ selected.status === 'ACTIVE' ? '进行中' : '已结束' }}</span></div><p class="muted">{{ date(selected.started_at) }} → {{ date(selected.ended_at) }}</p>
        <form @submit.prevent="save"><label>场次名称<input v-model="editTitle" maxlength="120" required></label><label>备注<textarea v-model="editNotes" maxlength="20000" rows="3"></textarea></label><div class="actions"><button :disabled="busy || !editTitle.trim()">保存场次信息</button><button type="button" @click="openAiPanel('recap', null, selected.id)">总结这一场</button><router-link :to="{path:'/journal', query:{adventureSessionId:selected.id}}">查看 / 添加本场记录 ↗</router-link></div></form>
        <section class="records"><h3>本场记录 · {{ related.length }}</h3><router-link v-for="entry in related" :key="entry.id" :to="{path:'/journal',query:{entryId:entry.id}}"><small>{{ entry.entry_type === 'QUEST' ? '任务' : entry.entry_type === 'NPC' ? '人物' : '见闻 / 战斗' }}</small><b>{{ entry.title }}</b></router-link><p v-if="!related.length" class="muted">还没有本场记录。新建日志或归档战斗时可关联到本场。</p></section>
        <section class="records"><h3>已保存的总结 · {{ summaries.length }}</h3><button v-for="recap in summaries" :key="recap.id" @click="openSavedRecap(recap.id)"><b>{{ recap.title }}</b><small>{{ date(recap.updated_at) }} · {{ recap.model }}</small></button><p v-if="!summaries.length" class="muted">生成回顾后，点击“编辑并保存”即可留在这里。</p></section>
      </main>
    </div>
  </section>
</template>

<style scoped>
.sessions-page{padding:30px clamp(20px,4vw,56px) 70px;color:var(--text-primary)}header{margin-bottom:24px}.eyebrow{font-size:10px;letter-spacing:.2em;color:#bfa260}h1{font-size:36px;color:#e9d7ac;margin:7px 0}header>p:last-child,.muted{color:#aaa991;font-size:12px;line-height:1.8}h2{font-size:20px;color:#ddd0ad;margin:0 0 10px}h3{font-size:15px;color:#d5c79e;margin-bottom:12px}button,input,textarea{font:inherit}button{cursor:pointer;color:#d9c89c;background:#293124;border:1px solid #6c6040;border-radius:6px;padding:9px 13px;font-size:12px}button:disabled{opacity:.5;cursor:default}.primary{background:#c1a25e;color:#1b190e;font-weight:700}a{color:#d0b574;text-decoration:none;font-size:12px}.active-session,.start-form,aside,.session-detail{padding:22px;border:1px solid #625737;border-radius:10px;background:#1c241bed}.active-session{display:flex;justify-content:space-between;align-items:center;gap:20px;margin-bottom:22px}.active-session small{color:#a1ba8d;font-size:11px}.active-session h2{margin:5px 0}.active-session p{font-size:11px;color:#9da68e}.start-form{max-width:780px;margin-bottom:24px}.start-form button{margin-top:15px}label{display:block;font-size:12px;color:#bfc5ac;margin:14px 0}input,textarea{display:block;width:100%;padding:10px;margin-top:7px;border:1px solid #526045;border-radius:6px;background:#101a13;color:#dcdcc6;resize:vertical}.actions{display:flex;gap:10px;align-items:center;flex-wrap:wrap}.session-layout{display:grid;grid-template-columns:280px minmax(0,1fr);gap:20px;align-items:start}.session-item{display:block;width:100%;text-align:left;background:#121b14;margin:9px 0}.session-item.selected{border-color:#c6a764;background:#4c4b272f}small{display:block;font-size:10px;color:#9da68d;line-height:1.8}.session-item b{display:block;font-size:13px;margin-bottom:5px}aside>a{display:block;margin-top:18px}.detail-heading{display:flex;justify-content:space-between;gap:15px}.detail-heading span{font-size:11px;color:#b3c199}.records{border-top:1px solid #58613c66;margin-top:24px;padding-top:20px}.records>a,.records>button{display:block;width:100%;text-align:left;padding:12px;background:#111b14;border:1px solid #4f583a;border-radius:6px;margin:8px 0}.records b{display:block;color:#d8d0af;font-size:13px}.error{padding:12px;border:1px solid #8b5140;background:#5c2e2555;color:#e5b6a2;margin-bottom:15px;font-size:12px}@media(max-width:850px){.session-layout{grid-template-columns:1fr}.active-session{align-items:flex-start;flex-direction:column}aside{max-height:350px;overflow:auto}}
</style>
