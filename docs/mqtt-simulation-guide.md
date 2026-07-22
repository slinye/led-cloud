# LED Cloud MQTTX 设备模拟指南

> 使用 MQTTX 客户端模拟一台 LED 屏幕设备，完整验证 MQTT 通信协议
> 对应协议文档: [mqtt-topics.md](./mqtt-topics.md)

---

## 一、环境信息

| 参数 | 值 | 来源 |
|------|-----|------|
| Broker URL | `tcp://localhost:1886` | `docker-compose.yml` → `${MQTT_EXTERNAL_PORT:-1886}` |
| Topic 前缀 | `led` | `application.yml` → `mqtt.topic-prefix` |
| 用户名 | (空) | 匿名访问，`mosquitto.conf` → `allow_anonymous true` |
| 密码 | (空) | 同上 |
| 模拟设备 SN | `20000001` | 2 开头 8 位数字 |
| QoS | 1 | 全部 Topic 统一 |

> **端口说明**: Docker 将 Mosquitto 容器内 `1883` 映射到宿主机 `1886`（`.env.example` → `MQTT_EXTERNAL_PORT=1886`）。  
> 若本地直连 Mosquitto/EMQX（非 Docker），端口为 `1883`。

> **注意**: 如果 Broker 不在 localhost，请替换为实际 IP。若启用了认证，填写对应的用户名/密码。

---

## 二、连接 MQTT Broker

### 步骤 1：新建连接

打开 MQTTX → 点击左侧 **"+"** → 填写：

| 字段 | 值 |
|------|-----|
| Name | `LED-20000001` |
| Client ID | `20000001` |
| Host | `mqtt://localhost` |
| Port | `1886` |
| Username | (空) |
| Password | (空) |
| Clean Session | ✅ 开启 |
| Keep Alive | `30` |

点击右上角 **"Connect"**。

![示意图]

### 步骤 2：订阅下行 Topic

连接成功后，需要订阅服务端下发的命令。逐个添加订阅：

| # | Topic | QoS | 用途 |
|---|-------|-----|------|
| 1 | `led/ping/20000001` | 1 | 接收服务端心跳探测 |
| 2 | `led/command/20000001/play` | 1 | 接收播放节目指令 |
| 3 | `led/command/20000001/command` | 1 | 接收通用控制指令 |

操作：点击 **"+ New Subscription"** → 输入 Topic → 选择 QoS 1 → **Confirm**

订阅完成后左侧应显示 3 个已订阅 Topic。

---

## 三、模拟设备启动（注册）

### 步骤 3：发送注册消息

设备开机后首先向服务端注册。

**操作：** 点击底部发送区域 → 填写：

| 字段 | 值 |
|------|-----|
| Topic | `led/register/20000001` |
| QoS | `1` |
| Payload | 见下方 |

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

点击 **发送** ✈

**预期结果：**
- 服务端日志输出：`[MQTT] 屏幕注册: 20000001`
- 若 `t_screen` 表中不存在 `mqtt_client_id = 20000001` 的记录，**服务端会自动创建一条新屏幕记录**：
  - 屏幕名称：`自动注册-20000001`
  - 状态：`online`
  - IP、型号、分辨率、亮度取自注册 payload
- 若记录已存在，则更新其状态、心跳时间和 payload 中的字段
- 数据库 `t_screen` 表对应记录 `status` 变为 `online`，`last_heartbeat` 刷新
- 管理端 WebSocket 推送在线状态

> **提示**：如果管理后台没看到新设备，请检查 payload 是否为合法 JSON，以及 `mqttClientId` 是否为 2 开头 8 位数字。

---

## 四、模拟心跳通信

### 步骤 4：回复服务端 Ping

服务端每 30 秒向 `led/ping/20000001` 发送 ping 消息：
```json
{"type":"ping"}
```

设备收到后应立即回复心跳。

**操作（收到 ping 后）：**

| 字段 | 值 |
|------|-----|
| Topic | `led/heartbeat/20000001` |
| QoS | `1` |
| Payload | 见下方 |

```json
{
  "type": "heartbeat",
  "status": "online",
  "brightness": 75,
  "temperature": 42
}
```

点击 **发送** ✈

**预期结果：**
- 服务端更新 `last_heartbeat`，维持设备在线状态
- 如 30 秒内未收到心跳，`ScreenOfflineChecker` 将标记设备离线

---

## 五、模拟设备主动上报状态

### 步骤 5：发送状态变更消息

设备在亮度变化、节目切换时主动上报。

| 字段 | 值 |
|------|-----|
| Topic | `led/status/20000001` |
| QoS | `1` |
| Payload | 见下方 |

```json
{
  "status": "playing",
  "brightness": 90,
  "programName": "促销广告A",
  "volume": 50
}
```

点击 **发送** ✈

**预期结果：**
- 服务端更新 `screen.brightness`、`screen.status`
- 管理端 WebSocket 推送状态变更

---

## 六、接收下行命令（模拟验证）

### 6.1 接收播放指令

当管理后台下发播放计划时，MQTTX 订阅列表中会收到消息：

**Topic:** `led/command/20000001/play`

**Payload（多素材轮播）：**
```json
{
  "type": "play",
  "programName": "轮播套餐",
  "items": [
    {
      "type": "image",
      "url": "https://cdn.example.com/01.jpg",
      "duration": 10
    },
    {
      "type": "video",
      "url": "https://cdn.example.com/ad.mp4",
      "duration": 30
    },
    {
      "type": "text",
      "content": "欢迎光临",
      "duration": 5,
      "fontSize": 48,
      "color": "#FF0000"
    }
  ]
}
```

### 6.2 接收控制指令

**Topic:** `led/command/20000001/command`

| 指令 | Payload |
|------|---------|
| 开机 | `{"type":"power_on"}` |
| 关机 | `{"type":"power_off"}` |
| 停止 | `{"type":"stop"}` |
| 暂停 | `{"type":"pause"}` |
| 恢复 | `{"type":"resume"}` |
| 调亮度 | `{"type":"brightness","value":80}` |

### 6.3 模拟设备回复确认

设备收到命令后建议回复一条状态确认：

```json
// 发送到 led/status/20000001
{"status":"playing","command":"ack","programName":"轮播套餐"}
```

---

## 七、完整通信流程时序图

```
  MQTTX(20000001)               MQTT Broker               LED Cloud Server
       │                            │                           │
       │① CONNECT(20000001)────────►│                           │
       │◄─────────CONNACK───────────│                           │
       │                            │                           │
       │② SUBSCRIBE────────────►    │                           │
       │   led/ping/20000001        │                           │
       │   led/command/+/20000001   │                           │
       │                            │                           │
       │③ PUBLISH──────────────►    │                           │
       │   led/register/20000001    │──────────►                │
       │   {ip,model,version...}    │    更新数据库，标记online   │
       │                            │                           │
       │                            │          ◄─── 每30秒 ────│
       │◄──── PUBLISH ──────────────│  led/ping/20000001        │
       │    {"type":"ping"}         │                           │
       │                            │                           │
       │④ PUBLISH──────────────►    │                           │
       │   led/heartbeat/20000001   │──────────►                │
       │   {type:"heartbeat"...}    │    刷新last_heartbeat      │
       │                            │                           │
       │                            │     ◄── 后台下发播放计划 ──│
       │◄──── PUBLISH ──────────────│  led/command/../play      │
       │    {type:"play",items:[..]│                            │
       │                            │                           │
       │⑤ PUBLISH──────────────►    │                           │
       │   led/status/20000001      │──────────►                │
       │   {status:"playing"...}    │    更新播放状态            │
```

---

## 八、验证清单

使用此清单逐步验证每个环节：

- [ ] **连接**：MQTTX 显示 `Connected`
- [ ] **订阅**：3 个下行 Topic 订阅成功
- [ ] **注册**：发送 `led/register/20000001`，数据库 `t_screen.mqtt_client_id='20000001'` 记录 `status=online`
- [ ] **心跳**：收到 `led/ping/20000001` 后回复 `led/heartbeat/20000001`，`last_heartbeat` 刷新
- [ ] **状态**：发送 `led/status/20000001`，管理端 WebSocket 收到推送
- [ ] **播放**：后台下发节目，MQTTX 收到 `led/command/20000001/play` 消息
- [ ] **控制**：后台操作（开关机/调亮度/暂停），MQTTX 收到 `led/command/20000001/command` 消息
- [ ] **离线**：停止发送心跳 30 秒后，管理端显示设备离线

---

## 九、常见问题

### Q1: 连接失败 "Connection refused"
- 确认 MQTT Broker（Mosquitto）容器已启动：`docker ps | findstr mosquitto`
- 检查端口：`netstat -an | findstr 1886`
- Docker 环境使用端口 `1886`，本地直连使用 `1883`

### Q2: 发送注册消息后数据库无变化
- 确认 `led-device` 服务已启动（端口 9002）
- 查看服务端日志是否有 `[MQTT IN]` 输出
- 检查数据库 `t_screen` 表是否已有 `mqtt_client_id='20000001'` 的记录

### Q3: 收不到 ping 消息
- ping 由 `ScreenOfflineChecker` 定时发送，默认 30 秒一次
- 需要设备已完成注册且状态为 online 才会收到

### Q4: MQTTX 断线重连
- MQTTX 设置中开启 "Automatic Reconnect"
- 重连后需重新发送注册消息

---

## 十、多设备模拟

如需同时模拟多台设备，可复制 MQTTX 连接，修改 Client ID：

| 设备 | Client ID | 注册 Topic |
|------|-----------|------------|
| 设备 1 | `20000001` | `led/register/20000001` |
| 设备 2 | `20000002` | `led/register/20000002` |
| 设备 3 | `20000003` | `led/register/20000003` |

> 注意：数据库 `t_screen` 表需预先存在对应 `mqtt_client_id` 的记录，否则注册时仅更新已有记录。

---

## 附录：MQTTX 导出配置

以下 JSON 可直接导入 MQTTX（文件 → 导入）：

```json
{
  "name": "LED-20000001",
  "clientId": "20000001",
  "host": "mqtt://localhost",
  "port": 1886,
  "protocol": "mqtt://",
  "clean": true,
  "keepalive": 30,
  "subscriptions": [
    { "topic": "led/ping/20000001", "qos": 1 },
    { "topic": "led/command/20000001/play", "qos": 1 },
    { "topic": "led/command/20000001/command", "qos": 1 }
  ]
}
```

保存为 `mqttx-connection.json` → MQTTX → 右上角 ⚙ → Import → 选择该文件。
