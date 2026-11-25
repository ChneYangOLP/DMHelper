import { contextBridge, ipcRenderer } from 'electron'

const apiTokenArgument = process.argv.find((argument) => argument.startsWith('--dmhelper-api-token='))
const apiToken = apiTokenArgument?.slice('--dmhelper-api-token='.length) || process.env.DMHELPER_API_TOKEN || ''

contextBridge.exposeInMainWorld('electronAPI', {
  apiToken,
  ai: {
    get: () => ipcRenderer.invoke('ai:get-settings'),
    save: (draft: unknown) => ipcRenderer.invoke('ai:save-settings', draft),
    test: (draft: unknown) => ipcRenderer.invoke('ai:test-connection', draft),
    cancelTest: () => ipcRenderer.invoke('ai:cancel-test'),
    generate: (request: unknown) => ipcRenderer.invoke('ai:generate', request),
    cancelGeneration: () => ipcRenderer.invoke('ai:cancel-generation')
  },
  minimizeWindow: () => ipcRenderer.invoke('window:minimize'),
  maximizeWindow: () => ipcRenderer.invoke('window:maximize'),
  closeWindow: () => ipcRenderer.invoke('window:close'),
  isMaximized: () => ipcRenderer.invoke('window:isMaximized')
})
