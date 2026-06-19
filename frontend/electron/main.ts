import { app, BrowserWindow, ipcMain } from 'electron'
import { spawn, ChildProcess } from 'child_process'
import { join } from 'path'
import { existsSync } from 'fs'

// --- Java Backend Manager ---

let javaProcess: ChildProcess | null = null

function startBackend() {
  if (process.env.EXTERNAL_SERVER === 'true') {
    console.log('[Backend] Running in external server mode (Java already started). Skipping spawn.')
    return
  }

  const isDev = process.env.VITE_DEV_SERVER_URL ? true : false
  let jarPath = join(app.getAppPath(), 'core', 'dmhelper-backend.jar')
  
  if (!existsSync(jarPath)) {
    jarPath = join(process.cwd(), 'core', 'dmhelper-backend.jar')
  }

  if (!existsSync(jarPath)) {
    console.log('[Backend] JAR not found at expected paths. Frontend will run standalone.')
    return
  }

  console.log(`[Backend] Starting Java backend: ${jarPath}`)
  javaProcess = spawn('java', ['-jar', jarPath, '--server'])

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
      contextIsolation: true
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

app.whenReady().then(() => {
  startBackend()
  createWindow()
})

app.on('window-all-closed', () => {
  stopBackend()
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
