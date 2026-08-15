---
tags:
  - Docker
  - 容器化
category: 容器化/Docker
---

# Docker 容器原理

## Q: 容器和虚拟机有什么区别？

| 对比项 | 容器（Container） | 虚拟机（VM） |
|--------|------------------|-------------|
| 虚拟化层级 | 操作系统级虚拟化 | 硬件级虚拟化 |
| 内核 | **共享宿主机内核** | 每个VM有**独立 Guest OS 内核** |
| 隔离方式 | Namespace（资源隔离）+ Cgroups（资源限制） | Hypervisor（硬件虚拟化） |
| 启动速度 | 秒级（本质是进程） | 分钟级（需启动整个OS） |
| 资源开销 | 极小（无Guest OS开销） | 大（每个VM完整OS占用GB级） |
| 镜像大小 | MB 级 | GB 级 |
| 性能 | 接近原生 | 有虚拟化损耗 |
| 隔离性 | 弱（共享内核，内核漏洞影响所有容器） | 强（完全隔离） |

```
┌─────────────────────────────────────┐  ┌─────────────────────────────────┐
│  App A  │  App B  │  App C         │  │  App A  │  App B  │  App C     │
│─────────│─────────│────────────────│  │─────────│─────────│────────────│
│Bins/Libs│Bins/Libs│Bins/Libs       │  │Bins/Libs│Bins/Libs│Bins/Libs   │
│─────────│─────────│────────────────│  │─────────│─────────│────────────│
│   Container Engine  (Docker)       │  │  Guest OS│ Guest OS│ Guest OS  │
│────────────────────────────────────│  │─────────│─────────│────────────│
│          Host OS Kernel            │  │  Hypervisor (KVM/VMware)       │
│────────────────────────────────────│  │────────────────────────────────│
│          Hardware                  │  │          Host OS Kernel        │
└────────────────────────────────────┘  │────────────────────────────────│
          容器                              │          Hardware            │
                                          └────────────────────────────────┘
                                                 虚拟机
```

---

## Q: Docker 底层用了哪些 Linux 技术？

### 1. Linux Namespace —— 资源隔离

| Namespace | 隔离内容 | 系统调用参数 | 说明 |
|-----------|---------|-------------|------|
| PID | 进程ID | CLONE_NEWPID | 容器内 PID 1 是 init，宿主机看到不同 PID |
| NET | 网络栈 | CLONE_NEWNET | 独立网络设备、IP、端口、路由表 |
| MNT | 挂载点 | CLONE_NEWNS | 独立文件系统挂载视图 |
| UTS | 主机名/域名 | CLONE_NEWUTS | 容器有自己的 hostname |
| IPC | 进程间通信 | CLONE_NEWIPC | 独立 System V IPC / POSIX 消息队列 |
| USER | 用户ID | CLONE_NEWUSER | 容器内 root 映射到宿主机普通用户 |

```c
// Namespace 示例（C 语言）
clone(child_func, stack, CLONE_NEWPID | CLONE_NEWNET | CLONE_NEWNS, NULL);
// 子进程拥有独立的 PID / 网络 / 挂载命名空间
```

### 2. Linux Cgroups —— 资源限制

| 子系统 | 限制内容 | 说明 |
|--------|---------|------|
| cpu | CPU 使用时间 | 限制 CPU 占比/核数 |
| memory | 内存使用量 | OOM 时杀容器进程 |
| blkio | 块设备 IO | 限制磁盘读写带宽 |
| cpuset | CPU 核心 | 绑定容器到指定 CPU 核 |
| devices | 设备访问 | 控制可访问的设备 |
| pids | 进程数 | 防止 fork 炸弹 |

```bash
# Docker 资源限制示例
docker run -d \
    --cpus=2 \              # 最多用2个CPU核
    --memory=4g \           # 最多用4GB内存
    --memory-swap=8g \      # 内存+swap 总共8GB
    --pids-limit=100 \      # 最多100个进程
    nginx
```

### 3. UnionFS（联合文件系统）—— 镜像分层

- Docker 镜像由**多个只读层**叠加组成
- 容器启动时在最上层添加**可写层**（Container Layer）
- 底层原理：OverlayFS / AUFS / DeviceMapper

---

## Q: Docker 镜像分层原理是什么？

```
┌──────────────────────┐
│   可写层（Container）  │  ← 容器运行时修改写这里（CoW）
├──────────────────────┤
│   Layer 4: EXPOSE 80  │  ← 只读
├──────────────────────┤
│   Layer 3: COPY . .   │  ← 只读
├──────────────────────┤
│   Layer 2: RUN apt... │  ← 只读
├──────────────────────┤
│   Layer 1: FROM ubuntu│  ← 只读（基础镜像）
└──────────────────────┘
```

### CoW（Copy-on-Write）机制

| 操作 | 行为 |
|------|------|
| 读取文件 | 从上到下逐层查找，找到即返回 |
| 修改文件 | 先从下层**复制**到可写层，再修改（不改变下层） |
| 删除文件 | 在可写层创建 **whiteout 文件**标记删除（不删下层） |

> **优点**：多个容器共享同一基础镜像层，节省磁盘和内存；`docker pull` 只下载本地缺少的层。

### Dockerfile 最佳实践（减少层数）

```dockerfile
# 差：每条 RUN 产生一层
RUN apt-get update
RUN apt-get install -y python3
RUN apt-get install -y vim
# 3 个只读层

# 好：合并 RUN，减少层数
RUN apt-get update && \
    apt-get install -y python3 vim && \
    rm -rf /var/lib/apt/lists/*
# 1 个只读层，且清理缓存减小镜像体积
```

---

## Q: 两个 Docker 容器如何跨机器通信？

### 同一宿主机上的容器通信

| 方式 | 原理 | 说明 |
|------|------|------|
| **Bridge 网络（默认）** | docker0 网桥 + veth pair | 同一 bridge 网络下容器可互通 |
| **Host 网络** | 容器直接用宿主机网络栈 | 无隔离，端口冲突风险 |
| **None 网络** | 无网络 | 需手动配 |
| **自定义 Bridge** | `docker network create` | 比 docker0 更好：支持 DNS 名称解析 |

```bash
# 自定义 Bridge 网络（推荐）
docker network create mynet
docker run -d --name app1 --network mynet nginx
docker run -d --name app2 --network mynet nginx

# app1 可直接用 DNS 名访问 app2
# 在 app1 内：curl http://app2:80  ✅
```

### 跨宿主机容器通信

| 方式 | 原理 | 适用场景 |
|------|------|---------|
| **Overlay 网络** | VXLAN 隧道封装，跨主机虚拟二层网络 | Docker Swarm 原生方案 |
| **Host 网络 + 外部 SDN** | 容器用 host 网络，靠外部 SDN/云 VPC 互通 | K8s 常见 |
| **端口映射 + 外部 LB** | `-p 80:80` 映射到宿主机端口，外部 LB 转发 | 简单部署 |
| **Macvlan** | 容器直接分配物理网络 IP/MAC | 需要容器直接在物理网络 |

```bash
# Overlay 网络（需先创建 Swarm 或 etcd 集群做服务发现）
docker network create -d overlay --subnet=10.10.0.0/16 my-overlay

# 跨主机容器互通
# Host A:
docker run -d --name web --network my-overlay nginx
# Host B:
docker run -d --name api --network my-overlay my-api
# web 容器可直接 curl http://api:8080  ✅
```

```
Host A                              Host B
┌──────────┐    VXLAN Tunnel    ┌──────────┐
│ Container │ ◄════════════════►│ Container │
│ 10.10.0.2 │    (封封装解)      │ 10.10.0.3 │
└──────────┘                     └──────────┘
     │                                 │
  docker0/overlay                   docker0/overlay
     │                                 │
  Host A NIC ────── 物理网络 ────── Host B NIC
```

---

## Q: Docker Compose 是什么？

Docker Compose 用于**单机多容器编排**，通过 YAML 文件定义和启动多个关联容器。

```yaml
# docker-compose.yml
version: '3.8'
services:
  web:
    build: .
    ports:
      - "80:80"
    depends_on:
      - db
      - redis
    networks:
      - app-net

  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: secret
    volumes:
      - db-data:/var/lib/mysql
    networks:
      - app-net

  redis:
    image: redis:7
    networks:
      - app-net

volumes:
  db-data:

networks:
  app-net:
    driver: bridge
```

```bash
docker-compose up -d      # 启动所有服务
docker-compose ps         # 查看状态
docker-compose logs -f    # 查看日志
docker-compose down       # 停止并删除
```

> **Compose vs K8s**：Compose 适合开发/测试单机多容器；K8s 适合生产分布式多节点编排。

---

## 一句话总结

Docker 容器本质是共享宿主机内核的隔离进程，靠 Namespace 隔离资源、Cgroups 限制资源、UnionFS 分层存储；镜像由只读层 + 可写层（CoW）组成；同主机容器走 Bridge 网络互通，跨主机走 Overlay（VXLAN）或 Host+外部 SDN；多容器编排单机用 Compose，集群用 K8s。
