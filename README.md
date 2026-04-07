# DMHelper

`DMHelper` 是一个基于 Java Swing 的本地桌面 D&D 5e 辅助工具，定位是单机版 DM 控制台与角色管理器。  
项目当前已经覆盖角色创建、升级选择、法术管理、装备与背包、战斗结算、怪物图鉴、自定义物品和 SQLite 持久化。

## 项目定位

- 面向本地使用，不依赖服务端
- 以 2014 版 D&D 5e 核心规则为主要实现方向
- 目标是“可管理、可记录、可游玩”，不是完全自动化的全规则引擎

## 当前功能

### 角色系统

- 创建角色：姓名、年龄、性别、六维、背景故事、性格、理想、羁绊、缺陷
- 核心种族：人类、矮人、精灵、半身人、龙裔、侏儒、半精灵、半兽人、提夫林
- 子种族/血统选择：
  - 矮人：丘陵矮人、山地矮人
  - 精灵：高等精灵、木精灵、卓尔
  - 半身人：轻足半身人、健壮半身人
  - 侏儒：森林侏儒、岩侏儒
  - 龙裔：10 种龙族血脉
  - 半精灵：可选两项额外 +1 属性
- 种族信息展示：属性加值、速度、体型、语言、黑暗视觉、种族特性摘要

### 职业与成长

- 已实现职业：
  - 战士 `Fighter`
  - 法师 `Wizard`
  - 术士 `Sorcerer`
  - 邪术士 `Warlock`
  - 圣武士 `Paladin`
- 已实现的主要成长内容：
  - 子职业选择
  - 属性值提升 / 专长
  - 技能熟练选择
  - 法术与戏法学习
  - 准备法术管理
  - 战斗大师战技选择
  - 术士超魔
  - 邪术士邪术祈请与契约恩赐
- 当前已覆盖的子职业示例：
  - 战士：冠军勇士、战斗大师
  - 法师：8 大学派
  - 术士：龙脉术士、狂野魔法
  - 邪术士：邪魔恩主、妖精恩主、旧日支配者
  - 圣武士：奉献誓言、远古誓言、复仇誓言

### 装备与背包

- 默认职业装备初始化
- 装备槽位：
  - 护甲
  - 主手
  - 副手 / 盾牌
  - 披风
  - 护符
- 背包支持堆叠、分类查看、双击使用、丢弃
- 内置物品库包含：
  - 常规武器与护甲
  - 消耗品
  - 更多法术卷轴
- 支持自定义装备并写入 SQLite

### 施法系统

- 法师：
  - 法术书
  - 准备法术
  - 奥术回能
- 术士：
  - 已知法术
  - 术法点
  - 超魔
- 邪术士：
  - 契约法术位
  - 邪术祈请
  - 契约恩赐
- 圣武士：
  - 准备法术
  - 圣疗池
  - 神圣感知

### 战斗系统

- 参战角色选择
- 怪物图鉴搜索与遭遇配置
- 先攻排序
- 目标与攻击方式选择
- 攻击结算与日志记录
- 敌人双击移除
- 死亡角色不可加入战斗
- 战斗中支持使用部分物品

### 数据持久化

- 角色数据保存在 SQLite
- 自定义装备单独持久化
- 启动时自动初始化表结构
- 老版本项目根目录数据库会尝试迁移到正式存档目录

## 界面入口

应用主界面目前包含 4 个核心入口：

- `创建角色`
- `角色一览 (只读)`
- `角色管理 (装备与升级)`
- `战斗系统`

启动入口文件：

- [Main.java](/Users/hatys/IdeaProjects/DMHelper/src/main/java/Main.java)

## JavaFX 现代主界面

`Main` 会优先加载 JavaFX 启动器 [`FxLauncher`](/Users/hatys/IdeaProjects/DMHelper/src/main/java/FxLauncher.java)，并把“建角、列表、管理、战斗”四个核心流程全部呈现在 JavaFX 原生界面中；Swing 版本仅作为遗留参考，不再自动弹出。

JavaFX 面板的主题位于 [`main-menu.css`](/Users/hatys/IdeaProjects/DMHelper/src/main/java/com/DMHelper/basic/javafx/main-menu.css)，可以继续在其中调整配色、圆角、阴影等视觉细节。

当前已完成的 JavaFX 窗体：

- `CharacterCreateWindow`：重新设计的角色创建流程，包含基础信息、能力值与背景设定。
- `CharacterRosterWindow`：角色列表与详情页，支持搜索、排序与即时刷新。
- `CharacterManagerWindow`：角色装备/背包/资源的集中查看与基本维护，可执行短休/长休。
- `CombatConsoleWindow`：简化版战斗控制台，支持添加参战者、掷先攻、轮次追踪与日志记录。

### 使用 Maven 运行 JavaFX 版本

1. 安装 JDK 17+ 与 Maven 3.9+。
2. 在仓库根目录执行一次编译（会自动下载 sqlite-jdbc 与 OpenJFX）：

   ```bash
   mvn clean compile
   ```

3. 启动 JavaFX 主界面：

   ```bash
   mvn javafx:run
   ```

   - 默认会根据当前操作系统选择 `javafx.platform`。如果需要覆盖，可显式指定，例如 `mvn -Djavafx.platform=win javafx:run`（Windows）、`mvn -Djavafx.platform=mac javafx:run`（Intel Mac）、`mvn -Djavafx.platform=linux javafx:run`（Linux）。
   - 纯命令行运行也可以使用 `mvn -Dmain.class=Main exec:java` 等方式，但 `javafx:run` 会自动加上 `--add-modules javafx.controls,javafx.graphics`。

4. 若想回到旧体验，可直接在 IDE 中运行 `Main_Menu.main`（Swing 版仍保留）。

## 项目结构

```text
src/
├── main/
│   ├── java/
│   │   ├── Main.java
│   │   ├── FxLauncher.java
│   │   └── com/DMHelper/
│   │       ├── assets/                  # 图标与桌面资源（类路径访问）
│   │       └── basic/
│   │           ├── Character_Sheet.java # 角色核心模型
│   │           ├── ...                  # 其他子模块
│   └── resources/
│       └── META-INF/                    # MANIFEST 等资源
└── test/                                # 预留（当前为空）
```

## 运行环境

- JDK 17 或以上（推荐与系统保持一致，例如 macOS 上的 Temurin/OpenJDK）。
- Maven 3.9 及以上版本。
- OpenJFX 与 sqlite-jdbc 由 Maven 自动拉取，无需在 `lib/` 中手工放置 JAR。

## 本地运行

### 方式一：在 IDE 中运行

1. 以 “Open Existing Maven Project” 方式导入根目录（IDE 会自动识别 `pom.xml` 并下载依赖）。
2. 运行 `Main.main` 即可启动，或直接使用 IDE 的 Maven 面板执行 `javafx:run` 获得同样效果。

### 方式二：命令行编译

Maven 已经封装好了编译流程，直接执行：

```bash
mvn clean compile
```

若只想做一次快速校验，可省略清理步骤：

```bash
mvn -q -DskipTests compile
```

## macOS 打包

当前项目已经可以在 macOS 上直接打包为 `.app` 和 `.dmg`。

仓库内置了打包脚本：

```bash
./scripts/package-macos-dmg.sh
```

产物位置：

- `target/jpackage/dist/DMHelper.app`
- `target/jpackage/dist/DMHelper-1.0.0.dmg`

说明：

- 脚本会自行编译源码、打包应用 jar，并调用 `jpackage` 生成安装包。
- 依赖前提是本机已经安装 JDK 17+，并且本地 Maven 仓库里已有 OpenJFX 与 `sqlite-jdbc` 依赖。
- 当前生成的是可本地安装分发的未公证包；如果要面向普通 macOS 用户直接分发，后续还需要 Apple Developer 签名与 notarization。

## 数据库存档位置

数据库文件名固定为：

- `dnd_data.db`

实际存放位置按操作系统决定：

- macOS
  - `~/Library/Application Support/DMHelper/dnd_data.db`
- Windows
  - `%APPDATA%/DMHelper/dnd_data.db`
- Linux
  - `~/.local/share/DMHelper/dnd_data.db`

兼容逻辑：

- 如果旧版本把 `dnd_data.db` 放在项目根目录，程序首次启动时会尝试迁移

数据库相关实现：

- [DB_Helper.java](/Users/hatys/IdeaProjects/DMHelper/src/main/java/com/DMHelper/basic/database/DB_Helper.java)
- [Init_DB.java](/Users/hatys/IdeaProjects/DMHelper/src/main/java/com/DMHelper/basic/database/Init_DB.java)

## 主要源码入口

- 应用启动：
  - [Main.java](/Users/hatys/IdeaProjects/DMHelper/src/main/java/Main.java)
- 主菜单：
  - [Main_Menu.java](/Users/hatys/IdeaProjects/DMHelper/src/main/java/com/DMHelper/basic/menus/Main_Menu.java)
- 角色创建：
  - [Create_Character_UI.java](/Users/hatys/IdeaProjects/DMHelper/src/main/java/com/DMHelper/basic/menus/Create_Character_UI.java)
- 角色管理：
  - [Character_Manager_UI.java](/Users/hatys/IdeaProjects/DMHelper/src/main/java/com/DMHelper/basic/menus/Character_Manager_UI.java)
- 战斗系统：
  - [Combat_System_UI.java](/Users/hatys/IdeaProjects/DMHelper/src/main/java/com/DMHelper/basic/menus/Combat_System_UI.java)

## 规则实现说明

这个项目当前更偏向“桌面辅助工具”而不是“全自动规则引擎”，所以有些能力是：

- 已记录并展示
- 已进入升级流程
- 但尚未在所有战斗细节中完全自动结算

例如某些种族武器熟练、部分高阶职业特性、部分法术/状态的完整自动化仍有继续细化空间。

## 已完成的近期增强

- 全局界面主题美化
- 战斗系统界面重做与交互优化
- 死亡角色禁止参战
- 敌人支持双击移除
- 扩充武器与法术卷轴
- 战斗大师战技池扩充
- 邪术士契约恩赐与更完整祈请池
- 核心种族子种族 / 血统 / 特性展示接入

## 后续可继续补充的方向

- 更多职业与子职业
- 更多怪物、法术、专长、装备
- 更完整的规则自动化结算
- 导出角色卡 / 遭遇日志
- 更细致的 UI 打磨与桌面打包流程
