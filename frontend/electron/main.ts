import { app, BrowserWindow, dialog, ipcMain } from 'electron'
import { spawn, ChildProcess } from 'child_process'
import { randomBytes } from 'crypto'
import { join } from 'path'
import { existsSync } from 'fs'
import { pathToFileURL } from 'node:url'
import { registerAiSettingsIpc } from './aiIpc'

// --- Java Backend Manager ---

let javaProcess: ChildProcess | null = null
const apiToken = process.env.DMHELPER_API_TOKEN || randomBytes(32).toString('hex')
process.env.DMHELPER_API_TOKEN = apiToken

function packagedResource(...parts: string[]): string {
  return join(app.isPackaged ? process.resourcesPath : app.getAppPath(), ...parts)
}

function resolveJavaExecutable(): string {
  const bundledJava = packagedResource('runtime', 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
  if (existsSync(bundledJava)) return bundledJava

  const javaHome = process.env.JAVA_HOME
  if (javaHome) {
    const javaFromHome = join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
    if (existsSync(javaFromHome)) return javaFromHome
  }
  return process.platform === 'win32' ? 'java.exe' : 'java'
}

async function waitForBackend(timeoutMs = 15000): Promise<boolean> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    try {
      const response = await fetch('http://127.0.0.1:8080/api/ping', {
        headers: { 'X-DMHelper-Token': apiToken }
      })
      if (response.ok) return true
    } catch {
      // The backend may still be starting.
    }
    await new Promise(resolve => setTimeout(resolve, 200))
  }
  return false
}

async function startBackend(): Promise<boolean> {
  if (process.env.EXTERNAL_SERVER === 'true') {
    console.log('[Backend] Running in external server mode (Java already started). Skipping spawn.')
    return waitForBackend()
  }

  let jarPath = packagedResource('core', 'dmhelper-backend.jar')
  
  if (!existsSync(jarPath)) {
    jarPath = join(process.cwd(), 'core', 'dmhelper-backend.jar')
  }

  if (!existsSync(jarPath)) {
    console.error('[Backend] JAR not found at expected paths.')
    return false
  }

  console.log(`[Backend] Starting Java backend: ${jarPath}`)
  javaProcess = spawn(resolveJavaExecutable(), ['-jar', jarPath], {
    env: {
      ...process.env,
      DMHELPER_API_TOKEN: apiToken
    }
  })

  javaProcess.stdout?.on('data', (data) => {
    console.log(`[Backend] ${data.toString().trim()}`)
  })

  javaProcess.stderr?.on('data', (data: Buffer) => {
    console.error('[Backend Error]', data.toString().trim())
  })

  javaProcess.on('close', (code) => {
    console.log(`[Backend] Process exited with code ${code}`)
    javaProcess = null
  })
  
  javaProcess.on('error', (err) => {
    console.error(`[Backend] Failed to start Java process:`, err)
    javaProcess = null
  })
  return waitForBackend()
}

function stopBackend(): void {
  if (javaProcess && !javaProcess.killed) {
    console.log('[Backend] Stopping Java backend...')
    javaProcess.kill()
    javaProcess = null
  }
}

// --- Window Creation ---

let mainWindow: BrowserWindow | null = null

function createWindow(): void {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1024,
    minHeight: 700,
    frame: false,
    titleBarStyle: 'hidden',
    backgroundColor: '#1a1410',
    show: false,
    webPreferences: {
      preload: join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      additionalArguments: [`--dmhelper-api-token=${apiToken}`]
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow?.show()
  })

  // Load content
  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
  } else {
    mainWindow.loadFile(join(__dirname, '../dist/index.html'))
  }

  // --- IPC Handlers ---
  ipcMain.handle('window:minimize', () => {
    mainWindow?.minimize()
  })

  ipcMain.handle('window:maximize', () => {
    if (mainWindow?.isMaximized()) {
      mainWindow.unmaximize()
    } else {
      mainWindow?.maximize()
    }
  })

  ipcMain.handle('window:close', () => {
    mainWindow?.close()
  })

  ipcMain.handle('window:isMaximized', () => {
    return mainWindow?.isMaximized() ?? false
  })
}

// --- App Lifecycle ---

app.whenReady().then(async () => {
  const ai = registerAiSettingsIpc(() => mainWindow,
    process.env.VITE_DEV_SERVER_URL || pathToFileURL(join(__dirname, '../dist/index.html')).href)
  app.on('before-quit', () => { ai.cancelTest(); ai.cancelGeneration() })
  const backendReady = await startBackend()
  if (!backendReady) {
    dialog.showErrorBox('DMHelper 启动失败', '本地规则服务未能启动。请重新安装应用，或检查日志中的 Java 后端错误。')
    app.quit()
    return
  }
  createWindow()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('before-quit', () => {
  stopBackend()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})
