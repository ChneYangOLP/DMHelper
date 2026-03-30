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

- [Main.java](/Users/hatys/IdeaProjects/DMHelper/src/Main.java)

## 项目结构

```text
src/
├── Main.java
├── META-INF/
└── com/DMHelper/
    ├── assets/                      # 图标与桌面资源
    └── basic/
        ├── Character_Sheet.java     # 角色核心模型
        ├── Stats.java               # 六维与调整值
        ├── armor/                   # 护甲兼容模型
        ├── combat/                  # 战斗引擎、怪物、掉落、状态
        ├── database/                # SQLite 连接、DAO、初始化、序列化
        ├── equipment/               # 装备、物品库、槽位
        ├── feat/                    # 专长定义与专长库
        ├── menus/                   # Swing 界面与交互辅助
        ├── playerclass/             # 职业、子职业、成长与资源
        ├── race/                    # 种族、子种族与恢复工厂
        └── spell/                   # 法术定义与法术库
```

## 运行环境

- Java Swing 桌面环境
- SQLite JDBC 驱动：
  - `lib/sqlite-jdbc-3.51.3.0.jar`
- 当前项目可使用以下方式直接编译运行

如果你在 IDE 中运行，确认 `sqlite-jdbc-3.51.3.0.jar` 已加入 classpath。

## 本地运行

### 方式一：在 IDE 中运行

直接运行：

- [Main.java](/Users/hatys/IdeaProjects/DMHelper/src/Main.java)

### 方式二：命令行编译

在项目根目录执行：

```bash
javac -cp lib/sqlite-jdbc-3.51.3.0.jar -d out $(find src -name '*.java' | sort)
```

快速构建校验：

```bash
javac -cp lib/sqlite-jdbc-3.51.3.0.jar -d /tmp/dmhelper-build $(find src -name '*.java' | sort)
```

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

- [DB_Helper.java](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/basic/database/DB_Helper.java)
- [Init_DB.java](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/basic/database/Init_DB.java)

## 主要源码入口

- 应用启动：
  - [Main.java](/Users/hatys/IdeaProjects/DMHelper/src/Main.java)
- 主菜单：
  - [Main_Menu.java](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/basic/menus/Main_Menu.java)
- 角色创建：
  - [Create_Character_UI.java](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/basic/menus/Create_Character_UI.java)
- 角色管理：
  - [Character_Manager_UI.java](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/basic/menus/Character_Manager_UI.java)
- 战斗系统：
  - [Combat_System_UI.java](/Users/hatys/IdeaProjects/DMHelper/src/com/DMHelper/basic/menus/Combat_System_UI.java)

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
