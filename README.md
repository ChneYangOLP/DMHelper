# DMHelper

`DMHelper` 目前是一个基于 Java Swing 的桌面 D&D 5e 辅助工具，后面可能会对界面做美化。面向本地单机使用场景。
当前项目已经包含角色创建、升级、施法资源、装备/背包、战斗结算、怪物图鉴、掉落、自定义物品与数据库持久化等功能。

## 功能概览

- 创建角色：支持核心种族与多个职业，带属性、背景故事、性格、理想、羁绊、缺陷等自定义内容。
- 角色管理：支持经验值、升级、长休、装备槽位、背包、职业特性查看。
- 物品系统：支持从内置物品库搜索添加、自定义普通物品、自定义武器/护甲模板、背包堆叠数量与分类筛选。
- 物品交互：背包支持双击使用、右键菜单、丢弃一件/全部、药水治疗、卷轴施法、炸弹投掷、钥匙/任务道具记录。
- 施法系统：支持法师、术士、邪术士、圣武士等职业的法术/戏法管理与资源显示。
- 战斗系统：支持角色参战、怪物检索、先攻排序、攻击结算、状态效果、经验分配与掉落分配；背包卷轴和炸弹可直接联动当前战斗目标。
- 怪物图鉴：内置一批常见与扩展怪物，可直接用于遭遇构建与战斗测试。
- 数据持久化：角色数据通过 SQLite 保存，关闭应用后仍可继续读取。
- 桌面打包：可通过 `jpackage` 生成 macOS `.app` / `.dmg`，也预留了 Windows 图标资源。

## 项目结构

```text
src/
├── Main.java                         # 桌面应用启动入口
├── com/DMHelper/assets/            # 图标等资源
└── com/DMHelper/basic/
    ├── Character_Sheet.java         # 角色核心模型
    ├── Stats.java                   # 六维属性模型
    ├── armor/                       # 旧护甲结构兼容
    ├── combat/                      # 战斗引擎、攻击选项、怪物、掉落、状态
    ├── database/                    # SQLite 初始化、DAO、序列化与连接
    ├── equipment/                   # 新装备/背包/槽位系统
    ├── feat/                        # 专长库
    ├── menus/                       # Swing 界面与升级/法术管理辅助
    ├── playerclass/                 # 各职业与升级资源
    ├── race/                        # 种族定义
    └── spell/                       # 法术定义与法术库
```

## 运行环境

- JDK 25
- macOS / Windows / Linux
- SQLite JDBC 驱动：
  [sqlite-jdbc-3.51.3.0.jar](/Users/hatys/IdeaProjects/DMHelper/lib/sqlite-jdbc-3.51.3.0.jar)

## 在 IDE 中运行

入口类：

- [Main.java](/Users/hatys/IdeaProjects/DMHelper/src/Main.java)

运行前确认：

- 项目 `classpath` 已包含 `sqlite-jdbc-3.51.3.0.jar`
- 资源文件已包含应用图标：
  [app_icon.png](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/assets/app_icon.png)

## 命令行编译

在项目根目录执行：

```bash
javac -cp lib/sqlite-jdbc-3.51.3.0.jar -d out $(find src -name '*.java' | sort)
```

如果只想做一次快速构建校验，也可以输出到临时目录：

```bash
javac -cp lib/sqlite-jdbc-3.51.3.0.jar -d /tmp/dmhelper-build $(find src -name '*.java' | sort)
```

## 数据库存档位置

应用当前会把 SQLite 数据库存到用户目录下的应用数据路径，而不是依赖启动目录。

- macOS:
  `~/Library/Application Support/DMDHelper/dnd_data.db`
- Windows:
  `%APPDATA%/DMDHelper/dnd_data.db`
- Linux:
  `~/.local/share/DMDHelper/dnd_data.db`

兼容说明：

- 如果老版本项目根目录下已经存在 `dnd_data.db`，应用首次启动时会自动尝试迁移到新的正式存档目录。

## 打包说明

### macOS `.app`

```bash
jpackage \
  --type app-image \
  --name DMHelper \
  --input build/input \
  --main-jar DMHelper.jar \
  --main-class Main \
  --icon src/com/DMHelper/assets/app_icon.icns \
  --dest dist
```

### macOS `.dmg`

```bash
jpackage \
  --type dmg \
  --name DMHelper \
  --input build/input \
  --main-jar DMHelper.jar \
  --main-class Main \
  --icon src/com/DMHelper/assets/app_icon.icns \
  --dest dist
```

### Windows 图标资源

Windows 打包时建议使用：

- [app_icon.ico](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/assets/app_icon.ico)

macOS 打包时建议使用：

- [app_icon.icns](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/assets/app_icon.icns)

## 当前实现特点

- 规则目标以 D&D 5e 桌面辅助为主，不是完整规则引擎。
- 自定义装备支持模板化创建：
  - 可先选轻甲 / 中甲 / 重甲 / 单手武器 / 双手武器 / 灵巧武器 / 远程武器 / 盾牌模板
  - 再填写名称、描述、AC 或伤害等内容并持久化到 SQLite
- 背包系统已经具备可玩性：
  - 同类背包物品可堆叠并持久化数量
  - 支持 `全部 / 消耗品 / 材料/战利品 / 工具/任务 / 自定义` 分类筛选
  - 治疗药水会自动回血并扣减数量
  - `疗伤术卷轴`、`火球术卷轴`、`鉴定术卷轴` 会走专属使用流程
  - `火焰炸弹`、`震雷炸弹` 可在战斗中选择真实目标并写入战斗日志
- 界面层与规则层已经基本拆开：
  - `menus/` 负责交互
  - `combat/` 负责战斗
  - `playerclass/` 负责职业成长
  - `database/` 负责持久化
- 部分复杂规则目前采用“可记录、可展示、可管理”的方式实现，而不是完全自动化判定。
