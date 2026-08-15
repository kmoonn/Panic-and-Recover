---
tags:
  - Python
category: Python
---

# FastAPI

## Q：FastAPI 是什么？

FastAPI 是一个现代、高性能的 Python Web 框架，基于 **Starlette**（ASGI 框架）和 **Pydantic**（数据校验库）构建。它的核心设计理念是利用 Python 的类型提示来驱动 API 开发，自动生成交互式 OpenAPI 文档。

---

## 核心特点

| 特点 | 说明 |
|---|---|
| 高性能 | 基于 Starlette ASGI，性能媲美 Go（NodeJS/Starlette 量级） |
| 自动文档 | 根据类型提示自动生成 OpenAPI（Swagger UI / ReDoc）文档 |
| 类型提示驱动 | 利用 Python type hints 做参数校验、序列化和文档生成 |
| 异步原生 | 原生支持 async/await，基于 ASGI 协议 |
| 依赖注入 | 内置强大的 Depends 依赖注入系统 |
| 编辑器支持 | 类型提示带来完善的 IDE 自动补全和类型检查 |

---

## 与 Flask / Django 的对比

| 对比维度 | FastAPI | Flask | Django |
|---|---|---|---|
| 性能 | 高（ASGI 异步） | 中（WSGI 同步） | 中（WSGI 同步） |
| 异步支持 | 原生 async/await | 需要额外扩展 | Django 3.1+ 部分支持 |
| 自动API文档 | 内置 OpenAPI | 需 Flask-RESTX 等扩展 | 需 DRF + drf-spectacular |
| 数据校验 | 内置 Pydantic | 需手动或 Flask-Marshmallow | 需 DRF Serializer |
| 类型提示 | 核心设计 | 非核心 | 非核心 |
| 依赖注入 | 内置 Depends | 无 | 无内置 |
| ORM | 无内置（常用 SQLAlchemy/Tortoise） | 无内置 | 内置 Django ORM |
| Admin 后台 | 无 | 无 | 内置 |
| 学习曲线 | 低 | 低 | 中高 |
| 适用场景 | API 服务、微服务 | 小型应用、API 原型 | 全栈应用、CMS |

---

## 基本使用

### 路由定义

```python
from fastapi import FastAPI

app = FastAPI()

@app.get("/")
def read_root():
    return {"message": "Hello World"}

@app.get("/items/{item_id}")
def read_item(item_id: int, q: str | None = None):
    return {"item_id": item_id, "q": q}
```

### 请求参数类型

| 参数类型 | 说明 | 示例 |
|---|---|---|
| Path | 路径参数 | `item_id: int` |
| Query | 查询参数 | `q: str \| None = None` |
| Body | 请求体 | `item: Item`（Pydantic 模型） |
| Header | 请求头 | `user_agent: str = Header(None)` |
| Cookie | Cookie | `session_id: str = Cookie(None)` |

```python
from fastapi import FastAPI, Path, Query, Body
from pydantic import BaseModel

class Item(BaseModel):
    name: str
    price: float
    description: str | None = None

@app.post("/items/")
def create_item(item: Item):          # Body 参数，自动校验
    return item

@app.get("/items/{item_id}")
def read_item(
    item_id: int = Path(..., gt=0),   # Path 参数，必须大于0
    q: str | None = Query(None, max_length=50),  # Query 参数
):
    return {"item_id": item_id, "q": q}
```

### 响应模型

```python
from pydantic import BaseModel

class ItemResponse(BaseModel):
    id: int
    name: str
    price: float

@app.post("/items/", response_model=ItemResponse)
def create_item(item: Item) -> ItemResponse:
    # response_model 控制输出字段和文档
    return {"id": 1, "name": item.name, "price": item.price}
```

---

## 依赖注入系统（Depends）

FastAPI 内置依赖注入，通过 `Depends` 实现，支持复用逻辑（认证、数据库连接等）。

```python
from fastapi import Depends, FastAPI, HTTPException

# 定义依赖
def get_db():
    db = Database()
    try:
        yield db
    finally:
        db.close()

def get_current_user(token: str = Header(...)):
    user = verify_token(token)
    if not user:
        raise HTTPException(status_code=401, detail="Invalid token")
    return user

# 使用依赖
@app.get("/me")
def read_me(user=Depends(get_current_user), db=Depends(get_db)):
    return {"username": user.name}
```

> **依赖可以嵌套**：一个 Depends 依赖可以依赖另一个 Depends。

---

## 中间件与异常处理

### 中间件

```python
from fastapi import FastAPI, Request
import time

app = FastAPI()

@app.middleware("http")
async def add_process_time(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    process_time = time.time() - start
    response.headers["X-Process-Time"] = str(process_time)
    return response
```

### 全局异常处理

```python
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

class CustomException(Exception):
    pass

@app.exception_handler(CustomException)
async def custom_handler(request: Request, exc: CustomException):
    return JSONResponse(status_code=400, content={"message": str(exc)})
```

---

## Pydantic 数据校验

Pydantic 是 FastAPI 的数据校验核心，所有请求体、响应体都通过 Pydantic 模型进行校验和序列化。

```python
from pydantic import BaseModel, Field, EmailStr, field_validator

class User(BaseModel):
    name: str = Field(..., min_length=2, max_length=50)
    email: EmailStr
    age: int = Field(..., ge=0, le=150)

    @field_validator("name")
    @classmethod
    def name_must_not_be_empty(cls, v):
        if not v.strip():
            raise ValueError("name cannot be empty")
        return v

# 自动校验 + 自动生成 JSON Schema + OpenAPI 文档
```

---

## 异步支持

FastAPI 原生支持 ASGI 异步，可以直接使用 `async def`：

```python
import httpx

@app.get("/external")
async def fetch_external():
    async with httpx.AsyncClient() as client:
        resp = await client.get("https://api.example.com/data")
        return resp.json()
```

> **注意**：同步函数（`def`）FastAPI 会在线程池中执行，不会阻塞事件循环；异步函数（`async def`）直接在事件循环中执行。如果函数内部有 CPU 密集计算，应使用 `def` 而非 `async def`。

---

## 面试高频：FastAPI 为什么快？

| 因素 | 说明 |
|---|---|
| **Starlette ASGI** | 基于 ASGI 协议，支持异步非阻塞 I/O，不同于 WSGI 的同步阻塞模型 |
| **Pydantic 序列化优化** | Pydantic v2 使用 Rust 核心重写，序列化/反序列化速度极快 |
| **uvicorn 服务器** | 基于 uvloop（Cython 实现的事件循环），比默认 asyncio 快 2-4 倍 |
| **无 ORM 负担** | 不内置 ORM，减少了框架层面的性能开销 |
| **类型提示编译优化** | 类型提示在启动时完成解析，运行时零开销 |

简单理解：**ASGI 异步 + uvloop 高性能事件循环 + Pydantic Rust 核心序列化**，三重加速。

---

## 一句话总结

FastAPI 是基于 Starlette + Pydantic 的高性能 Python Web 框架，核心优势是类型提示驱动的自动文档与数据校验、原生异步支持、内置依赖注入，性能媲美 Go 的原因在于 ASGI 异步 + uvloop + Pydantic Rust 核心。
