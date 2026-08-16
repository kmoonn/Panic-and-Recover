---
tags:
  - 消息队列
  - 中间件
category: 消息队列
---

# RabbitMQ

## Q: RabbitMQ 是什么？

**RabbitMQ** 是一个基于 **AMQP**（Advanced Message Queuing Protocol，高级消息队列协议）协议的开源消息代理（Message Broker），由 Erlang 语言开发。它提供可靠的消息传递、灵活的路由和丰富的消息确认机制，是企业级消息通信的常用中间件。

核心特点：
- 支持 AMQP 0-9-1、STOMP、MQTT 等多种协议
- 内置管理界面，运维友好
- 支持消息确认、持久化、死信队列等可靠性机制
- 丰富的 Exchange 类型实现灵活路由

---

## Q: RabbitMQ 的核心概念有哪些？

| 概念 | 说明 |
|------|------|
| **Producer** | 消息生产者，将消息发送到 Exchange |
| **Exchange** | 交换机，接收 Producer 的消息并根据路由规则分发到 Queue |
| **Queue** | 消息队列，存储消息直到被 Consumer 消费 |
| **Binding** | 绑定关系，将 Exchange 与 Queue 关联，指定 routing key 规则 |
| **Consumer** | 消息消费者，从 Queue 中获取消息并处理 |
| **Virtual Host** | 虚拟主机，逻辑隔离单位，每个 vhost 有独立的 Exchange/Queue/权限 |
| **Connection** | 客户端与 Broker 的 TCP 连接 |
| **Channel** | 连接内的轻量级虚拟连接，复用 TCP 连接 |

---

## Q: RabbitMQ 的整体架构是怎样的？

```
Producer                         RabbitMQ Broker                          Consumer
┌──────┐     ┌─────────┐    ┌────────────────────────────┐     ┌──────┐
│      │     │         │    │                            │     │      │
│      │────▶│Exchange │───▶│ Binding ──▶ Queue ────────│────▶│      │
│      │     │         │    │              │              │     │      │
└──────┘     └─────────┘    │              │              │     └──────┘
                             │              ▼              │
                             │           Queue ────────│────▶ Consumer
                             │                            │
                             │  ┌──────────────────────┐  │
                             │  │    Virtual Host       │  │
                             │  │  Exchange / Queue /   │  │
                             │  │  Binding / Permission │  │
                             │  └──────────────────────┘  │
                             └────────────────────────────┘

消息流转：Producer → Exchange → (Binding 规则) → Queue → Consumer
```

---

## Q: Exchange 有哪些类型？

### 对比表

| Exchange 类型 | 路由规则 | 使用场景 | 示例 |
|--------------|---------|---------|------|
| **Direct** | 精确匹配 routing key | 点对点通信、路由到特定队列 | routing key = `order.create` 精确匹配 |
| **Fanout** | 广播到所有绑定队列，忽略 routing key | 广播通知、群发消息 | 所有绑定队列都收到消息 |
| **Topic** | 通配符匹配 routing key（`*` 匹配一个词，`#` 匹配零或多个词） | 发布-订阅、分类路由 | `order.*` 匹配 `order.create`，`order.#` 匹配 `order.create.success` |
| **Headers** | 基于消息头属性匹配，忽略 routing key | 复杂条件路由 | headers 中 `x-match=all` 要求所有头匹配 |

### 详细说明

**1. Direct Exchange**

```
Exchange: order.direct
  Binding: Queue-order     ← routing key: "order.create"
  Binding: Queue-payment   ← routing key: "payment.complete"

消息 routing key = "order.create" → 只进入 Queue-order
```

**2. Fanout Exchange**

```
Exchange: log.fanout
  Binding: Queue-log-file
  Binding: Queue-log-es
  Binding: Queue-log-alert

任何消息 → 同时进入三个队列（广播）
```

**3. Topic Exchange**

```
Exchange: order.topic
  Binding: Queue-all      ← routing key: "order.#"        （匹配 order 下所有）
  Binding: Queue-create   ← routing key: "order.create.*" （匹配 order.create.xxx）
  Binding: Queue-pay      ← routing key: "*.pay.*"        （匹配含 pay 的路由）

通配符：
  * (星号) — 恰好匹配一个单词，如 "order.*" 匹配 "order.create"，不匹配 "order.create.success"
  # (井号) — 匹配零个或多个单词，如 "order.#" 匹配 "order"、"order.create"、"order.create.success"
```

**4. Headers Exchange**

```
Exchange: msg.headers
  Binding: Queue-A  ← headers: {"format": "pdf", "x-match": "all"}
  Binding: Queue-B  ← headers: {"format": "any", "x-match": "any"}

x-match:
  all — 所有 header 字段都匹配
  any — 任一 header 字段匹配即可
```

---

## Q: RabbitMQ 的消息确认机制是怎样的？

RabbitMQ 从**生产端**和**消费端**两方面保证消息可靠性。

### 生产者确认（Publisher Confirm）

确保消息成功到达 Broker。

| 机制 | 说明 |
|------|------|
| **Confirm 机制** | Channel 开启 confirm 模式后，每条消息 Broker 会回送 ACK/NACK |
| **Return 机制** | 消息不可路由（Exchange 无法路由到 Queue）时回退消息 |

```java
// 开启 Confirm 模式
channel.confirmSelect();

// 异步确认回调
channel.addConfirmListener(new ConfirmListener() {
    public void handleAck(long deliveryTag, boolean multiple) {
        // 消息成功到达 Broker
    }
    public void handleNack(long deliveryTag, boolean multiple) {
        // 消息未到达，需重发
    }
});

// 不可路由消息退回
channel.addReturnListener(returnMessage -> {
    // 处理退回消息
});
```

### 消费者确认（Consumer ACK）

确保消息被消费者成功处理。

| 应答方式 | 说明 |
|---------|------|
| **自动 ACK** | 消息发送后立即确认，可能丢消息（不推荐） |
| **手动 ACK** | 消费者处理完后显式 ACK，处理失败则 NACK/Reject |

```java
// 手动 ACK
boolean autoAck = false;
channel.basicConsume(queueName, autoAck, new DefaultConsumer(channel) {
    @Override
    public void handleDelivery(String tag, Envelope envelope,
                               AMQP.BasicProperties props, byte[] body) {
        try {
            // 处理业务逻辑
            processMessage(body);
            // 成功 → 确认
            channel.basicAck(envelope.getDeliveryTag(), false);
        } catch (Exception e) {
            // 失败 → 拒绝并重回队列
            channel.basicNack(envelope.getDeliveryTag(), false, true);
        }
    }
});
```

---

## Q: RabbitMQ 如何实现消息持久化？

消息持久化需要**三个层面**同时设置：

| 层面 | 设置方式 | 说明 |
|------|---------|------|
| **Exchange 持久化** | `durable = true` | Broker 重启后 Exchange 不丢失 |
| **Queue 持久化** | `durable = true` | Broker 重启后 Queue 不丢失 |
| **Message 持久化** | `deliveryMode = 2` | 消息写入磁盘，重启后可恢复 |

```java
// Exchange 持久化
channel.exchangeDeclare("order.direct", "direct", true);  // durable = true

// Queue 持久化
channel.queueDeclare("order-queue", true, false, false, null);  // durable = true

// Message 持久化
AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
    .deliveryMode(2)  // 2 = persistent
    .build();
channel.basicPublish("order.direct", "order.create", props, messageBytes);
```

> 注意：仅设置 Message 持久化但 Exchange/Queue 非持久化，Broker 重启后消息仍会丢失。

---

## Q: 什么是死信队列（DLX）？

**DLX**（Dead Letter Exchange）：当消息成为"死信"后，会被自动转发到绑定的死信交换机，再路由到死信队列，用于后续排查或重试。

**消息成为死信的条件：**

| 条件 | 说明 |
|------|------|
| 消息被 NACK/Reject 且 `requeue = false` | 消费者拒绝且不重回队列 |
| 消息 TTL 过期 | 消息生存时间到期 |
| 队列达到最大长度 | 队列满，最早的消息被丢弃 |

```
普通队列配置 DLX：
  Queue: order-queue
    x-dead-letter-exchange: order.dlx
    x-dead-letter-routing-key: order.dead

消息成为死信 → 转发到 DLX → 路由到死信队列 → 人工处理或重试
```

```java
// 声明死信交换机和队列
channel.exchangeDeclare("order.dlx", "direct", true);
channel.queueDeclare("order-dead-queue", true, false, false, null);
channel.queueBind("order-dead-queue", "order.dlx", "order.dead");

// 普通队列绑定 DLX
Map<String, Object> args = new HashMap<>();
args.put("x-dead-letter-exchange", "order.dlx");
args.put("x-dead-letter-routing-key", "order.dead");
channel.queueDeclare("order-queue", true, false, false, args);
```

---

## Q: RabbitMQ 如何实现延迟消息？

### 方案一：TTL + DLX

消息设置 TTL，过期后进入死信队列，消费者监听死信队列实现延迟消费。

```
Producer → Exchange → Queue(TTL=30min, DLX) → [等待30分钟] → DLX → Dead Queue → Consumer
```

```java
// 延迟队列：消息存活 30 分钟后转入死信队列
Map<String, Object> args = new HashMap<>();
args.put("x-message-ttl", 30 * 60 * 1000);           // TTL = 30 分钟
args.put("x-dead-letter-exchange", "order.dlx");       // 死信交换机
args.put("x-dead-letter-routing-key", "order.timeout"); // 死信路由键
channel.queueDeclare("order-delay-queue", true, false, false, args);
```

**注意**：RabbitMQ 的 TTL 惰性检查机制可能导致队列头部的短 TTL 消息阻塞后续长 TTL 消息的过期投递，因此建议**每种延迟时间使用独立队列**。

### 方案二：延迟插件（rabbitmq-delayed-message-exchange）

安装延迟插件后，Exchange 直接支持延迟投递，无需 DLX。

```
Producer → Delayed Exchange(延迟x秒) → [等待] → Queue → Consumer
```

```java
// 声明延迟交换机
Map<String, Object> args = new HashMap<>();
args.put("x-delayed-type", "direct");
channel.exchangeDeclare("order.delay", "x-delayed-message", true, false, args);

// 发送延迟消息
AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
    .headers(Map.of("x-delay", 30 * 60 * 1000))  // 延迟 30 分钟
    .build();
channel.basicPublish("order.delay", "order.create", props, messageBytes);
```

### 两种方案对比

| 对比项 | TTL + DLX | 延迟插件 |
|--------|----------|---------|
| 原理 | 队列 TTL 过期转死信 | Exchange 端延迟投递 |
| 额外组件 | 不需要 | 需安装插件 |
| 延迟精度 | 惰性检查可能导致延迟 | 基于定时器，精度更高 |
| 队列数量 | 每种延迟时间一个队列 | 一个 Exchange 即可 |
| 推荐度 | 经典方案，通用 | 更简洁，推荐 |

---

## Q: RabbitMQ 适用于哪些场景？

| 场景 | 说明 |
|------|------|
| **异步处理** | 注册后异步发送邮件/短信，提升响应速度 |
| **应用解耦** | 订单系统与库存系统通过消息通信，互不依赖 |
| **流量削峰** | 秒杀请求先入队列，后端按速率消费 |
| **延迟任务** | 订单超时自动取消、定时提醒 |
| **日志收集** | 各服务发送日志到 RabbitMQ，统一存储分析 |
| **RPC 调用** | 利用 ReplyTo 和 CorrelationId 实现异步 RPC |

---

## 一句话总结

RabbitMQ 是基于 AMQP 协议的消息代理，通过 Exchange 灵活路由（Direct/Fanout/Topic/Headers），配合 Publisher Confirm 和 Consumer ACK 保证消息可靠性，适合复杂路由和业务消息场景。
