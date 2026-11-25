# Electron 生成安装器教程

本文以 DMHelper 当前工程为例，介绍如何使用 `electron-builder` 将 Electron 应用打包为 Windows 安装器，并说明如何允许用户在安装时选择安装路径。

## 1. 打包流程概览

DMHelper 的桌面版由以下几部分组成：

- Vue 3 + Vite 前端
- Electron 主进程
- Java 后端 JAR
- 通过 `jlink` 生成的精简 Java 运行时
- `electron-builder` 生成的 NSIS 安装器

完整流程如下：

```text
Java 后端源码
    ↓ Maven package
dmhelper-backend.jar
    ↓ 复制到 frontend/core

JDK
    ↓ jlink
frontend/runtime

Vue 与 Electron 源码
    ↓ Vite build
frontend/dist + frontend/dist-electron

上述产物
    ↓ electron-builder
Windows NSIS 安装器（.exe）
```

## 2. 环境要求

在 Windows 构建机上安装：

- Node.js，建议使用当前 LTS 版本
- npm
- JDK 17 或更高版本，并正确设置 `JAVA_HOME`
- Maven 3.9 或更高版本；本项目也可以使用仓库内的 Maven Wrapper

在 PowerShell 中检查环境：

```powershell
node --version
npm --version
java -version
mvn --version
```

如果系统找不到 `jlink`，请确认 `%JAVA_HOME%\bin\jlink.exe` 存在。

## 3. 安装打包依赖

进入前端目录并安装锁定版本的依赖：

```powershell
cd D:\idealproject\DMHelper\frontend
npm ci
```

项目使用的关键开发依赖如下：

```json
{
  "devDependencies": {
    "electron": "^35.0.1",
    "electron-builder": "^26.0.12"
  }
}
```

新项目可使用下面的命令安装：

```powershell
npm install --save-dev electron electron-builder
```

## 4. 配置 package.json

Electron 应用至少需要通过 `main` 指定主进程入口：

```json
{
  "main": "dist-electron/main.js"
}
```

DMHelper 当前使用的打包脚本为：

```json
{
  "scripts": {
    "backend:build": "node scripts/build-backend.mjs",
    "electron:build": "npm run backend:build && vite build && electron-builder"
  }
}
```

其中 `backend:build` 会完成以下工作：

1. 使用 Maven 生成 `target/dmhelper-backend.jar`。
2. 将 JAR 复制到 `frontend/core`。
3. 使用 `jlink` 在 `frontend/runtime` 中生成内置 Java 运行时。

### electron-builder 配置

当前项目的核心配置如下：

```json
{
  "build": {
    "appId": "com.dmhelper.desktop",
    "productName": "DMHelper",
    "asar": true,
    "directories": {
      "output": "release"
    },
    "files": [
      "dist/index.html",
      "dist/assets/**/*",
      "dist-electron/**/*",
      "package.json"
    ],
    "extraResources": [
      {
        "from": "core",
        "to": "core",
        "filter": ["**/*"]
      },
      {
        "from": "runtime",
        "to": "runtime",
        "filter": ["**/*"]
      }
    ],
    "win": {
      "icon": "../src/main/java/com/DMHelper/assets/app_icon.ico",
      "target": "nsis"
    },
    "nsis": {
      "oneClick": false,
      "allowToChangeInstallationDirectory": true
    }
  }
}
```

各配置项的作用：

| 配置项 | 作用 |
| --- | --- |
| `appId` | 应用的唯一标识，发布后不要随意更改 |
| `productName` | 安装器和应用显示的名称 |
| `asar` | 将应用代码封装到 `app.asar` 中 |
| `directories.output` | 指定安装器输出目录 |
| `files` | 控制打包进应用主体的文件，避免把源码和无关文件带入安装包 |
| `extraResources` | 将后端 JAR、Java 运行时等资源放入安装目录的 `resources` 下 |
| `win.target` | Windows 使用 NSIS 安装器 |
| `oneClick: false` | 使用分步骤安装向导，而不是静默的一键安装 |
| `allowToChangeInstallationDirectory: true` | 在安装向导中允许用户选择安装路径 |

如果使用 `oneClick: true`，安装器通常会直接安装到默认目录，不显示路径选择页面。

## 5. 生成 Windows 安装器

在 `frontend` 目录执行：

```powershell
npm run electron:build
```

该命令会依次构建后端、Java 运行时、前端和 Electron 安装器。成功后产物位于：

```text
D:\idealproject\DMHelper\frontend\release\DMHelper Setup 1.0.0.exe
```

如果 `core`、`runtime`、`dist` 和 `dist-electron` 都已经是最新的，只重新生成 Windows 安装器可以执行：

```powershell
npx electron-builder --win
```

正式发布时建议运行完整的 `npm run electron:build`，以免安装器包含旧的前端或后端文件。

## 6. 安装路径

当前安装器为按用户安装，默认目录通常是：

```text
%LOCALAPPDATA%\Programs\DMHelper
```

由于配置了：

```json
{
  "oneClick": false,
  "allowToChangeInstallationDirectory": true
}
```

用户可以在安装向导中点击“浏览”并选择其他目录。

如果需要为所有用户安装，可以进一步配置 `perMachine`，但这通常需要管理员权限：

```json
{
  "nsis": {
    "oneClick": false,
    "allowToChangeInstallationDirectory": true,
    "perMachine": true
  }
}
```

是否启用 `perMachine` 应在发布前确定，因为它会改变安装权限和默认目录。

## 7. 在程序中访问额外资源

打包后，`extraResources` 中的文件位于 Electron 的 `process.resourcesPath` 下。例如：

```ts
import { app } from 'electron'
import { join } from 'node:path'

const jarPath = join(process.resourcesPath, 'core', 'dmhelper-backend.jar')
const javaPath = join(
  process.resourcesPath,
  'runtime',
  'bin',
  process.platform === 'win32' ? 'java.exe' : 'java'
)
```

开发模式下 `process.resourcesPath` 与打包后的目录不同，因此实际项目通常需要通过 `app.isPackaged` 区分路径。

## 8. 修改版本号和安装器名称

安装器文件名中的版本来自 `package.json`：

```json
{
  "version": "1.0.0"
}
```

例如改成 `1.1.0` 后重新打包，默认会生成：

```text
DMHelper Setup 1.1.0.exe
```

建议遵循语义化版本格式：

- 修复缺陷：`1.0.0` → `1.0.1`
- 向后兼容的新功能：`1.0.0` → `1.1.0`
- 不兼容的大改动：`1.0.0` → `2.0.0`

## 9. 下载超时或网络错误

首次打包时，Electron 和 NSIS 相关文件可能需要从网络下载。如果连接 GitHub 超时，可以先重试：

```powershell
npx electron-builder --win
```

也可以在当前 PowerShell 会话中设置 Electron 镜像：

```powershell
$env:ELECTRON_MIRROR = 'https://npmmirror.com/mirrors/electron/'
npx electron-builder --win
```

如果本机已经有完整的 Electron ZIP 缓存，也可以将其解压到一个临时目录，并让 `electron-builder` 直接使用该目录。该目录根部必须包含 `electron.exe`：

```powershell
npx electron-builder --win --config.electronDist=.electron-dist-35.7.5
```

这种方法中的目录名和 Electron 版本必须与项目实际安装的版本一致。临时解压目录不应提交到 Git。

## 10. 常见问题

### 安装包体积异常大

检查 `build.files`，不要直接把整个前端目录打进安装包，否则可能把 `node_modules`、缓存和构建产物重复包含进去。对于额外资源，应使用 `extraResources` 精确声明。

### 安装后找不到后端 JAR

检查以下内容：

- `frontend/core/dmhelper-backend.jar` 是否在打包前生成。
- `extraResources` 是否包含 `core`。
- 程序是否从 `process.resourcesPath/core` 读取 JAR。

### 安装后的电脑没有 Java

本项目会使用 `jlink` 生成 `frontend/runtime` 并打包，因此目标电脑不需要单独安装 Java。应确认安装目录的 `resources/runtime/bin/java.exe` 存在。

### 安装时不能选择目录

确认 NSIS 配置同时包含：

```json
{
  "oneClick": false,
  "allowToChangeInstallationDirectory": true
}
```

修改配置后必须重新生成安装器，旧安装器不会自动更新。

### Windows 提示“未知发布者”

这是安装器没有代码签名时的常见提示。正式对外发布时，应购买代码签名证书，并配置 `electron-builder` 的 Windows 签名参数。不要通过关闭系统安全功能来规避提示。

## 11. 发布前检查清单

每次发布前建议检查：

- `package.json` 中的版本号正确。
- 前端生产构建没有 TypeScript 或 Vite 错误。
- Java 后端测试通过，JAR 可以独立启动。
- 内置 Java 运行时已生成。
- 安装器能够选择安装目录。
- 在一台没有开发环境的 Windows 电脑或虚拟机中测试安装。
- 应用能够启动，后端接口能够响应。
- 开始菜单、桌面快捷方式及卸载功能正常。
- 卸载后不会误删用户自行保存的数据。
- 对外发布的安装器已完成代码签名和安全扫描。

## 12. DMHelper 快速打包命令

```powershell
cd D:\idealproject\DMHelper\frontend
npm ci
npm run electron:build
```

最终安装器：

```text
D:\idealproject\DMHelper\frontend\release\DMHelper Setup 1.0.0.exe
```
