---
tags:
  - 消息队列
  - 中间件
category: 消息队列
---

# Kafka与RabbitMQ的区别

## Q: Kafka 和 RabbitMQ 有什么区别？

### 核心对比

| 对比维度 | Kafka | RabbitMQ |
|---------|-------|----------|
| **协议** | 自定义二进制协议 | AMQP 0-9-1（也支持 STOMP/MQTT） |
| **模型** | 发布-订阅模型（Topic/Partition） | 消息代理模型（Exchange/Queue） |
| **消息顺序** | 同一 Partition 内有序，跨 Partition 无序 | 单队列内 FIFO 有序 |
| **吞吐量** | 极高（百万级 TPS） | 中等（万级 TPS） |
| **延迟** | 毫秒级 | 微秒级 |
| **消息持久化** | 磁盘追加日志，默认持久化 | 需显式配置 Exchange/Queue/Message 持久化 |
| **消息回溯** | 支持（修改 Offset 即可重新消费） | 不支持（消息 ACK 后即删除） |
| **消费模型** | Pull（消费者拉取） | Push（Broker 推送） |
| **消息保留** | 基于时间/大小策略保留，消费不删除 | 消费确认后删除 |
| **路由灵活性** | 基于 Topic 和 Partition，路由简单 | Exchange 四种类型，路由灵活 |
| **消息确认** | Offset 提交 | Producer Confirm + Consumer ACK |
| **可靠性** | 副本机制（ISR） | 确认机制 + 持久化 |
| **适用场景** | 高吞吐日志/流处理、大数据 | 复杂路由、业务消息、中小规模 |
| **社区生态** | 大数据生态（Kafka Streams/Flink/Spark） | 企业应用生态（Spring AMQP 等） |
| **运维复杂度** | 较高（依赖 ZooKeeper/KRaft） | 较低（内置管理界面） |

---

## Q: Kafka 和 RabbitMQ 的架构差异是什么？

### 消费模型：拉取式 vs 推送式

```
Kafka（Pull 模式）：
  Consumer 主动从 Broker 拉取消息
  ┌──────┐          ┌──────┐
  │Broker│◀─Pull────│Cons- │
  │      │          │umer  │
  └──────┘          └──────┘
  优点：Consumer 按自身速率消费，不会压垮
  缺点：无消息时仍需轮询（长轮询优化）

RabbitMQ（Push 模式）：
  Broker 主动向 Consumer 推送消息
  ┌──────┐          ┌──────┐
  │Broker│──Push───▶│Cons- │
  │      │          │umer  │
  └──────┘          └──────┘
  优点：消息实时性高，延迟低
  缺点：Consumer 处理慢时可能积压（需限流 prefetch）
```

### 存储模型：日志 vs 队列

```
Kafka：
  Topic → 多个 Partition → 每个 Partition 是一个追加日志
  消息不因消费而删除，支持多 Consumer Group 回溯消费
  ┌─────────────────────────────┐
  │ Partition 0: m1 m2 m3 m4 m5 │ ← Consumer A offset=3
  │                    ↑         │ ← Consumer B offset=1
  └─────────────────────────────┘

RabbitMQ：
  Exchange → Binding → Queue
  消息被消费确认后即删除，不支持回溯
  ┌───────────┐
  │ Queue: m1  │ → Consumer ACK → 删除 m1
  │ Queue: m2  │ → 等待消费
  └───────────┘
```

---

## Q: Kafka 和 RabbitMQ 的消息可靠性机制有什么不同？

| 可靠性机制 | Kafka | RabbitMQ |
|-----------|-------|----------|
| **冗余** | Partition 多副本（Leader/Follower），ISR 保证同步 | 镜像队列（Queue HA）或仲裁队列（Quorum Queue） |
| **生产端确认** | `acks=all`：所有 ISR 副本确认写入才算成功 | Publisher Confirm：Broker 确认消息已路由到队列 |
| **消费端确认** | 手动提交 Offset（避免消息丢失） | 手动 ACK（避免消息丢失） |
| **持久化** | 默认写入磁盘日志 | 需三层配置：Exchange + Queue + Message 均设 durable |
| **数据丢失风险** | `acks=all` + `min.insync.replicas>=2` + 手动提交 Offset → 基本不丢 | 持久化 + Publisher Confirm + 手动 ACK → 基本不丢 |

---

## Q: 如何选择 Kafka 和 RabbitMQ？

### 选 Kafka 的场景

- **高吞吐量需求**：日志收集、用户行为追踪、数据管道，TPS 在十万到百万级
- **流处理**：需要对接 Kafka Streams / Flink / Spark 做实时计算
- **消息回溯**：需要重新消费历史数据（如修复数据、重新计算指标）
- **大数据生态**：与 Hadoop/Spark/Flink 等天然集成
- **事件驱动架构**：大量事件的发布订阅

### 选 RabbitMQ 的场景

- **低延迟需求**：业务消息需要在微秒级送达
- **复杂路由**：需要根据 routing key / headers 灵活分发消息
- **中小规模**：单机或小集群即可满足业务
- **延迟消息**：需要原生或通过插件支持延迟投递
- **多协议**：需要支持 AMQP/STOMP/MQTT 等多种协议
- **运维简单**：内置管理界面，上手快

### 选型决策树

```
需要百万级 TPS？
  ├─ 是 → Kafka
  └─ 否 → 需要复杂路由？
              ├─ 是 → RabbitMQ
              └─ 否 → 需要消息回溯？
                          ├─ 是 → Kafka
                          └─ 否 → 需要微秒级延迟？
                                      ├─ 是 → RabbitMQ
                                      └─ 否 → 看团队技术栈和生态选型
```

---

## 一句话总结

Kafka 胜在吞吐量和持久化（日志模型、拉取消费、消息回溯），适合大数据和流处理；RabbitMQ 胜在灵活路由和低延迟（Exchange 模型、推送消费、确认机制），适合业务消息和复杂路由场景。
