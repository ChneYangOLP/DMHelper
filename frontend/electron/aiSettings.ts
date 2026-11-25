import { mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { randomUUID } from 'node:crypto'
import type { AiAssistantResult, AiMessage, AiSettingsDraft, AiSettingsView, AiTestResult } from '../src/aiTypes'

interface SecretStorage {
  isEncryptionAvailable: () => boolean
  encryptString: (value: string) => Buffer
  decryptString: (value: Buffer) => string
  getSelectedStorageBackend?: () => string
}
interface StoredSettings {
  version: 1
  baseUrl: string
  model: string
  timeoutSeconds: number
  encryptedApiKey: string
}
const defaults = (): StoredSettings => ({ version: 1, baseUrl: '', model: '', timeoutSeconds: 60, encryptedApiKey: '' })

export function normalizeBaseUrl(value: string): string {
  let url: URL
  try { url = new URL(value.trim()) } catch { throw new Error('请填写完整的 API 地址。') }
  const local = ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)
  if (url.protocol !== 'https:' && !(url.protocol === 'http:' && local)) throw new Error('远程 API 请使用 HTTPS；本机服务可以使用 HTTP。')
  if (url.username || url.password || url.search || url.hash) throw new Error('API 地址不能包含账号、密码、查询参数或片段。')
  url.pathname = url.pathname.replace(/\/+$/, '').replace(/\/chat\/completions$/, '')
  return url.toString().replace(/\/$/, '')
}

export class AiSettingsService {
  private activeTest: AbortController | null = null
  private activeGeneration: AbortController | null = null
  private writing = false
  constructor(private readonly path: string, private readonly secrets: SecretStorage) {}

  private encryptionAvailable() {
    return this.secrets.isEncryptionAvailable() && this.secrets.getSelectedStorageBackend?.() !== 'basic_text'
  }
  private async read(): Promise<StoredSettings> {
    let text: string
    try { text = await readFile(this.path, 'utf8') }
    catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') return defaults()
      throw new Error('无法读取 AI 配置，请检查应用数据目录的访问权限。')
    }
    try {
      const data = JSON.parse(text) as StoredSettings
      if (data.version !== 1 || typeof data.baseUrl !== 'string' || typeof data.model !== 'string'
        || !Number.isInteger(data.timeoutSeconds) || data.timeoutSeconds < 5 || data.timeoutSeconds > 180
        || typeof data.encryptedApiKey !== 'string') throw new Error()
      return data
    } catch { throw new Error('AI 配置文件损坏，无法读取；请先备份并移除 ai-settings.json 后重新配置。') }
  }
  private view(settings: StoredSettings): AiSettingsView {
    return { baseUrl: settings.baseUrl, model: settings.model, timeoutSeconds: settings.timeoutSeconds,
      hasApiKey: Boolean(settings.encryptedApiKey), secureStorageAvailable: this.encryptionAvailable() }
  }
  async get(): Promise<AiSettingsView> { return this.view(await this.read()) }

  private validate(draft: AiSettingsDraft) {
    if (!draft || typeof draft.baseUrl !== 'string' || typeof draft.model !== 'string' || typeof draft.apiKey !== 'string'
      || typeof draft.clearApiKey !== 'boolean') throw new Error('AI 配置格式无效。')
    const baseUrl = normalizeBaseUrl(draft.baseUrl)
    const model = draft.model.trim()
    if (!model || model.length > 200 || /[\r\n]/.test(model)) throw new Error('模型名称需为 1–200 个字符。')
    if (!Number.isInteger(draft.timeoutSeconds) || draft.timeoutSeconds < 5 || draft.timeoutSeconds > 180)
      throw new Error('超时时间需为 5–180 秒的整数。')
    const apiKey = draft.apiKey.trim()
    if (apiKey.length > 8192 || /[^\x21-\x7e]/.test(apiKey)) throw new Error('API Key 格式无效，请检查是否包含空格或换行。')
    if (draft.clearApiKey && apiKey) throw new Error('清除密钥与填写新密钥不能同时使用。')
    return { baseUrl, model, apiKey, timeoutSeconds: draft.timeoutSeconds }
  }
  private canKeepKey(previous: StoredSettings, baseUrl: string, draft: AiSettingsDraft) {
    if (!previous.encryptedApiKey || draft.apiKey.trim() || draft.clearApiKey) return false
    if (new URL(previous.baseUrl).origin !== new URL(baseUrl).origin)
      throw new Error('服务地址已更换，请重新填写密钥，或选择清除已保存密钥。')
    return true
  }
  async save(draft: AiSettingsDraft): Promise<AiSettingsView> {
    if (this.writing) throw new Error('配置正在保存，请稍后重试。')
    this.writing = true
    try {
      const clean = this.validate(draft)
      const previous = await this.read()
      const keep = this.canKeepKey(previous, clean.baseUrl, draft)
      let encryptedApiKey = keep ? previous.encryptedApiKey : ''
      if (clean.apiKey) {
        if (!this.encryptionAvailable()) throw new Error('系统安全存储不可用，无法保存密钥；请先启用系统凭据服务。')
        try { encryptedApiKey = this.secrets.encryptString(clean.apiKey).toString('base64') }
        catch { throw new Error('系统加密密钥失败，原有配置已保留。') }
      }
      const settings: StoredSettings = { version: 1, baseUrl: clean.baseUrl, model: clean.model, timeoutSeconds: clean.timeoutSeconds, encryptedApiKey }
      await mkdir(dirname(this.path), { recursive: true })
      // Rename only after a complete write so an interrupted save keeps the previous configuration.
      const temp = `${this.path}.${randomUUID()}.tmp`
      try {
        await writeFile(temp, JSON.stringify(settings, null, 2), { mode: 0o600 })
        await rename(temp, this.path)
      } catch {
        await rm(temp, { force: true }).catch(() => {})
        throw new Error('AI 配置保存失败，请检查应用数据目录的访问权限。')
      }
      return this.view(settings)
    } finally { this.writing = false }
  }
  cancelTest() { this.activeTest?.abort() }
  cancelGeneration() { this.activeGeneration?.abort() }

  async test(draft: AiSettingsDraft): Promise<AiTestResult> {
    if (this.activeTest) throw new Error('已有连接测试正在进行。')
    const controller = new AbortController()
    this.activeTest = controller
    try {
      const result = await this.request(draft, [{ role: 'user', content: 'Reply with only OK.' }], 256, controller, '连接测试')
      return { latencyMs: result.latencyMs, reply: result.reply.slice(0, 240) }
    } finally { this.activeTest = null }
  }

  async generate(messages: AiMessage[]): Promise<AiAssistantResult> {
    if (this.activeGeneration) throw new Error('已有 AI 回答正在生成，请等待或取消。')
    const controller = new AbortController()
    this.activeGeneration = controller
    try { return await this.request(null, messages, 8192, controller, 'AI 生成') }
    finally { this.activeGeneration = null }
  }

  private async request(draft: AiSettingsDraft | null, messages: AiMessage[], tokenLimit: number,
    controller: AbortController, operation: string): Promise<AiAssistantResult> {
    let timer: ReturnType<typeof setTimeout> | undefined
    let timedOut = false
    try {
      const previous = await this.read()
      if (!draft && (!previous.baseUrl || !previous.model)) throw new Error('请先在“AI 配置”中保存模型连接。')
      const input = draft ?? { ...previous, apiKey: '', clearApiKey: false }
      const clean = this.validate(input)
      const keep = this.canKeepKey(previous, clean.baseUrl, input)
      let apiKey = clean.apiKey
      if (keep) {
        if (!this.encryptionAvailable()) throw new Error('系统安全存储不可用，无法读取已保存密钥。')
        try { apiKey = this.secrets.decryptString(Buffer.from(previous.encryptedApiKey, 'base64')) }
        catch { throw new Error('已保存密钥无法解密，请重新填写密钥。') }
      }
      if (controller.signal.aborted) throw new Error(`${operation}已取消。`)
      timer = setTimeout(() => { timedOut = true; controller.abort() }, clean.timeoutSeconds * 1000)
      const started = Date.now()
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      if (apiKey) headers.Authorization = `Bearer ${apiKey}`
      const deepseek = new URL(clean.baseUrl).origin === 'https://api.deepseek.com'
      const request = async (limitField: string) => {
        const response = await fetch(`${clean.baseUrl}/chat/completions`, {
          method: 'POST', headers, signal: controller.signal, redirect: 'manual',
          body: JSON.stringify({ model: clean.model, messages, stream: false,
            [limitField]: tokenLimit, ...(deepseek ? { thinking: { type: 'disabled' } } : {}) })
        })
        const reader = response.body?.getReader()
        const chunks: Uint8Array[] = []
        let length = 0
        if (reader) {
          try {
            while (true) {
              const chunk = await reader.read()
              if (chunk.done) break
              length += chunk.value.byteLength
              if (length > 1024 * 1024) { await reader.cancel(); throw new Error('服务返回内容过大，请检查 API 地址与接口格式。') }
              chunks.push(chunk.value)
            }
          } finally { reader.releaseLock() }
        }
        return { response, text: Buffer.concat(chunks).toString('utf8') }
      }
      let result: Awaited<ReturnType<typeof request>>
      try {
        result = await request(deepseek ? 'max_tokens' : 'max_completion_tokens')
        // Older compatible services only accept max_tokens. Retry only a rejected parameter, never a generation.
        if (result.response.status === 400 && /max_completion_tokens/i.test(result.text)
          && /unsupported|unknown|unrecognized|unexpected|not\s+(allowed|permitted|supported)|extra|不支持/i.test(result.text)) {
          result = await request('max_tokens')
        }
      } catch (error) {
        if (controller.signal.aborted) throw new Error(timedOut ? `请求超时（${clean.timeoutSeconds} 秒），请检查地址与网络。` : `${operation}已取消。`)
        if (error instanceof Error && error.message.startsWith('服务返回内容过大')) throw error
        throw new Error('无法连接模型服务，请检查地址、网络及证书。')
      }
      const status = result.response.status
      if (!result.response.ok) {
        const reason = status === 401 || status === 403 ? '密钥无效或没有访问权限。'
          : status === 404 ? '接口或模型不存在，请检查 API 地址和模型名称。'
          : status === 429 ? '请求受限或额度不足，请检查服务商账户。'
          : status >= 300 && status < 400 ? '服务返回重定向，请直接填写最终 API 地址。'
          : status >= 500 ? '模型服务暂时不可用，请稍后重试。' : '服务拒绝请求，请确认模型支持 Chat Completions 接口。'
        throw new Error(`${operation}失败（HTTP ${status}）：${reason}`)
      }
      let reply: unknown
      let finishReason: unknown
      try {
        const choice = JSON.parse(result.text).choices?.[0]
        reply = choice?.message?.content
        finishReason = choice?.finish_reason
      }
      catch { throw new Error('服务返回的不是有效 JSON，请检查 API 地址。') }
      if (typeof reply !== 'string' || !reply.trim()) throw new Error('接口已响应，但没有返回有效文本，请检查模型或接口兼容性。')
      const safeReply = apiKey ? reply.split(apiKey).join('[已隐藏密钥]') : reply
      return { latencyMs: Date.now() - started, reply: safeReply.trim().slice(0, 32000), model: clean.model,
        truncated: finishReason === 'length' || safeReply.trim().length > 32000 }
    } finally {
      clearTimeout(timer)
    }
  }
}
