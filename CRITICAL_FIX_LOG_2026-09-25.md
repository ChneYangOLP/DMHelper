# DMHelper 严重问题修复日志

- 修复日期：2026-09-25
- 修复范围：本地 API 访问安全、物品出售结算
- 状态：已完成并通过构建检查

## 1. 本地 API 可被任意网页访问

### 问题描述

后端原先监听固定的 `8080` 端口，并返回以下宽松的 CORS 响应头：

```text
Access-Control-Allow-Origin: *
Access-Control-Allow-Headers: *
```

角色、物品、金币、经验值和法术等写操作也没有客户端身份校验。用户运行 DMHelper 时，其他网页可能向本机 API 发起请求并修改本地角色数据。

### 风险

- 未授权修改角色资料、经验值、装备和库存。
- 未授权触发休息、升级及法术管理操作。
- 后端若监听非回环网卡，还可能被同一局域网中的其他设备访问。

### 根因

- Javalin 使用默认监听地址，没有显式限制为回环地址。
- CORS 允许所有来源和请求头。
- Electron 前端与 Java 后端之间没有用于识别合法客户端的凭据。

### 解决办法

1. 将 Javalin 的监听地址限制为 `127.0.0.1`，禁止局域网直接访问。
2. 移除通配符 CORS，只允许 Electron 文件页面和本地 Vite 开发地址。
3. Electron 每次启动时生成随机 API 令牌。
4. 启动 Java 后端时通过 `DMHELPER_API_TOKEN` 环境变量传递令牌。
5. 通过 preload 安全桥接将令牌提供给渲染进程。
6. 所有前端 API 请求携带 `X-DMHelper-Token` 请求头。
7. 后端在进入业务处理器前校验来源和令牌；非法来源返回 `403`，令牌错误返回 `401`。

### 涉及文件

- `src/main/java/com/DMHelper/api/ApiServer.java`
- `src/main/java/Main.java`
- `frontend/electron/main.ts`
- `frontend/electron/preload.ts`
- `frontend/src/api/index.ts`
- `frontend/env.d.ts`

### 修复结果

- API 仅监听 `http://127.0.0.1:8080/`。
- 普通网页无法再直接调用业务接口修改本地数据。
- Electron 正常启动时会自动完成令牌生成、传递和请求认证，不需要用户手动配置。

## 2. 物品出售数量可伪造并错误结算金币

### 问题描述

出售接口原先直接使用客户端提交的数量计算金币：

```text
出售收益 = 单件售价 × 请求数量
```

随后才循环移除库存，并且在库存不足时只退出循环，不会修正已经计算的收益。例如角色只有 1 件物品，但请求出售 100 件时，系统仍可能按照 100 件发放金币。

接口也没有拒绝负数、零、非整数或可能造成整数溢出的数量。

### 风险

- 可以通过超额出售无限增加金币。
- 异常数量可能造成错误日志、错误结算或整数溢出。
- 前端使用 `parseInt`，类似 `1abc` 的输入也可能被当作合法数量提交。

### 根因

- 服务端信任客户端提交的数量。
- 收益计算发生在确认实际库存之前。
- 前后端均缺少严格的正整数校验。

### 解决办法

1. 服务端要求出售数量必须是大于零的整数。
2. 结算前调用角色库存计数，确认持有数量不少于请求数量。
3. 库存不足时返回 `400 Not enough items to sell`，不移除物品，也不发放金币。
4. 使用 `Math.multiplyExact` 计算总收益和购买成本，拒绝整数溢出。
5. 对 action、itemKey 和物品是否存在进行统一校验。
6. 前端使用 `Number` 和 `Number.isInteger` 校验输入，并限制出售数量不能超过当前持有数量。
7. 前端在接口返回失败时不再刷新并伪装成成功操作。

### 关联加固

同一库存接口还增加了使用物品前的所有权检查，避免提交未持有的治疗物品 key 进行无限治疗。

### 涉及文件

- `src/main/java/com/DMHelper/api/CharacterController.java`
- `frontend/src/components/manager/EquipmentTab.vue`

### 修复结果

- 超额出售请求会被拒绝，不会改变库存或金币。
- 零、负数、小数、非数字和溢出数量均无法进入结算。
- 出售收益只会在库存校验通过后发放。
- 未持有的物品不能通过 API 被使用。

## 验证记录

- Java 相关修改已通过针对性编译检查。
- `vue-tsc --noEmit` 通过。
- Vite 前端生产构建通过。
- Electron main/preload 构建通过。
- Electron Windows 运行时版本：`v35.7.5`。
- 构建产物已更新至 `frontend/dist` 和 `frontend/dist-electron`。

