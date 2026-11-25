import { spawnSync } from 'node:child_process'
import { copyFileSync, existsSync, mkdirSync, rmSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const repositoryDir = resolve(frontendDir, '..')
const jarOnly = process.argv.includes('--jar-only')
const isWindows = process.platform === 'win32'

if (process.env.EXTERNAL_SERVER === 'true') {
  console.log('[Backend Build] External server mode; skipping backend build.')
  process.exit(0)
}

const mavenWrapper = join(repositoryDir, isWindows ? 'mvnw.cmd' : 'mvnw')
const mavenCommand = existsSync(mavenWrapper) ? mavenWrapper : (isWindows ? 'mvn.cmd' : 'mvn')
run(mavenCommand, ['-q', '-DskipTests', 'clean', 'package'], repositoryDir, 'Maven backend build failed')

const sourceJar = join(repositoryDir, 'target', 'dmhelper-backend.jar')
if (!existsSync(sourceJar)) {
  fail(`Backend JAR was not created: ${sourceJar}`)
}

const coreDir = join(frontendDir, 'core')
mkdirSync(coreDir, { recursive: true })
copyFileSync(sourceJar, join(coreDir, 'dmhelper-backend.jar'))
console.log('[Backend Build] Copied backend JAR into frontend/core.')

if (!jarOnly) {
  const runtimeDir = join(frontendDir, 'runtime')
  rmSync(runtimeDir, { recursive: true, force: true })
  const jlinkCommand = resolveJlink()
  run(jlinkCommand, [
    '--add-modules', 'java.se,jdk.unsupported',
    '--strip-debug',
    '--no-header-files',
    '--no-man-pages',
    '--compress=2',
    '--output', runtimeDir
  ], repositoryDir, 'jlink runtime build failed')
  console.log('[Backend Build] Created bundled Java runtime.')
}

function resolveJlink() {
  if (process.env.JAVA_HOME) {
    const candidate = join(process.env.JAVA_HOME, 'bin', isWindows ? 'jlink.exe' : 'jlink')
    if (existsSync(candidate)) return candidate
  }
  return isWindows ? 'jlink.exe' : 'jlink'
}

function run(command, args, cwd, errorMessage) {
  const result = spawnSync(command, args, {
    cwd,
    env: process.env,
    stdio: 'inherit',
    shell: isWindows && command.endsWith('.cmd')
  })
  if (result.error) fail(`${errorMessage}: ${result.error.message}`)
  if (result.status !== 0) fail(`${errorMessage} (exit ${result.status})`)
}

function fail(message) {
  console.error(`[Backend Build] ${message}`)
  process.exit(1)
}
