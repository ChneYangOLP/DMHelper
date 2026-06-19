# DMHelper

`DMHelper` 是一个基于 Java 17 + JavaFX 22 的本地桌面 D&D 5e 辅助工具，定位是单机版 DM 控制台与角色管理器。
项目当前已经覆盖角色创建、升级选择、法术管理、装备与背包、战斗结算（含全面规则自动化）、怪物图鉴、自定义物品、PDF 导出和 SQLite 持久化。

## 项目定位

- 面向本地使用，不依赖服务端
- 以 2014 版 D&D 5e 核心规则为主要实现方向
- 目标是"可管理、可记录、可游玩、可导出"

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
  - 吟游诗人 `Bard`
- 已实现的主要成长内容：
  - 子职业选择
  - 属性值提升 / 专长
  - 技能熟练选择
  - 法术与戏法学习
  - 准备法术管理
  - 战斗大师战技选择
  - 术士超魔
  - 邪术士邪术祈请与契约恩赐
  - 吟游诗人吟游激励与魔法秘辛
- 当前已覆盖的子职业示例：
  - 战士：冠军勇士、战斗大师
  - 法师：8 大学派
  - 术士：龙脉术士、狂野魔法
  - 邪术士：邪魔恩主、妖精恩主、旧日支配者
  - 圣武士：奉献誓言、远古誓言、复仇誓言
  - 吟游诗人：学识学院、勇气学院

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
- 吟游诗人：
  - 吟游激励
  - 休憩之歌
  - 魔法秘辛

### 战斗系统

- 参战角色选择
- 怪物图鉴搜索与遭遇配置
- 先攻排序
- 目标与攻击方式选择
- 攻击结算与日志记录
- 敌人双击移除
- 死亡角色不可加入战斗
- 战斗中支持使用部分物品
- 战后结算（经验分配 + 掉落物分配）

### 规则自动化

#### 战斗规则

- **优势/劣势系统**：自动从状态效果判定优势/劣势，影响攻击掷骰
  - `INSPIRED`（激励）→ 攻击优势
  - `FRIGHTENED`（恐慌）→ 对恐惧来源攻击劣势
  - `POISONED`（中毒）/ `PRONE`（倒地）/ `RESTRAINED`（束缚）→ 攻击劣势
  - 优势与劣势互相抵消
- **掩护系统**：半掩护（AC+2）、四分之三掩护（AC+5）、全掩护
- **借机攻击**：战斗者移出敌方触及范围时自动触发借机攻击
  - 麻痹/沉睡状态的战斗者不能触发
  - 隐形战斗者不会触发
- **施法专注管理**：施放法术时自动开始专注追踪
  - 受击时自动进行专注检定（DC = max(10, 伤害/2)）
  - 新法术自动打破旧专注
  - 回合结束时统一检查所有活跃专注

#### 非战斗规则

- **技能检定自动化**：支持技能检定、属性检定、豁免检定，自动计算加值
  - 支持优势/劣势和自然 20/自然 1 判定
- **负重系统**：计算装备/背包总重量，4 级负重等级
  - 携带能力 = 力量 × 15
  - 轻度负重（>5×力量）、重度负重（>10×力量）、超重（>15×力量）
  - 重度/超重影响移动速度

### 战斗日志系统

- 完整事件记录（30 种日志类型）
- 自动记录：攻击掷骰、伤害、治疗、豁免检定、状态附加/消失、专注检定、借机攻击、死亡等
- 回合计数器
- 伤害/治疗统计（按阵营分别累计）
- 支持短格式、详细格式、PDF 导出格式

### PDF 导出

- **角色卡 PDF**：D&D 5e 风格角色卡
  - 深色主题头部横幅（角色名/种族/职业/等级/XP/金币）
  - 六维属性 3×2 网格（带调整值）
  - 战斗数据（HP/AC/先攻/速度/熟练加值/生命骰）
  - 豁免检定（熟练标记）
  - 技能列表 3 列布局（单圆=熟练/双圆=专精）
  - 装备信息 5 槽位
  - 施法信息（自动检测施法职业）
  - 背包物品列表
  - 种族特性/职业特性
  - 背景故事（性格/理想/羁绊/缺陷）
  - 负重可视化
- **遭遇日志 PDF**：完整战斗日志报告
  - 标题页（参战者列表 + 战斗统计摘要）
  - 时间线表格（颜色编码：攻击=红、治疗=绿、状态=蓝、系统=灰）
  - 参战者最终状态摘要
  - 经验值与战利品记录

#### 导出入口

| 界面 | 触发方式 |
|------|----------|
| 角色一览 (JavaFX) | 选中角色 → 点击"导出角色卡 PDF" |
| 角色管理 (JavaFX) | 选中角色 → 点击"导出角色卡 PDF" |
| 角色管理 (Swing) | 成长与升级标签页 → 点击"导出角色卡 PDF" |
| 战斗系统 (Swing) | 战斗胜利结算完成后 → 弹出确认框"是否导出战斗日志 PDF？" |

### 数据持久化

- 角色数据保存在 SQLite
- 自定义装备单独持久化
- 启动时自动初始化表结构
- 老版本项目根目录数据库会尝试迁移到正式存档目录

## 界面入口

应用主界面（FxLauncher）包含 4 个核心入口，**全部已迁移为 JavaFX 原生实现**，不再依赖 Swing：

- `创建角色` → `CharacterCreateWindow`（JavaFX）
- `角色一览` → `CharacterRosterWindow`（JavaFX，含导出角色卡 PDF）
- `角色管理` → `CharacterManagerWindow`（JavaFX，全功能管理 + 导出角色卡 PDF）
- `战斗系统` → `CombatConsoleWindow`（JavaFX，完整战斗 + 导出战斗日志 PDF）

Swing 版界面（`Character_Manager_UI`、`Combat_System_UI`、`Create_Character_UI` 等）仍保留在代码库中作为参考，但主菜单不再直接调用。

启动入口文件：

- [Main.java](src/main/java/Main.java)
- [FxLauncher.java](src/main/java/FxLauncher.java)

### 使用 Maven 运行 JavaFX 版本

1. 安装 JDK 17+ 与 Maven 3.9+。
2. 在仓库根目录执行一次编译（会自动下载 sqlite-jdbc、OpenJFX 与 PDFBox）：

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
│   │       ├── fx/                      # JavaFX 窗口层
│   │       │   ├── CharacterCreateWindow.java
│   │       │   ├── CharacterRosterWindow.java
│   │       │   ├── CharacterManagerWindow.java
│   │       │   ├── CombatConsoleWindow.java
│   │       │   └── FxThemes.java
│   │       └── basic/
│   │           ├── Character_Sheet.java  # 角色核心模型
│   │           ├── Stats.java            # 六维属性
│   │           ├── Skill_Check.java      # 技能检定自动化
│   │           ├── Encumbrance_System.java # 负重系统
│   │           ├── PDF_Util.java         # PDF 生成工具类
│   │           ├── Character_Card_PDF.java # 角色卡 PDF 导出
│   │           ├── Encounter_Log_PDF.java  # 遭遇日志 PDF 导出
│   │           ├── armor/               # 护甲系统
│   │           ├── combat/              # 战斗系统
│   │           │   ├── Combat_Engine.java          # 战斗结算引擎
│   │           │   ├── Combatant.java             # 战斗者模型
│   │           │   ├── Combat_Attack_Helper.java  # 攻击选项构建
│   │           │   ├── Attack_Option.java         # 攻击选项模型
│   │           │   ├── Dice_Util.java             # 骰子工具
│   │           │   ├── Combat_Status_Type.java     # 状态效果类型（13种）
│   │           │   ├── Combat_Status_Effect.java   # 状态效果实例
│   │           │   ├── Combat_Log_Entry.java       # 战斗日志条目
│   │           │   ├── Advantage_Disadvantage.java # 优势/劣势系统
│   │           │   ├── Cover_Type.java             # 掩护类型
│   │           │   ├── Concentration_Manager.java  # 施法专注管理
│   │           │   ├── Opportunity_Attack.java     # 借机攻击系统
│   │           │   ├── Monster_Definition.java     # 怪物定义
│   │           │   ├── Monster_Library.java        # 怪物图鉴（70+种）
│   │           │   └── Monster_Loot_Helper.java    # 怪物掉落系统
│   │           ├── database/            # 数据持久化层
│   │           ├── equipment/           # 装备系统
│   │           ├── feat/                # 专长系统
│   │           ├── menus/               # Swing UI（遗留）
│   │           ├── playerclass/         # 职业系统
│   │           ├── race/                # 种族系统
│   │           └── spell/               # 法术系统
│   └── resources/
│       └── META-INF/                    # MANIFEST 等资源
└── test/                                # 预留（当前为空）
```

## 运行环境

- JDK 17 或以上（推荐与系统保持一致，例如 macOS 上的 Temurin/OpenJDK）。
- Maven 3.9 及以上版本。
- OpenJFX、sqlite-jdbc、Apache PDFBox 由 Maven 自动拉取，无需手工放置 JAR。

## 依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| OpenJFX | 22.0.2 | JavaFX UI 框架 |
| sqlite-jdbc | 3.51.3.0 | SQLite 数据库驱动 |
| Apache PDFBox | 3.0.1 | PDF 生成与导出 |

## 本地运行

### 方式一：在 IDE 中运行

1. 以 "Open Existing Maven Project" 方式导入根目录（IDE 会自动识别 `pom.xml` 并下载依赖）。
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
- 依赖前提是本机已经安装 JDK 17+，并且本地 Maven 仓库里已有 OpenJFX、`sqlite-jdbc` 与 PDFBox 依赖。
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

- [DB_Helper.java](src/main/java/com/DMHelper/basic/database/DB_Helper.java)
- [Init_DB.java](src/main/java/com/DMHelper/basic/database/Init_DB.java)

## 主要源码入口

- 应用启动：
  - [Main.java](src/main/java/Main.java)
- JavaFX 窗口：
  - [CharacterCreateWindow.java](src/main/java/com/DMHelper/fx/CharacterCreateWindow.java)
  - [CharacterRosterWindow.java](src/main/java/com/DMHelper/fx/CharacterRosterWindow.java)
  - [CharacterManagerWindow.java](src/main/java/com/DMHelper/fx/CharacterManagerWindow.java)
  - [CombatConsoleWindow.java](src/main/java/com/DMHelper/fx/CombatConsoleWindow.java)
- Swing UI（遗留）：
  - [Main_Menu.java](src/main/java/com/DMHelper/basic/menus/Main_Menu.java)
  - [Create_Character_UI.java](src/main/java/com/DMHelper/basic/menus/Create_Character_UI.java)
  - [Character_Manager_UI.java](src/main/java/com/DMHelper/basic/menus/Character_Manager_UI.java)
  - [Combat_System_UI.java](src/main/java/com/DMHelper/basic/menus/Combat_System_UI.java)
- 规则系统：
  - [Combat_Engine.java](src/main/java/com/DMHelper/basic/combat/Combat_Engine.java)
  - [Advantage_Disadvantage.java](src/main/java/com/DMHelper/basic/combat/Advantage_Disadvantage.java)
  - [Concentration_Manager.java](src/main/java/com/DMHelper/basic/combat/Concentration_Manager.java)
  - [Opportunity_Attack.java](src/main/java/com/DMHelper/basic/combat/Opportunity_Attack.java)
  - [Skill_Check.java](src/main/java/com/DMHelper/basic/Skill_Check.java)
  - [Encumbrance_System.java](src/main/java/com/DMHelper/basic/Encumbrance_System.java)
- PDF 导出：
  - [PDF_Util.java](src/main/java/com/DMHelper/basic/PDF_Util.java)
  - [Character_Card_PDF.java](src/main/java/com/DMHelper/basic/Character_Card_PDF.java)
  - [Encounter_Log_PDF.java](src/main/java/com/DMHelper/basic/Encounter_Log_PDF.java)

## 规则实现说明

这个项目已从"桌面辅助工具"升级为"带全面规则自动化的桌面辅助工具"：

- ✅ 优势/劣势自动判定与掷骰
- ✅ 掩护系统（半掩护/四分之三掩护/全掩护）
- ✅ 借机攻击自动触发
- ✅ 施法专注追踪与检定
- ✅ 技能/属性/豁免检定自动化
- ✅ 负重系统与速度惩罚
- ✅ 完整战斗日志记录
- ✅ 角色卡与遭遇日志 PDF 导出

仍有继续细化空间的部分：

- 某些种族武器熟练的自动应用
- 部分高阶职业特性的完全自动化
- 更多状态效果的交互（如恐慌的目标范围判定）
- 夹击/侧袭等战术位置规则

## 更新日志

### 2026-04-27 — 全面 JavaFX 迁移

- **重写**：CharacterCreateWindow — 完整两步创建流程（基础信息 → 背景性格），与 Swing 版功能一致
- **重写**：CharacterManagerWindow — 全功能角色管理（1634行），四个标签页（基础与属性/装备与物品/施法与法术/成长与升级），完整道具使用系统
- **重写**：CombatConsoleWindow — 完整战斗系统（2157行），配置面板+战斗面板，完整道具使用系统，战后结算+PDF导出
- **重写**：FxLauncher — 所有入口改为指向 JavaFX 窗口，移除 Swing 依赖
- **新增**：Swing 版 Character_Manager_UI 基础与属性标签页添加"导出角色卡 PDF"按钮
- **变更**：Swing 版界面仅作为代码参考保留，主菜单不再直接调用

### 2026-04-25 — 规则自动化与 PDF 导出

- **新增**：优势/劣势系统（自动从状态效果判定）
- **新增**：掩护系统（半掩护 AC+2、四分之三掩护 AC+5）
- **新增**：借机攻击系统（移出触及范围自动触发）
- **新增**：施法专注管理器（专注追踪、受击检定、新法术打破旧专注）
- **新增**：技能检定自动化（技能/属性/豁免检定，支持优势/劣势）
- **新增**：负重系统（4 级负重等级，速度惩罚）
- **新增**：战斗日志系统（30 种日志类型，完整事件记录）
- **新增**：角色卡 PDF 导出（D&D 5e 风格，含六维属性/战斗数据/技能/装备/法术/背景）
- **新增**：遭遇日志 PDF 导出（标题页 + 时间线表格 + 统计摘要 + 参战者状态）
- **新增**：角色一览界面导出角色卡 PDF 按钮
- **新增**：角色管理界面导出角色卡 PDF 按钮
- **新增**：战斗系统战斗结束后弹出导出战斗日志 PDF 确认框
- **新增**：Apache PDFBox 3.0.1 依赖
- **增强**：Combat_Engine 集成优势/劣势、专注管理、战斗日志、伤害统计
- **修复**：javafx-maven-plugin 版本号修正（22.0.2 → 0.0.8）
- **修复**：javafx-maven-plugin 配置语法修正（jvmArg → option）

## 后续可继续补充的方向

- 更多职业与子职业（游荡者、牧师、德鲁伊、武僧、野蛮人等）
- 更多怪物、法术、专长、装备
- 更完整的状态效果交互（恐慌目标范围、夹击/侧袭）
- 导出角色卡支持更多自定义选项
- 更细致的 UI 打磨与桌面打包流程
