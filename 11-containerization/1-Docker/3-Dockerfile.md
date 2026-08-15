---
tags:
  - Docker
  - 容器化
category: 容器化/Docker
---

# Dockerfile 镜像构建文件

## Q：Dockerfile 是什么？

Dockerfile 是一个纯文本文件，包含一组**指令序列**，用于自动化构建 Docker 镜像。每条指令对应镜像中的一层（Layer），Docker 按顺序执行这些指令，最终生成可运行的镜像。

```bash
# 构建镜像
docker build -t myapp:1.0 .

# 基于镜像运行容器
docker run -d -p 8080:80 myapp:1.0
```

---

## 常用指令

| 指令 | 说明 | 示例 |
|---|---|---|
| `FROM` | 指定基础镜像（必须为第一条指令） | `FROM python:3.11-slim` |
| `RUN` | 构建时执行命令（创建新层） | `RUN apt-get update && apt-get install -y curl` |
| `COPY` | 复制本地文件到镜像 | `COPY app.py /app/app.py` |
| `ADD` | 复制文件（支持远程URL和自动解压tar） | `ADD app.tar.gz /opt/` |
| `WORKDIR` | 设置工作目录 | `WORKDIR /app` |
| `ENV` | 设置环境变量 | `ENV JAVA_HOME=/usr/lib/jvm/java-17` |
| `EXPOSE` | 声明容器监听端口（仅文档作用） | `EXPOSE 8080` |
| `CMD` | 容器启动时执行的默认命令（可被覆盖） | `CMD ["python", "app.py"]` |
| `ENTRYPOINT` | 容器启动时执行的入口命令（不可覆盖） | `ENTRYPOINT ["java", "-jar"]` |
| `VOLUME` | 声明数据卷挂载点 | `VOLUME ["/data"]` |
| `ARG` | 构建时变量（仅构建期有效） | `ARG VERSION=1.0` |
| `LABEL` | 添加元数据 | `LABEL maintainer="dev@example.com"` |

---

## CMD vs ENTRYPOINT

| 对比维度 | CMD | ENTRYPOINT |
|---|---|---|
| 用途 | 提供默认执行命令 | 定义入口点程序 |
| 是否可被覆盖 | `docker run` 参数会**覆盖** CMD | `docker run` 参数会**追加**到 ENTRYPOINT |
| 多次定义 | 只有最后一个生效 | 只有最后一个生效 |
| 必须性 | 不必须 | 不必须 |
| 组合使用 | 可作为 ENTRYPOINT 的默认参数 | 与 CMD 配合实现默认参数 |

```dockerfile
# 方式一：ENTRYPOINT + CMD 组合
ENTRYPOINT ["java", "-jar"]
CMD ["app.jar"]                    # 默认参数

# docker run myapp                  → java -jar app.jar
# docker run myapp other.jar        → java -jar other.jar（CMD被覆盖）

# 方式二：纯 CMD
CMD ["python", "app.py"]

# docker run myapp                  → python app.py
# docker run myapp script.py        → python script.py（整个CMD被覆盖）
```

---

## COPY vs ADD

| 对比维度 | COPY | ADD |
|---|---|---|
| 基本复制 | 支持 | 支持 |
| 远程URL | 不支持 | 支持（但不推荐，应用 curl/wget 替代） |
| 自动解压tar | 不支持 | 支持（本地 tar 文件自动解压到目标目录） |
| 推荐程度 | **推荐**（语义明确） | 仅在需要自动解压时使用 |

> **最佳实践**：优先使用 COPY，语义更清晰。只有在需要自动解压 tar 包时才用 ADD。

---

## 多阶段构建（Multi-stage Build）

多阶段构建通过多个 `FROM` 指令，在最终镜像中只保留运行时所需内容，大幅减小镜像体积。

```dockerfile
# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline          # 先下载依赖（利用缓存）
COPY src ./src
RUN mvn package -DskipTests            # 编译打包

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```dockerfile
# Python 多阶段构建示例
FROM python:3.11 AS builder
WORKDIR /build
COPY requirements.txt .
RUN pip install --user -r requirements.txt

FROM python:3.11-slim
WORKDIR /app
COPY --from=builder /root/.local /root/.local
COPY . .
ENV PATH=/root/.local/bin:$PATH
CMD ["python", "app.py"]
```

| 对比 | 无多阶段 | 多阶段 |
|---|---|---|
| Java 镜像体积 | ~800MB（含 Maven + JDK） | ~200MB（仅 JRE） |
| Python 镜像体积 | ~1.2GB（含编译工具链） | ~150MB（slim + 依赖） |
| 安全性 | 较低（含编译器等工具） | 较高（仅运行时） |

---

## Dockerfile 最佳实践

### 1. 使用小体积基础镜像

```dockerfile
# 不推荐
FROM python:3.11              # ~1GB

# 推荐
FROM python:3.11-slim         # ~150MB
FROM python:3.11-alpine       # ~50MB（但兼容性可能有问题）
```

### 2. 合并 RUN 减少层数

```dockerfile
# 不推荐：每条 RUN 创建一层
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y git
RUN rm -rf /var/lib/apt/lists/*

# 推荐：合并为一条 RUN
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl git && \
    rm -rf /var/lib/apt/lists/*
```

### 3. 利用构建缓存

Docker 按指令顺序构建，某层缓存失效后所有后续层都重新构建。

```dockerfile
# 不推荐：代码变更导致依赖重新安装
COPY . /app
RUN pip install -r requirements.txt

# 推荐：先复制依赖文件，利用缓存
COPY requirements.txt .
RUN pip install -r requirements.txt    # 依赖不变则缓存命中
COPY . /app                             # 代码变更不影响上层缓存
```

> **原则**：把不常变的指令放前面（系统依赖、pip install），把常变的放后面（COPY 源码）。

### 4. .dockerignore 文件

```
# .dockerignore
.git
__pycache__
*.pyc
node_modules
.venv
.env
*.md
docker-compose.yml
```

> 避免将无关文件发送到构建上下文，加速构建并减小镜像体积。

### 5. 非 root 用户运行

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY . .

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

CMD ["python", "app.py"]
```

---

## 完整示例

### Java 应用 Dockerfile

```dockerfile
# 多阶段构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 安全：非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080
HEALTHCHECK --interval=30s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Python 应用 Dockerfile

```dockerfile
FROM python:3.11-slim AS builder
WORKDIR /build
COPY requirements.txt .
RUN pip install --user --no-cache-dir -r requirements.txt

FROM python:3.11-slim
WORKDIR /app

# 安全：非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser appuser
COPY --from=builder /root/.local /root/.local
COPY . .
RUN chown -R appuser:appuser /app

ENV PATH=/root/.local/bin:$PATH
USER appuser

EXPOSE 8000
HEALTHCHECK --interval=30s CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

## 一句话总结

Dockerfile 是构建 Docker 镜像的指令文件，核心考点为 CMD 与 ENTRYPOINT 的区别（可覆盖vs追加）、COPY 与 ADD 的区别（推荐COPY）、多阶段构建减小镜像体积、以及最佳实践（小体积基础镜像、合并RUN减少层数、利用构建缓存、.dockerignore、非root用户运行）。
