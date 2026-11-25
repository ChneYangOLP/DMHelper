export interface AiSettingsDraft {
  baseUrl: string
  model: string
  timeoutSeconds: number
  apiKey: string
  clearApiKey: boolean
}
export interface AiSettingsView {
  baseUrl: string
  model: string
  timeoutSeconds: number
  hasApiKey: boolean
  secureStorageAvailable: boolean
}
export interface AiTestResult { latencyMs: number; reply: string }
export type AiAssistantMode = 'recap' | 'stories' | 'progress' | 'destination'
export interface AiAssistantRequest {
  mode: AiAssistantMode
  question: string
  context: string
}
export interface AiAssistantResult extends AiTestResult {
  model: string
  truncated: boolean
}
export interface AiMessage { role: 'system' | 'user'; content: string }
export type AiReply<T> = { ok: true; value: T } | { ok: false; error: string }
export interface AiSettingsBridge {
  get: () => Promise<AiReply<AiSettingsView>>
  save: (draft: AiSettingsDraft) => Promise<AiReply<AiSettingsView>>
  test: (draft: AiSettingsDraft) => Promise<AiReply<AiTestResult>>
  cancelTest: () => Promise<AiReply<null>>
  generate: (request: AiAssistantRequest) => Promise<AiReply<AiAssistantResult>>
  cancelGeneration: () => Promise<AiReply<null>>
}
