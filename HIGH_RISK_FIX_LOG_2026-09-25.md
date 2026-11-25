# DMHelper 高风险问题修复日志

- 修复日期：2026-09-25
- 修复范围：API 错误传播、角色生命值持久化、长休规则一致性
- 修复数量：3
- 状态：已完成并通过编译与构建检查

## 1. 前端吞掉 API 错误并显示假成功

### 问题描述

前端 API 封装在请求失败、网络中断或收到非 2xx HTTP 状态时，会捕获异常并返回 `null`。上层页面使用 `try/catch` 判断操作是否失败，但由于异常已经被底层吞掉，`catch` 不会执行。

这会导致创建、保存、购买、升级、休息等操作实际失败后，页面仍继续跳转、刷新或提示成功。

### 风险

- 用户误以为数据已经保存，关闭应用后才发现数据丢失。
- 金币不足、经验不足、认证失败等业务错误无法正确展示。
- 网络故障和后端异常被隐藏，增加排查难度。

### 根因

`frontend/src/api/index.ts` 中的 `get`、`post` 和 `put` 方法统一捕获异常，然后返回 `null`，破坏了调用方基于异常的控制流程。

### 解决办法

1. 新增 `ApiError`，保存 HTTP 状态码和后端返回的错误消息。
2. 新增统一的 `parseResponse` 响应处理函数。
3. 非 2xx 响应读取响应正文并抛出 `ApiError`，不再返回 `null`。
4. 网络错误和 JSON 解析错误继续向上传播，由具体页面负责提示。
5. 为角色列表增加加载失败状态和重试按钮。
6. 为物品使用、出售、购买、商店搜索和法术加载补充明确的异常处理。
7. 操作失败时不再刷新数据或显示成功提示。

### 涉及文件

- `frontend/src/api/index.ts`
- `frontend/src/components/CharacterRoster.vue`
- `frontend/src/components/manager/EquipmentTab.vue`
- `frontend/src/components/manager/SpellcastingTab.vue`

### 修复结果

- HTTP 失败会进入页面的 `catch` 分支。
- 页面能够展示后端返回的具体错误原因。
- 创建、保存、购买、升级和休息等既有 `try/catch` 现在可以正常工作。
- 不再出现请求失败但界面提示成功的情况。

## 2. 死亡角色重启后自动恢复满血

### 问题描述

角色读档时使用了以下判断逻辑：

```text
current_hp <= 0 ? max_hp : current_hp
```

由于 `0 HP` 是角色死亡或倒地的合法状态，读档代码却将它视为旧存档缺省值，因此角色重启应用后会恢复为满血。

### 风险

- 战斗结果无法正确持久化。
- 死亡或倒地状态在重启后丢失。
- 角色生命值与数据库记录不一致。

### 根因

代码使用数值 `0` 同时表达了两种含义：

- 合法的死亡状态；
- 旧数据库缺少 `current_hp` 时的默认状态。

读档时无法区分这两种状态。

### 解决办法

1. 数据库中新建或迁移的 `current_hp` 字段默认值改为 `NULL`。
2. 读档时通过 `ResultSet.getObject` 保留数据库的空值信息。
3. 只有 `current_hp IS NULL` 才按照旧存档处理，并恢复为角色最大生命值。
4. `current_hp = 0` 被视为合法数据，原样传给 `set_current_hp`。
5. `set_current_hp` 继续负责将异常值限制在 `0..max_hp` 范围内。

### 兼容策略

- 新创建和正常保存的角色始终显式写入当前生命值。
- 真正缺少当前生命值的旧存档使用 `NULL`，首次加载时恢复满血。
- 已明确保存为 `0 HP` 的角色保持死亡或倒地状态。

### 涉及文件

- `src/main/java/com/DMHelper/basic/database/Character_DAO.java`
- `src/main/java/com/DMHelper/basic/database/Init_DB.java`

### 修复结果

- `0 HP` 角色在重新启动应用后仍保持 `0 HP`。
- 旧存档缺失生命值时仍能安全加载。
- 生命值继续受到最大值和最小值约束。

## 3. API 长休遗漏生命骰和职业资源同步

### 问题描述

API 控制器原先自行实现长休，只执行以下操作：

- 当前生命值恢复至最大值；
- 调用职业的基础长休资源恢复方法；
- 写入一条日志。

项目领域模型中已经存在完整的 `Character_Sheet.take_long_rest()`，但 API 没有调用它。因此，通过 Electron 前端进行长休时会遗漏：

- 长休生命骰恢复；
- 派生属性重新计算；
- 吟游诗人魅力资源上限同步；
- 圣武士魅力资源上限同步；
- 领域模型定义的统一长休日志。

### 风险

- Electron 前端与 JavaFX 界面的长休结果不一致。
- 玩家生命骰无法按规则恢复。
- 部分职业资源在属性或等级变化后保持错误上限。
- 后续新增长休规则时容易再次遗漏 API 入口。

### 根因

控制器重复实现了领域逻辑，形成两套不同的长休流程。

### 解决办法

将 API 控制器中的分散长休代码替换为：

```java
character.take_long_rest();
```

所有长休规则统一由 `Character_Sheet` 维护，控制器只负责解析请求、持久化结果并返回响应。

### 涉及文件

- `src/main/java/com/DMHelper/api/CharacterController.java`
- 复用：`src/main/java/com/DMHelper/basic/Character_Sheet.java`

### 修复结果

- 长休恢复满生命值。
- 按规则恢复部分生命骰。
- 恢复法术位和职业资源。
- 正确同步吟游诗人与圣武士的魅力资源上限。
- Electron 与其他界面入口共享同一套长休规则。

## 验证记录

- Java 相关修改已通过 `javac --release 17` 针对性编译检查。
- `vue-tsc --noEmit` 通过。
- Vite 前端生产构建通过。
- Electron main/preload 构建通过。
- 构建过程未出现 TypeScript 错误或 Vite/Rollup 配置警告。
- 新的前端构建产物已写入 `frontend/dist` 和 `frontend/dist-electron`。

