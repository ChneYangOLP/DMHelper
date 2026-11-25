import { app, ipcMain, safeStorage } from 'electron'
import type { BrowserWindow, IpcMainInvokeEvent } from 'electron'
import { join } from 'node:path'
import { AiSettingsService } from './aiSettings'
import { buildAssistantMessages } from '../src/aiAssistant'
import type { AiAssistantRequest, AiReply, AiSettingsDraft } from '../src/aiTypes'

export function registerAiSettingsIpc(getWindow: () => BrowserWindow | null, pageUrl: string,
  settingsPath = join(app.getPath('userData'), 'ai-settings.json')) {
  const service = new AiSettingsService(settingsPath, {
    isEncryptionAvailable: () => safeStorage.isEncryptionAvailable(),
    encryptString: value => safeStorage.encryptString(value),
    decryptString: value => safeStorage.decryptString(value),
    getSelectedStorageBackend: () => process.platform === 'linux' ? safeStorage.getSelectedStorageBackend() : 'system'
  })
  const trusted = (event: IpcMainInvokeEvent) => {
    const window = getWindow()
    if (!window || window.isDestroyed() || event.sender !== window.webContents || event.senderFrame !== window.webContents.mainFrame) return false
    const actual = new URL(event.senderFrame.url), expected = new URL(pageUrl)
    return actual.origin === expected.origin && actual.pathname === expected.pathname
  }
  const handle = <T, Input = AiSettingsDraft>(channel: string, operation: (draft: Input) => Promise<T> | T) => {
    ipcMain.handle(channel, async (event, draft): Promise<AiReply<T>> => {
      try {
        if (!trusted(event)) return { ok: false, error: '此页面无权访问 AI 功能。' }
        return { ok: true, value: await operation(draft) }
      } catch (error) {
        return { ok: false, error: error instanceof Error ? error.message : 'AI 操作失败，请重试。' }
      }
    })
  }
  handle('ai:get-settings', () => service.get())
  handle('ai:save-settings', draft => service.save(draft))
  handle('ai:test-connection', draft => service.test(draft))
  handle('ai:cancel-test', () => { service.cancelTest(); return null })
  handle('ai:generate', (request: AiAssistantRequest) => service.generate(buildAssistantMessages(request)))
  handle('ai:cancel-generation', () => { service.cancelGeneration(); return null })
  return service
}
