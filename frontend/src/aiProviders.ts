export type AiProvider = 'openai' | 'gemini' | 'deepseek' | 'custom'

// Official API endpoints and text model IDs verified against provider documentation.
export const aiProviders = {
  openai: {
    label: 'GPT（OpenAI）',
    baseUrl: 'https://api.openai.com/v1',
    models: [
      { id: 'gpt-5.4-mini', label: 'GPT-5.4 Mini' },
      { id: 'gpt-5-mini', label: 'GPT-5 Mini' },
      { id: 'gpt-5.4', label: 'GPT-5.4' }
    ]
  },
  deepseek: {
    label: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com',
    models: [
      { id: 'deepseek-flash', label: 'DeepSeek Flash' },
      { id: 'deepseek-v4-pro', label: 'DeepSeek V4 Pro' }
    ]
  },
  gemini: {
    label: 'Gemini（Google）',
    baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai',
    models: [
      { id: 'gemini-3.8-flash', label: 'Gemini 3.8 Flash' },
      { id: 'gemini-3.5-flash-lite', label: 'Gemini 3.5 Flash-Lite' },
      { id: 'gemini-4-argon', label: 'Gemini 4 Argon' }
    ]
  }
}

export function providerForBaseUrl(baseUrl: string): AiProvider {
  try {
    const url = new URL(baseUrl)
    if (url.username || url.password || url.search || url.hash) return 'custom'
    const normalized = url.href.replace(/\/+$/, '').replace(/\/chat\/completions$/, '')
    for (const provider of Object.keys(aiProviders) as Exclude<AiProvider, 'custom'>[]) {
      if (normalized === aiProviders[provider].baseUrl) return provider
    }
  } catch { /* An empty or unfinished custom address has no provider preset. */ }
  return 'custom'
}

export function sameApiOrigin(first: string, second: string): boolean {
  try { return new URL(first).origin === new URL(second).origin } catch { return false }
}
