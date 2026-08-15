---
tags:
  - 消息队列
  - 中间件
category: 消息队列
---

# Kafka

## Q: Kafka 是什么？

**Kafka** 是一个分布式流处理平台（Streaming Platform），最初由 LinkedIn 开发，后成为 Apache 顶级项目。它本质上是一个**高吞吐量、低延迟的发布-订阅消息系统**，同时也可以作为分布式提交日志和流处理引擎使用。

三大核心能力：
- **消息队列**：发布-订阅模式的消息传递
- **分布式存储**：以追加日志方式持久化消息，支持回溯
- **流处理**：Kafka Streams 提供实时流处理能力

---

## Q: Kafka 的核心概念有哪些？

| 概念 | 说明 |
|------|------|
| **Broker** | Kafka 服务节点，一个集群由多个 Broker 组成 |
| **Topic** | 消息的逻辑分类，Producer 发布到 Topic，Consumer 从 Topic 订阅 |
| **Partition** | Topic 的物理分区，每个 Partition 是一个有序的追加日志；同一 Topic 可有多个 Partition，分布在不同 Broker 上 |
| **Replica** | Partition 的副本，分为 Leader（处理读写）和 Follower（同步数据） |
| **Producer** | 消息生产者，向 Topic 发送消息 |
| **Consumer** | 消息消费者，从 Topic 拉取消息 |
| **Consumer Group** | 消费者组，组内消费者共同消费一个 Topic 的所有 Partition，实现负载均衡 |
| **Offset** | 消息在 Partition 中的偏移量，Consumer 通过 Offset 标记消费位置 |

---

## Q: Kafka 的整体架构是怎样的？

```
                    ┌─────────────────────────────────────────────┐
                    │              Kafka Cluster                  │
                    │                                             │
 ┌──────┐    ┌──────┐  ┌──────────────────────┐  ┌──────┐  ┌──────┐
 │      │    │Broker│  │  Topic: order         │  │Broker│  │Broker│
 │      │    │  1   │  │  ┌─────┬─────┬─────┐ │  │  2   │  │  3   │
 │Prod- │───▶│      │  │  │ P0  │ P1  │ P2  │ │  │      │  │      │
 │ucer  │    │      │──│──├─────┤─────┤─────┤─│──│      │  │      │
 │      │    │      │  │  │ L/F │ L/F │ L/F │ │  │      │  │      │
 └──────┘    │      │  │  └─────┴─────┴─────┘ │  │      │  │      │
             └──────┘  └──────────────────────┘  └──────┘  └──────┘
                    │              │              │
                    └──────────────┼──────────────┘
                                   │ Pull
                    ┌──────────────┼──────────────┐
                    │              │              │
               ┌──────┐      ┌──────┐      ┌──────┐
               │Cons- │      │Cons- │      │Cons- │
               │umer1 │      │umer2 │      │umer3 │
               └──────┘      └──────┘      └──────┘
               └─── Consumer Group: group-a ───────┘
```

- Producer 将消息发送到 Topic 的某个 Partition
- Broker 集群存储消息，Leader Partition 处理读写请求
- Consumer 以 Pull 方式从 Partition 拉取消息

---

## Q: Kafka 如何实现高吞吐量？

Kafka 的高吞吐量来自三个关键设计：

| 技术 | 原理 | 效果 |
|------|------|------|
| **顺序写磁盘** | 消息以追加（Append）方式写入磁盘日志，避免随机寻址 | 磁盘顺序写速度可达 600MB/s，远超随机写的 100KB/s |
| **零拷贝（Zero Copy）** | 使用 `sendfile()` 系统调用，数据从磁盘直接传输到网卡，跳过用户态拷贝 | 减少两次 CPU 拷贝和两次上下文切换 |
| **批量压缩** | Producer 将多条消息打包批量发送，支持 GZIP/Snappy/LZ4/ZSTD 压缩 | 减少网络 IO 和磁盘占用 |

```
传统拷贝：  Disk → 内核缓冲区 → 用户缓冲区 → Socket缓冲区 → 网卡
零拷贝：    Disk → 内核缓冲区 ──────────────────────────────→ 网卡
```

---

## Q: Kafka 如何保证消息持久化？

- 消息以追加日志形式**存储在磁盘**，不依赖内存缓存
- 通过 `log.retention.hours`/`log.retention.bytes` 控制消息保留策略（基于时间或大小）
- 消费完成后**不会立即删除**，支持回溯消费（重新设置 Offset）
- 副本机制（Replica）保证单节点故障不丢数据

---

## Q: Partition 与 Consumer Group 的关系是什么？

**核心规则：一个 Partition 只能被同一个 Consumer Group 中的一个 Consumer 消费。**

```
Topic: order（3 个 Partition）

Consumer Group A:
  Consumer A1 → P0
  Consumer A2 → P1
  Consumer A3 → P2

Consumer Group B:
  Consumer B1 → P0, P1
  Consumer B2 → P2
```

| 情况 | 结果 |
|------|------|
| Consumer 数 = Partition 数 | 每个 Consumer 消费一个 Partition，负载均衡 |
| Consumer 数 < Partition 数 | 部分 Consumer 消费多个 Partition |
| Consumer 数 > Partition 数 | 多出的 Consumer 空闲（无法消费任何 Partition） |

> 设计建议：Consumer 数量不应超过 Partition 数量，否则会有 Consumer 空闲。

---

## Q: Kafka 如何保证消息顺序性？

- **Kafka 只保证同一 Partition 内消息有序**
- 不同 Partition 之间**不保证**全局顺序
- 如需全局有序，只能使用 1 个 Partition（牺牲并行度）

```
Partition 0:  m1 → m2 → m3 → m4   ✅ 有序
Partition 1:  m5 → m6 → m7 → m8   ✅ 有序
全局：        m1, m5, m2, m6, ...   ❌ 不保证顺序
```

---

## Q: 什么是 ISR 机制？

**ISR**（In-Sync Replicas）：与 Leader 保持同步的副本集合。

```
Partition 的副本列表：
  ISR = [Leader, Follower1]     ← 同步中的副本
  OSR = [Follower2]             ← 落后太多的副本（lag > replica.lag.time.max.ms）
  AR  = ISR + OSR               ← 所有副本
```

| 要素 | 说明 |
|------|------|
| **Leader** | 处理所有读写请求 |
| **Follower** | 从 Leader 拉取数据，保持同步 |
| **ISR** | 与 Leader 同步差距在阈值内的副本集合 |
| **OSR** | 超出同步阈值的副本 |
| **`min.insync.replicas`** | ISR 中最少需要几个副本才能写入（配合 `acks=all` 使用） |

当 Leader 宕机时，**优先从 ISR 中选举新 Leader**，确保数据不丢失。

---

## Q: Kafka 的消息投递语义有哪些？

| 语义 | 说明 | 实现方式 |
|------|------|---------|
| **At Most Once** | 最多一次，消息可能丢失但不会重复 | 先提交 Offset，再处理消息 |
| **At Least Once** | 至少一次，消息不会丢失但可能重复（默认） | 先处理消息，再提交 Offset |
| **Exactly Once** | 精确一次，不丢不重 | 幂等 Producer + 事务（Kafka 0.11+） |

```java
// 启用幂等 Producer（防止生产者重试导致重复）
props.put("enable.idempotence", "true");

// 启用事务
props.put("transactional.id", "order-tx-1");
producer.initTransactions();
producer.beginTransaction();
try {
    producer.send(record);
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
}
```

---

## Q: Kafka 适用于哪些场景？

| 场景 | 说明 |
|------|------|
| **日志收集** | 统一收集各服务日志，对接 ELK 等日志系统 |
| **流处理** | Kafka Streams / Flink 实时处理数据流 |
| **事件驱动架构** | 微服务间通过事件解耦，如订单创建 → 库存扣减 → 通知发货 |
| **消息队列** | 异步处理、流量削峰 |
| **数据管道** | 系统间数据同步，如数据库 CDC → Kafka → 数仓 |
| **用户行为追踪** | 收集用户点击/浏览行为，用于实时推荐和离线分析 |

---

## 一句话总结

Kafka 是基于分区和副本机制的分布式流处理平台，通过顺序写磁盘和零拷贝实现高吞吐，仅保证 Partition 内有序，适合日志收集、流处理等高吞吐场景。
