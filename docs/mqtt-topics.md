# LED Cloud MQTT Topic 协议文档

> 版本: 1.1  
> 更新时间: 2026-07-21  
> Topic 前缀 `${mqtt.topic-prefix}`，默认值 `led`

---

## 一、Topic 层级结构

```
{prefix}/
├── register/{SN}              # 上行: 设备注册
├── heartbeat/{SN}             # 上行: 设备心跳
├── status/{SN}                # 上行: 设备状态变更
├── ping/{SN}                  # 下行: 心跳探测
└── command/{SN}/
    ├── play                   # 下行: 播放节目
    └── command                # 下行: 通用控制命令
```

---

## 二、上行 Topic（设备 → 服务端）

服务端通过 `MqttConfig.mqttInbound()` 通配订阅：
- `{prefix}/register/#`
- `{prefix}/heartbeat/#`
- `{prefix}/status/#`

### 2.1 设备注册

| 项目 | 值 |
|------|---|
| Topic | `{prefix}/register/{SN}` |
| 示例 | `led/register/20000001` |
| QoS | 1 |
| 方向 | 设备 → 服务端 |
| 触发时机 | 设备开机、重启、重新连接 MQTT |

**Payload 示例：**
```json
{
  "ip": "192.168.1.100",
  "model": "P2.5-32x64",
  "version": "v2.1.0",
  "width": 1920,
  "height": 1080,
  "brightness": 80
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ip` | string | 否 | 设备 IP 地址 |
| `model` | string | 否 | 屏幕型号 |
| `version` | string | 否 | 固件版本号 |
| `width` | int | 否 | 分辨率宽度 |
| `height` | int | 否 | 分辨率高度 |
| `brightness` | int | 否 | 当前亮度 (0-100) |

**服务端处理逻辑：**
- 根据 SN 查找 `screen` 表
- 已存在 → 更新设备信息 + 标记 `status = online` + 刷新 `last_heartbeat`
- 不存在 → 新建屏幕记录，以 SN 作为设备名称和 MQTT 客户端 ID

---

### 2.2 设备心跳

| 项目 | 值 |
|------|---|
| Topic | `{prefix}/heartbeat/{SN}` |
| 示例 | `led/heartbeat/20000001` |
| QoS | 1 |
| 方向 | 设备 → 服务端 |
| 触发时机 | 收到服务端 ping 后回复；或设备定时上报 |

**Payload 示例：**
```json
{
  "type": "heartbeat",
  "status": "online",
  "brightness": 75,
  "temperature": 42
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | 否 | 消息类型标识 |
| `status` | string | 否 | 当前播放状态 |
| `brightness` | int | 否 | 当前亮度 |
| `temperature` | int | 否 | 设备温度（摄氏度） |

**服务端处理逻辑：**
- 更新 `screen.last_heartbeat` 为当前时间
- 标记 `screen.status = online`
- 通过 WebSocket 广播状态到管理端

**超时判定：**
- `ScreenOfflineChecker` 每 30 秒扫描一次
- `last_heartbeat` 超过 30 秒 → 标记设备离线

---

### 2.3 设备状态变更

| 项目 | 值 |
|------|---|
| Topic | `{prefix}/status/{SN}` |
| 示例 | `led/status/20000001` |
| QoS | 1 |
| 方向 | 设备 → 服务端 |
| 触发时机 | 设备亮度变化、播放状态变化等主动上报 |

**Payload 示例：**
```json
{
  "status": "playing",
  "brightness": 90,
  "programName": "广告节目A",
  "volume": 50
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | string | 否 | 播放状态: playing / paused / stopped / idle |
| `brightness` | int | 否 | 当前亮度 (0-100) |
| `programName` | string | 否 | 正在播放的节目名称 |
| `volume` | int | 否 | 当前音量 (0-100) |

**服务端处理逻辑：**
- 更新 `screen.brightness` 和 `screen.status`
- 通过 WebSocket 广播状态到管理端

---

## 三、下行 Topic（服务端 → 设备）

设备需要订阅以上行 topic 对应的命令 topic。

### 3.1 心跳探测

| 项目 | 值 |
|------|---|
| Topic | `{prefix}/ping/{SN}` |
| 示例 | `led/ping/20000001` |
| QoS | 1 |
| 方向 | 服务端 → 设备 |
| 触发时机 | 服务端每 30 秒向每个在线设备发送 |

**Payload：**
```json
{
  "type": "ping"
}
```

**设备响应要求：**
收到 ping 后，必须在 30 秒内回复到 `{prefix}/heartbeat/{SN}`。

---

### 3.2 播放节目

| 项目 | 值 |
|------|---|
| Topic | `{prefix}/command/{SN}/play` |
| 示例 | `led/command/20000001/play` |
| QoS | 1 |
| 方向 | 服务端 → 设备 |
| 触发时机 | 管理员在后台创建/修改播放计划时下发 |

**Payload 示例（单素材）：**
```json
{
  "type": "play",
  "programName": "促销广告",
  "items": [
    {
      "type": "image",
      "url": "https://cdn.example.com/media/promo.jpg",
      "duration": 10
    }
  ]
}
```

**Payload 示例（多素材轮播）：**
```json
{
  "type": "play",
  "programName": "轮播套餐",
  "items": [
    { "type": "image", "url": "https://cdn.example.com/01.jpg", "duration": 10 },
    { "type": "video", "url": "https://cdn.example.com/ad.mp4", "duration": 30 },
    { "type": "text",  "content": "欢迎光临", "duration": 5, "fontSize": 48, "color": "#FF0000" }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | 是 | 固定 `"play"` |
| `programName` | string | 是 | 节目名称 |
| `items` | array | 是 | 素材列表 |

**items 子字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | 是 | 素材类型: `image` / `video` / `text` |
| `url` | string | image/video 必填 | 素材 CDN 地址 |
| `content` | string | text 必填 | 文字内容 |
| `duration` | int | 是 | 播放时长（秒） |
| `fontSize` | int | 否 | 文字字号 |
| `color` | string | 否 | 文字颜色 |

---

### 3.3 通用控制命令

| 项目 | 值 |
|------|---|
| Topic | `{prefix}/command/{SN}/command` |
| 示例 | `led/command/20000001/command` |
| QoS | 1 |
| 方向 | 服务端 → 设备 |
| 触发时机 | 管理员在后台操作设备控制 |

#### 3.3.1 开机

```json
{ "type": "power_on" }
```

#### 3.3.2 关机

```json
{ "type": "power_off" }
```

#### 3.3.3 停止播放

```json
{ "type": "stop" }
```

#### 3.3.4 暂停播放

```json
{ "type": "pause" }
```

#### 3.3.5 恢复播放

```json
{ "type": "resume" }
```

#### 3.3.6 调节亮度

```json
{
  "type": "brightness",
  "value": 80
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `value` | int | 亮度值 (0-100) |

---

## 四、Topic 汇总速查表

| # | Topic | 方向 | QoS | Payload Type | 触发场景 |
|---|-------|------|-----|-------------|----------|
| 1 | `{prefix}/register/{SN}` | ↑ 上行 | 1 | 设备信息 JSON | 设备开机/重连 |
| 2 | `{prefix}/heartbeat/{SN}` | ↑ 上行 | 1 | 心跳 JSON | 收到 ping / 定时 |
| 3 | `{prefix}/status/{SN}` | ↑ 上行 | 1 | 状态 JSON | 设备状态变化 |
| 4 | `{prefix}/ping/{SN}` | ↓ 下行 | 1 | `{"type":"ping"}` | 服务端定时(30s) |
| 5 | `{prefix}/command/{SN}/play` | ↓ 下行 | 1 | `{"type":"play",...}` | 下发播放计划 |
| 6 | `{prefix}/command/{SN}/command` | ↓ 下行 | 1 | 控制命令 JSON | 后台手动操作 |

---

## 五、心跳机制详解

```
 服务端                           设备
   │                               │
   │ ─ {prefix}/ping/20000001 ─► │  每30秒发一次
   │                               │
   │ ◄── {prefix}/heartbeat/20000001 ── │  收到后回复
   │                               │
   │     (30秒未收到 → 标记离线)      │
```

**超时配置：**
- Keep-Alive: 30 秒
- 心跳超时: 30 秒
- `ScreenOfflineChecker` 扫描间隔: 30 秒

**代码位置：**
- `ScreenOfflineChecker.java`: 定时扫描在线设备，发送 ping
- `MqttCommandService.sendHeartbeatPing()`: 发送 ping
- `MqttInboundHandler.handleHeartbeat()`: 处理心跳回复

---

## 六、QoS 说明

所有主题统一使用 **QoS 1（至少一次）**：
- 消息不会丢失
- 可能收到重复消息（服务端/设备需做幂等处理）

---

## 七、{SN} 命名约定

设备 ID（SN / mqttClientId）统一使用 **2 开头的 8 位阿拉伯数字**。

| 字段 | 格式 | 示例 | 说明 |
|------|------|------|------|
| `mqtt_client_id` | 2 开头 8 位数字 | `20000001` | 存储在 `t_screen` 表，设备注册时上报 |
| 范围 | `20000000` ~ `29999999` | -- | 理论最大容量 1000 万台 |

### 7.1 下发命令

所有下行命令的 mqttClientId 必须从数据库 `t_screen.mqtt_client_id` 读取，**禁止**通过 `screen_id` 拼接。

代码示例（`PlaybackController` / `BatchController`）：
```java
Screen screen = screenMapper.selectById(screenId);
String mqttClientId = screen.getMqttClientId(); // 20000001
```

### 7.2 设备注册

设备固件写入唯一的 8 位设备 ID，开机时上报到 `{prefix}/register/{SN}`：
```json
// led/register/20000001
{ "ip": "192.168.1.100" }
```

服务端收到注册消息后，将 topic 中提取的 SN 存入 `t_screen.mqtt_client_id`。

### 7.3 历史数据迁移

如果数据库中仍存在旧版 `SCREEN-001` 格式的设备 ID，请执行迁移脚本：

```bash
mysql -u root -p led_management < sql/migrate-device-id.sql
```

脚本会将示例数据映射为：

| 原 `mqtt_client_id` | 新 `mqtt_client_id` |
|---------------------|---------------------|
| `SCREEN-001`        | `20000001`          |
| `SCREEN-002`        | `20000002`          |
| ...                 | ...                 |

执行前请备份数据库，并根据实际数据量调整脚本中的映射关系。

---

## 八、设备端 MQTT 实现要求

### 8.1 连接参数

| 参数 | 值 |
|------|---|
| Broker URL | 由服务端配置下发（Docker 环境: `tcp://localhost:1886`，直连: `tcp://localhost:1883`） |
| Client ID | 设备唯一标识，与服务端 `{SN}` 一致 |
| Keep Alive | 30 秒 |
| Clean Session | true |
| QoS | 1 |

### 8.2 订阅 Topic 列表

```
{prefix}/command/{SN}/play
{prefix}/command/{SN}/command
{prefix}/ping/{SN}
```

### 8.3 发布 Topic 列表

```
{prefix}/register/{SN}     # 开机时上报
{prefix}/heartbeat/{SN}    # 收到ping后回复 / 定时上报
{prefix}/status/{SN}       # 状态变化时上报
```

### 8.4 消息处理幂等性

由于 QoS 1 可能重复投递，设备端需要对以下命令做幂等处理：
- 收到 `play` 时，对比 `programName`，相同则忽略
- 收到 `brightness` 时，对比当前值，相同则忽略
- 收到 `ping` 时，每次都回复，无副作用

---

## 九、代码索引

| 组件 | 文件路径 |
|------|----------|
| MQTT 配置类 | `led-device/.../config/MqttConfig.java` |
| 入站消息处理 | `led-device/.../service/MqttInboundHandler.java` |
| 命令下发服务 | `led-device/.../service/MqttCommandService.java` |
| 离线检测器 | `led-device/.../service/ScreenOfflineChecker.java` |
| 设备端 WebSocket | `led-device/.../websocket/ScreenStatusWebSocket.java` |
| 配置文件 | `led-device/src/main/resources/application.yml` |
