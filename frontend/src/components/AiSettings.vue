<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import type { AiReply, AiSettingsDraft, AiSettingsView, AiTestResult } from '../aiTypes'
import { aiProviders, providerForBaseUrl, sameApiOrigin } from '../aiProviders'
import type { AiProvider } from '../aiProviders'
import { openAiPanel } from '../aiPanel'

const bridge = window.electronAPI?.ai
const initial = (): AiSettingsDraft => ({ baseUrl: '', model: '', timeoutSeconds: 60, apiKey: '', clearApiKey: false })
const form = reactive(initial())
const provider = ref<AiProvider>('openai')
const customModel = ref(false)
const preset = computed(() => provider.value === 'custom' ? null : aiProviders[provider.value])
const keyMatchesService = computed(() => Boolean(saved.value?.hasApiKey && sameApiOrigin(saved.value.baseUrl, form.baseUrl)))
const saved = ref<AiSettingsView | null>(null)
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const cancelling = ref(false)
const visibleKey = ref(false)
const error = ref('')
const notice = ref('')
const result = ref<AiTestResult | null>(null)
let disposed = false
const dirty = computed(() => saved.value != null && (form.baseUrl !== saved.value.baseUrl || form.model !== saved.value.model
  || form.timeoutSeconds !== saved.value.timeoutSeconds || Boolean(form.apiKey) || form.clearApiKey))
const endpoint = computed(() => {
  if (!form.baseUrl.trim()) return '填写 API 基础地址后显示'
  return `${form.baseUrl.trim().replace(/\/+$/, '').replace(/\/chat\/completions$/, '')}/chat/completions`
})
function unwrap<T>(response: AiReply<T>): T {
  if (!response.ok) throw new Error(response.error)
  return response.value
}
function message(cause: unknown) { return cause instanceof Error ? cause.message : '操作失败，请重试。' }
function apply(settings: AiSettingsView) {
  saved.value = settings
  const fresh = !settings.baseUrl && !settings.model
  provider.value = fresh ? 'openai' : providerForBaseUrl(settings.baseUrl)
  const baseUrl = fresh ? aiProviders.openai.baseUrl : settings.baseUrl
  const model = fresh ? aiProviders.openai.models[0]!.id : settings.model
  customModel.value = Boolean(preset.value && !preset.value.models.some(option => option.id === model))
  Object.assign(form, { baseUrl, model, timeoutSeconds: settings.timeoutSeconds, apiKey: '', clearApiKey: false })
  visibleKey.value = false
}
function changeProvider(event: Event) {
  provider.value = (event.target as HTMLSelectElement).value as AiProvider
  if (preset.value) {
    form.baseUrl = preset.value.baseUrl
    form.model = preset.value.models[0]!.id
  }
  // A draft key must never carry over when the user switches services.
  form.apiKey = ''; form.clearApiKey = false; visibleKey.value = false
  customModel.value = false
  error.value = ''
}
function changeModel(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  customModel.value = value === 'CUSTOM'
  form.model = customModel.value ? '' : value
}
async function load() {
  error.value = ''; loading.value = true
  if (!bridge) { error.value = '请在 DMHelper 桌面应用中配置 AI。'; loading.value = false; return }
  try { apply(unwrap(await bridge.get())) } catch (cause) { error.value = message(cause) }
  finally { loading.value = false }
}
function draft(): AiSettingsDraft { return { ...form } }
async function save() {
  if (!bridge || saving.value || testing.value) return
  saving.value = true; error.value = ''; notice.value = ''; result.value = null
  try {
    apply(unwrap(await bridge.save(draft())))
    notice.value = 'AI 配置已保存。'
  } catch (cause) { error.value = message(cause) }
  finally { saving.value = false }
}
async function test() {
  if (!bridge || saving.value || testing.value) return
  testing.value = true; error.value = ''; notice.value = ''; result.value = null
  try { const value = unwrap(await bridge.test(draft())); if (!disposed) result.value = value }
  catch (cause) { if (!disposed) error.value = message(cause) }
  finally { testing.value = false; cancelling.value = false }
}
async function cancel() {
  if (!bridge || cancelling.value) return
  cancelling.value = true
  try { unwrap(await bridge.cancelTest()) }
  catch (cause) { error.value = message(cause); cancelling.value = false }
}
watch(form, () => { result.value = null; notice.value = '' }, { flush: 'sync' })
watch(() => form.clearApiKey, clear => { if (clear) { form.apiKey = ''; visibleKey.value = false } })
onMounted(load)
onUnmounted(() => {
  disposed = true
  form.apiKey = ''
  if (testing.value) void bridge?.cancelTest().catch(() => {})
})
</script>

<template>
  <section class="workspace-page">
    <header class="workspace-heading"><h1>AI 配置</h1></header>
    <div v-if="error" class="banner error" role="alert">{{ error }}</div>
    <div v-if="notice" class="banner success" role="status">{{ notice }}</div>
    <p v-if="loading" class="muted">正在读取 AI 配置…</p>
    <div v-else class="settings-layout">
      <form class="settings-card" @submit.prevent="save">
        <div class="card-heading"><div><h2>模型连接</h2></div><span :class="['badge', { ready: saved?.model }]">{{ dirty ? '有未保存修改' : saved?.model ? '已保存配置' : '尚未配置' }}</span></div>
        <fieldset :disabled="!bridge || !saved || saving || testing">
          <label>模型服务<select :value="provider" aria-label="模型服务" @change="changeProvider"><option v-for="(option, key) in aiProviders" :key="key" :value="key">{{ option.label }}</option><option value="custom">自定义 / 本机模型</option></select></label>
          <label v-if="preset">选择模型<select :value="customModel ? 'CUSTOM' : form.model" aria-label="选择模型" @change="changeModel"><option v-for="option in preset.models" :key="option.id" :value="option.id">{{ option.label }}</option><option value="CUSTOM">其他模型（手动输入 ID）</option></select></label>
          <label v-if="provider === 'custom' || customModel">模型名称<input v-model="form.model" required maxlength="200" placeholder="填写服务商提供的模型 ID" autocomplete="off" spellcheck="false"></label>
          <p v-if="preset" class="hint">官方地址已自动配置，输入对应服务的 API Key 即可。也可选择其他模型；具体可用性以你的 API 账户为准。</p>
          <p v-else class="hint">支持文本 Chat Completions 兼容接口，模型 ID 按服务商提供的名称填写。</p>
          <label>API 基础地址<input v-model="form.baseUrl" :readonly="provider !== 'custom'" type="url" required maxlength="2048" placeholder="https://你的服务地址/v1" autocomplete="off" spellcheck="false"></label>
          <p v-if="provider === 'custom'" class="hint">填写完整基础地址并保留需要的路径（例如 /v1）。使用中转或本机接口请选择此项。</p>
          <div class="key-heading"><label for="ai-api-key">API Key</label><span :class="['key-state', { configured: keyMatchesService }]">{{ keyMatchesService ? '● 已保存密钥' : saved?.hasApiKey ? '○ 此服务尚未保存密钥' : '○ 未保存密钥' }}</span></div>
          <div class="key-input"><input id="ai-api-key" v-model="form.apiKey" :type="visibleKey ? 'text' : 'password'" :disabled="form.clearApiKey" :placeholder="keyMatchesService ? '留空则保留现有密钥' : provider === 'custom' ? '输入密钥；无需认证的本机服务可留空' : '输入 ' + (provider === 'openai' ? 'OpenAI' : provider === 'gemini' ? 'Gemini' : 'DeepSeek') + ' API Key'" autocomplete="new-password" spellcheck="false"><button type="button" :disabled="!form.apiKey || form.clearApiKey" :aria-pressed="visibleKey" @click="visibleKey = !visibleKey">{{ visibleKey ? '隐藏' : '显示' }}</button></div>
          <label v-if="saved?.hasApiKey" class="check"><input v-model="form.clearApiKey" type="checkbox">清除已保存密钥（保存后生效）</label>
          <p class="hint">密钥使用系统加密保存在本机。切换服务后需填写该服务的密钥；当前仅保存一套连接配置。</p>
          <p v-if="saved && !saved.secureStorageAvailable" class="storage-error">系统安全存储当前不可用，暂时无法保存或读取密钥；无需密钥的本机服务仍可配置。</p>
          <label class="timeout-label">请求超时（秒）<input v-model.number="form.timeoutSeconds" type="number" min="5" max="180" step="1" required></label>
        </fieldset>
        <div class="endpoint"><span>请求地址</span><code>{{ endpoint }}</code></div>
        <footer>
          <button v-if="testing" type="button" :disabled="cancelling" @click="cancel">{{ cancelling ? '正在取消…' : '取消测试' }}</button>
          <button v-else type="button" :disabled="!bridge || !saved || saving || !form.baseUrl.trim() || !form.model.trim()" @click="test">测试连接</button>
          <span v-if="testing" class="muted" role="status">正在等待模型回复…</span>
          <button class="btn-primary" :disabled="!bridge || !saved || saving || testing">{{ saving ? '保存中…' : '保存配置' }}</button>
        </footer>
        <p class="hint test-hint">测试使用当前表单，不会自动保存；会发送一句“仅回复 OK”的请求，可能产生少量 API 费用。</p>
      </form>

      <aside class="aside">
        <section class="test-card" aria-live="polite">
<h2>{{ testing ? '正在连接' : result ? '连接成功' : '等待测试连接' }}</h2>
          <template v-if="result"><p class="latency">{{ (result.latencyMs / 1000).toFixed(2) }} <span>秒</span></p><p class="hint">服务已返回有效文本</p><blockquote>{{ result.reply }}</blockquote><p v-if="dirty" class="hint">测试通过，记得保存当前配置。</p></template>
          <p v-else class="muted">{{ testing ? '请求正在发送到你填写的服务地址，可随时取消。' : '填写连接信息后，测试 API 地址、模型名称和密钥能否一起工作。' }}</p>
        </section>
        <section class="usage-card"><h2>连接说明</h2><p>保存配置只写入本机，不会发起模型请求。</p><p>测试连接仅发送固定测试文本，不会读取或发送角色、日志、地图和地点资料。</p><p>保存后，点击任意页面右下角的“回顾”，即可总结冒险经历、故事脉络、任务与线索。生成前可预览文字资料。</p><button type="button" @click="openAiPanel()">打开冒险回顾</button></section>
        <button v-if="!saved && bridge" :disabled="loading" @click="load">重新读取配置</button>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.settings-layout { display: grid; grid-template-columns: minmax(0,700px) minmax(240px,340px); gap: 22px; align-items: start; }
.settings-card, .test-card, .usage-card { background: var(--surface-panel); border-radius: 12px; padding: 25px; }
.card-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 8px; }
h2 { font-size: 19px; margin: 0; }
.badge { font-size: 10px; white-space: nowrap; padding: 5px 8px; border-radius: 15px; }
.badge.ready { color: var(--text-success); }
fieldset { margin: 0; padding: 0; min-width: 0; }
label { display: block; margin-top: 20px; color: var(--text-secondary); font-size: 12px; }
input, select { box-sizing: border-box; width: 100%; padding: 11px 12px; margin-top: 7px; border-radius: 6px; }
input[readonly] { color: var(--text-secondary); background: var(--surface-inset); }
input:disabled, select:disabled, fieldset:disabled { opacity: .6; }
.hint { font-size: 11px; line-height: 1.8; color: var(--text-dim); margin: 7px 0 0; }
.key-heading { display: flex; justify-content: space-between; align-items: center; margin-top: 20px; }
.key-heading label { margin: 0; }
.key-state { font-size: 10px; color: var(--text-dim); }
.key-state.configured { color: var(--text-success); }
.key-input { display: flex; gap: 7px; align-items: stretch; margin-top: 7px; }
.key-input input { margin: 0; min-width: 0; }
.key-input button { flex: 0 0 auto; }
.check { display: flex; align-items: center; gap: 7px; margin-top: 10px; font-size: 11px; }
.check input { width: auto; margin: 0; }
.timeout-label { max-width: 170px; }
.endpoint { display: flex; flex-direction: column; gap: 7px; padding: 12px; margin-top: 22px; border-radius: 6px; background: var(--surface-card); font-size: 10px; color: var(--text-dim); }
.endpoint code { font-size: 11px; color: var(--antique-gold); overflow-wrap: anywhere; }
footer { display: flex; align-items: center; gap: 10px; margin-top: 22px; }
footer .btn-primary { margin-left: auto; }
button { padding: 10px 15px; font-size: .8125rem; cursor: pointer; border-radius: 6px; }
.test-hint { margin-top: 12px; }
.aside { display: flex; flex-direction: column; gap: 16px; }
.test-card { min-height: 190px; box-sizing: border-box; }
.muted { color: var(--text-secondary); font-size: 12px; line-height: 1.9; }
.test-card .muted { margin-top: 20px; }
.latency { color: var(--text-success); font-size: 36px; margin: 18px 0 0; }
.latency span { font-size: 12px; color: var(--text-secondary); }
blockquote { white-space: pre-wrap; overflow-wrap: anywhere; padding: 12px; margin: 16px 0 0; background: var(--surface-card); color: var(--text-primary); font-size: 13px; border-radius: var(--radius-sm); }
.usage-card h2 { font-size: 15px; }
.usage-card p { color: var(--text-secondary); font-size: 12px; line-height: 1.9; margin: 13px 0 0; margin-bottom: 12px; }
.banner { padding: 12px 16px; margin: 0 0 18px; border-radius: 7px; font-size: 12px; }
.error, .storage-error { color: var(--text-danger); }
.error { background: var(--surface-danger); }
.success { color: var(--text-success); background: var(--surface-success); }
.storage-error { font-size: 11px; line-height: 1.8; }
@media (max-width:900px) {
  .settings-layout { grid-template-columns: minmax(0,1fr); }
  .aside { display: grid; grid-template-columns: 1fr 1fr; }
  .test-card { min-height: 0; }
}
@media (max-width:600px) {
  .settings-card, .test-card, .usage-card { padding: 18px; background: var(--surface-panel); }
  .aside { display: flex; }
  .card-heading { align-items: flex-start; flex-direction: column; }
  .badge { align-self: flex-start; }
  footer { flex-wrap: wrap; }
  footer .muted { order: 3; width: 100%; margin: 0; }
}
</style>
