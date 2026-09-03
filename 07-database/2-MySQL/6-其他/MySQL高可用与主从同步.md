---
tags:
  - MySQL
category: 数据库/MySQL/其他
---

# MySQL 高可用与主从同步

## Q: MySQL 主从复制的流程是什么？

主从复制基于 **binlog** 实现，核心流程如下：

```
Master                         Slave
──────                         ─────
1. 事务提交                        4. IO线程请求binlog
2. 写入 binlog  ──────────────►   5. IO线程写入relay log
3. dump binlog events             6. SQL线程读取relay log
                                  7. SQL线程重放SQL到从库数据
```

### 详细步骤

| 步骤 | 角色 | 动作 |
|------|------|------|
| 1 | Master | 事务提交，将数据变更写入 binlog |
| 2 | Master | Dump 线程读取 binlog 发送给 Slave |
| 3 | Slave | **IO 线程**接收 binlog events，写入 **relay log（中继日志）** |
| 4 | Slave | **SQL 线程**读取 relay log，重放 SQL 到从库 |

### 关键线程

| 线程 | 所在 | 职责 |
|------|------|------|
| Binlog Dump 线程 | Master | 读取 binlog 发送给 Slave |
| IO 线程 | Slave | 接收 binlog，写入 relay log |
| SQL 线程 | Slave | 读取 relay log，重放 SQL |

```sql
-- 查看主从状态
SHOW SLAVE STATUS\G

-- 关键字段
-- Slave_IO_Running: Yes     -- IO线程正常
-- Slave_SQL_Running: Yes    -- SQL线程正常
-- Seconds_Behind_Master: 0  -- 延迟秒数
-- Relay_Master_Log_File     -- 已重放到的主库binlog文件
-- Exec_Master_Log_Pos       -- 已重放到的binlog位置
```

---

## Q: 主从复制有哪几种模式？

| 模式 | 流程 | 性能 | 数据安全 | 适用场景 |
|------|------|------|---------|---------|
| **异步复制**（默认） | 主写完binlog即返回客户端 | 最高 | 最低（主宕机可能丢数据） | 对一致性要求不高 |
| **半同步复制** | 主写binlog + 等至少1个从IO ACK后返回 | 中等 | 中等（最多丢1个事务） | 大多数生产环境 |
| **全同步复制** | 主等所有从都重放完才返回 | 最低 | 最高（不丢数据） | 金融级强一致 |

### 半同步复制配置

```sql
-- 主库安装半同步插件
INSTALL PLUGIN rpl_semi_sync_master SONAME 'semisync_master.so';
SET GLOBAL rpl_semi_sync_master_enabled = 1;
SET GLOBAL rpl_semi_sync_master_timeout = 1000;  -- 等ACK超时ms，超时降级为异步

-- 从库安装半同步插件
INSTALL PLUGIN rpl_semi_sync_slave SONAME 'semisync_slave.so';
SET GLOBAL rpl_semi_sync_slave_enabled = 1;
```

> **注意**：半同步等待的是 IO 线程 ACK（binlog 写到 relay log），不是 SQL 线程重放完。所以从库数据仍有短暂延迟。

---

## Q: 主从延迟的原因和解决方案？

### 延迟原因

| 原因 | 说明 |
|------|------|
| **主库并行写，从库单线程重放** | MySQL 5.6 前 SQL 线程单线程，重放速度跟不上 |
| **大事务** | 一个事务更新百万行，从库重放耗时长 |
| **从库机器性能差** | CPU/IO 不如主库 |
| **主库写多读少，从库还承担查询** | 从库压力大会拖慢重放 |
| **网络延迟** | 主从之间网络慢，binlog 传输慢 |
| **DDL 操作** | ALTER TABLE 等大 DDL 在从库重放很慢 |

### 解决方案

| 方案 | 说明 |
|------|------|
| **并行复制** | MySQL 5.6 库级并行 → 5.7 组提交（LOGICAL_CLOCK）并行 → 8.0 WRITESET 并行 |
| **缩小事务** | 拆大事务为小事务，每次更新少量行 |
| **从库机器升配** | CPU/SSD 更强 |
| **读流量分担** | 多挂几个从库分担读 |
| **延迟检测** | 监控 `Seconds_Behind_Master`，超阈值告警 |

```sql
-- MySQL 5.7 开启基于组提交的并行复制
STOP SLAVE;
SET GLOBAL slave_parallel_type = 'LOGICAL_CLOCK';
SET GLOBAL slave_parallel_workers = 8;   -- 8个Worker线程
START SLAVE;
```

---

## Q: MySQL 高可用方案有哪些？

### 1. MHA（Master High Availability）

| 项目 | 说明 |
|------|------|
| 原理 | 主库宕机时，从**最新从库**提升为新主，其余从库切换到新主 |
| 优点 | 切换快（10~30秒），数据丢失最少 |
| 缺点 | 需要额外 Manager 节点，仅支持异步/半同步，已不太活跃维护 |
| 适用 | 传统主从架构 |

### 2. MySQL InnoDB Cluster（Group Replication）

| 项目 | 说明 |
|------|------|
| 原理 | 基于 **Paxos** 协议的组复制，多主或单主模式 |
| 组件 | MySQL Shell + MySQL Router + Group Replication |
| 优点 | 官方方案、自带冲突检测、支持多主写入 |
| 缺点 | 要求 MySQL 5.7+、网络延迟敏感、写性能有损耗 |
| 适用 | 中小规模、追求官方方案 |

### 3. Orchestrator

| 项目 | 说明 |
|------|------|
| 原理 | GitHub 开源的 MySQL HA 和拓扑管理工具 |
| 优点 | 自动故障恢复、可视化拓扑、支持 GTID、可回溯操作 |
| 缺点 | 本身不提供复制功能，只管理已有拓扑 |
| 适用 | 大规模 MySQL 集群管理 |

---

## Q: 读写分离怎么实现？

### 两种主要方式

| 方式 | 实现 | 优点 | 缺点 |
|------|------|------|------|
| **代理层（Proxy）** | ProxySQL / Mycat / ShardingSphere-Proxy | 对应用透明、集中管理 | 代理层是单点/瓶颈、多一跳延迟 |
| **应用层** | 代码中配多数据源，AOP/注解决定走主/从 | 无额外组件、灵活 | 改代码、各服务各自实现 |

```java
// 应用层读写分离示例（Spring + AOP）
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {
}

@Around("@annotation(readOnly)")
public Object around(ProceedingJoinPoint pjp, ReadOnly readOnly) {
    DynamicDataSource.setSlave();  // 切换到从库
    try {
        return pjp.proceed();
    } finally {
        DynamicDataSource.clear();
    }
}
```

### ProxySQL 配置示例

```sql
-- 配置主库（写）
INSERT INTO mysql_servers (hostgroup_id, hostname, port)
VALUES (10, 'master-ip', 3306);

-- 配置从库（读）
INSERT INTO mysql_servers (hostgroup_id, hostname, port)
VALUES (20, 'slave1-ip', 3306);
INSERT INTO mysql_servers (hostgroup_id, hostname, port)
VALUES (20, 'slave2-ip', 3306);

-- 读写分离规则
INSERT INTO mysql_query_rules (rule_id, active, match_digest, destination_hostgroup)
VALUES (1, 1, '^SELECT', 20);  -- SELECT 走读组
```

---

## Q: 故障切换（Failover）怎么做？

| 方式 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| **自动切换** | HA 工具检测主库宕机，自动提升从库 | 恢复快（秒级）、无需人工 | 脑裂风险、数据不一致风险 |
| **手动切换** | DBA 确认后执行切换 | 可控、安全 | 恢复慢（分钟级）、需值班 |
| **VIP 漂移** | 主库持有 VIP，切换时 VIP 漂移到新主 | 应用无感知（连的是 VIP） | 需要同网段、ARP 刷新 |

### VIP 漂移流程

```
正常：Master(192.168.1.100, VIP:192.168.1.200)  ←  应用连VIP
宕机：Slave1 提升为新主 → 执行 `ip addr add VIP` → ARP 通告
恢复：应用无感知，仍然连 192.168.1.200
```

```bash
# 新主绑定 VIP
ip addr add 192.168.1.200/24 dev eth0

# 发送免费 ARP 让交换机刷新 MAC
arping -c 3 -I eth0 192.168.1.200
```

---

## 八股速记

- **主从复制**：主库 binlog → 从库 relay log 回放；实现读写分离、高可用。存在**主从延迟**。
- **读写分离**：写主库、读从库，分摊读压力。
- **分库分表**：数据量大时按**水平（按行拆，如按 user_id 取模/范围）** 或**垂直（按列/业务拆）** 拆分。
- 带来的问题：分布式事务、跨库 join、全局唯一 id（雪花算法）、分页排序复杂。

**⭐ 易错**：主从延迟下"写完立即读"可能读到旧数据 → 强一致读走主库。这点测开做数据一致性测试要重点覆盖。

## 一句话总结

MySQL 主从复制基于 binlog + relay log 三线程模型，异步模式性能最高但有丢数据风险，半同步是生产常用折中；延迟靠并行复制+拆小事务解决，高可用选 MHA/InnoDB Cluster/Orchestrator，读写分离走 Proxy 或应用层，故障切换 VIP 漂移最简单但需同网段。
