---
tags:
  - 测试
category: 测试/框架
---

# Pytest

## Q：Pytest 是什么？

Pytest 是 Python 第三方测试框架，比标准库 unittest 更简洁、更强大、更易用。它以简洁的断言语法、强大的 fixture 机制和丰富的插件生态著称，是 Python 测试领域的事实标准。

---

## 核心特点

| 特点 | 说明 |
|---|---|
| 简洁断言 | 直接使用 Python 原生 `assert`，无需 `self.assertEqual()` |
| 自动发现 | 自动识别 `test_*.py` / `*_test.py` 文件和 `test_*` 函数 |
| 强大 Fixture | 灵活的测试固件机制，替代 setup/teardown |
| 参数化 | `@pytest.mark.parametrize` 一行搞定数据驱动 |
| 丰富插件 | 300+ 插件（覆盖率、HTML报告、并行、mock等） |
| 兼容 unittest | 可直接运行 unittest 测试用例 |

---

## 与 unittest 的对比

| 对比维度 | Pytest | unittest |
|---|---|---|
| 断言方式 | `assert x == 1`（原生 assert） | `self.assertEqual(x, 1)` |
| 测试发现 | 自动发现（约定优于配置） | 需手动组织 TestSuite |
| Fixture | `@pytest.fixture`，灵活强大 | `setUp`/`tearDown`，功能单一 |
| 参数化 | `@pytest.mark.parametrize` | 需 `ddt` 或 `subTest` |
| 插件生态 | 300+ 插件 | 较少 |
| 学习曲线 | 低 | 中 |
| 测试类 | 可选（纯函数即可） | 必须继承 `unittest.TestCase` |
| 报告 | 需插件（pytest-html） | 内置 TextTestRunner |
| 兼容性 | 可运行 unittest 用例 | — |

---

## 基本使用

### 测试文件/函数命名规则

| 规则 | 说明 |
|---|---|
| 文件名 | `test_*.py` 或 `*_test.py` |
| 类名 | `Test*`（不能有 `__init__` 方法） |
| 函数名 | `test_*` |
| 方法名 | `test_*`（在 Test* 类中） |

### 断言方式

```python
# Pytest：直接用 assert，失败信息自动分析
def test_addition():
    result = 1 + 1
    assert result == 2
    assert result > 0, "结果应该大于0"  # 可加自定义失败信息

# 对比 unittest
# self.assertEqual(result, 2)
# self.assertTrue(result > 0)
```

常用断言场景：
```python
def test_assertions():
    # 相等性
    assert a == b
    # 包含
    assert "hello" in "hello world"
    assert 3 in [1, 2, 3]
    # 异常
    with pytest.raises(ValueError):
        int("abc")
    # 异常 + 匹配消息
    with pytest.raises(ValueError, match="invalid literal"):
        int("abc")
    # 类型
    assert isinstance(result, dict)
    # 近似相等（浮点数）
    assert 0.1 + 0.2 == pytest.approx(0.3)
```

### 参数化：@pytest.mark.parametrize

```python
@pytest.mark.parametrize("input, expected", [
    (1, 2),
    (2, 4),
    (3, 6),
    (-1, -2),
])
def test_double(input, expected):
    assert input * 2 == expected

# 参数组合（多参数笛卡尔积）
@pytest.mark.parametrize("x", [1, 2])
@pytest.mark.parametrize("y", [10, 20])
def test_multiply(x, y):
    assert x * y > 0  # 会产生 2x2=4 个测试用例
```

---

## Fixture 机制

### @pytest.fixture 定义

```python
import pytest

@pytest.fixture
def sample_data():
    """提供测试数据"""
    return {"name": "Tom", "age": 25}

def test_user_name(sample_data):
    assert sample_data["name"] == "Tom"
```

### 作用域（scope）

| 作用域 | 生命周期 | 说明 |
|---|---|---|
| `function`（默认） | 每个测试函数 | 每个测试都重新创建 |
| `class` | 每个测试类 | 类中所有方法共享 |
| `module` | 每个模块 | 模块内所有测试共享 |
| `session` | 整个测试会话 | 所有测试共享，适合数据库连接等 |

```python
@pytest.fixture(scope="module")
def db_connection():
    conn = create_connection()
    yield conn        # yield 之前的代码是 setup
    conn.close()      # yield 之后的代码是 teardown

def test_query(db_connection):
    result = db_connection.execute("SELECT 1")
    assert result is not None
```

### conftest.py（共享 fixture）

```python
# conftest.py — 自动被同目录及子目录的测试发现
import pytest

@pytest.fixture
def base_url():
    return "https://api.example.com"

@pytest.fixture
def auth_token():
    return "Bearer test-token-123"
```

> **conftest.py 规则**：
> - 不需要 import，Pytest 自动发现
> - 作用域为 conftest.py 所在目录及其子目录
> - 可以有多个层级的 conftest.py（就近原则）

### fixture 依赖注入

Fixture 可以依赖其他 fixture，Pytest 自动解析依赖关系：

```python
@pytest.fixture
def base_url():
    return "https://api.example.com"

@pytest.fixture
def auth_headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}"}

@pytest.fixture
def api_client(base_url, auth_headers):
    return APIClient(base_url=base_url, headers=auth_headers)

def test_get_user(api_client):
    # api_client 自动注入，已经包含 base_url 和 auth_headers
    response = api_client.get("/users/1")
    assert response.status_code == 200
```

---

## 常用标记

| 标记 | 说明 | 示例 |
|---|---|---|
| `@pytest.mark.skip` | 跳过测试 | `@pytest.mark.skip(reason="未完成")` |
| `@pytest.mark.skipif` | 条件跳过 | `@pytest.mark.skipif(sys.platform=="win32")` |
| `@pytest.mark.xfail` | 预期失败 | `@pytest.mark.xfail(reason="已知Bug")` |
| `@pytest.mark.parametrize` | 参数化 | `@pytest.mark.parametrize("a,b", [(1,2)])` |
| `@pytest.mark.slow` | 自定义标记 | `pytest -m "not slow"` 排除慢测试 |

```python
import sys

@pytest.mark.skipif(sys.platform == "win32", reason="Linux only")
def test_linux_feature():
    assert True

@pytest.mark.xfail(reason="Bug #1234 未修复")
def test_known_bug():
    assert 1 == 2  # 预期失败，不会导致测试失败
```

---

## 插件生态

| 插件 | 用途 | 安装 |
|---|---|---|
| pytest-html | 生成 HTML 测试报告 | `pip install pytest-html` |
| pytest-cov | 测试覆盖率 | `pip install pytest-cov` |
| pytest-mock | Mock 支持（封装 unittest.mock） | `pip install pytest-mock` |
| pytest-xdist | 并行执行测试 | `pip install pytest-xdist` |
| pytest-asyncio | 异步测试支持 | `pip install pytest-asyncio` |
| pytest-rerunfailures | 失败重跑 | `pip install pytest-rerunfailures` |
| allure-pytest | Allure 报告 | `pip install allure-pytest` |

常用命令：
```bash
# 生成 HTML 报告
pytest --html=report.html

# 覆盖率报告
pytest --cov=myapp --cov-report=html

# 并行执行（4个进程）
pytest -n 4

# 失败重跑2次
pytest --reruns 2

# 只运行上次失败的用例
pytest --lf

# 详细输出
pytest -v

# 打印 print 输出
pytest -s
```

---

## 面试高频

### Fixture vs setup/teardown

| 对比维度 | Fixture | setup/teardown |
|---|---|---|
| 定义方式 | `@pytest.fixture` | `setUp()` / `tearDown()` 方法 |
| 灵活性 | 高（按需注入、依赖嵌套） | 低（每个测试都执行） |
| 作用域 | function/class/module/session | 仅 function/class 级别 |
| 复用性 | 可跨文件共享（conftest.py） | 仅当前类 |
| 命名 | 自定义名称 | 固定方法名 |
| 多 fixture | 支持多个 fixture 组合 | 只有一个 setup/teardown |
| 推荐 | Pytest 推荐方式 | unittest 方式 |

```python
# fixture 方式（推荐）
@pytest.fixture
def clean_db():
    db = create_db()
    yield db
    db.drop_all()

def test_insert(clean_db):
    clean_db.insert({"name": "test"})
    assert clean_db.count() == 1

# setup/teardown 方式（传统）
class TestDB:
    def setup_method(self):
        self.db = create_db()

    def teardown_method(self):
        self.db.drop_all()

    def test_insert(self):
        self.db.insert({"name": "test"})
        assert self.db.count() == 1
```

### 如何做接口自动化测试

```python
import pytest
import requests

@pytest.fixture(scope="session")
def base_url():
    return "https://api.example.com"

@pytest.fixture(scope="session")
def auth_token(base_url):
    resp = requests.post(f"{base_url}/login", json={
        "username": "admin",
        "password": "123456"
    })
    return resp.json()["token"]

@pytest.fixture
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}"}

class TestUserAPI:
    def test_get_user(self, base_url, headers):
        resp = requests.get(f"{base_url}/users/1", headers=headers)
        assert resp.status_code == 200
        assert resp.json()["id"] == 1

    def test_create_user(self, base_url, headers):
        resp = requests.post(f"{base_url}/users", headers=headers, json={
            "name": "New User",
            "email": "new@example.com"
        })
        assert resp.status_code == 201

    @pytest.mark.parametrize("user_id, expected_status", [
        (1, 200),
        (999, 404),
    ])
    def test_get_user_status(self, base_url, headers, user_id, expected_status):
        resp = requests.get(f"{base_url}/users/{user_id}", headers=headers)
        assert resp.status_code == expected_status
```

---

## 一句话总结

Pytest 是 Python 事实标准的测试框架，核心优势是简洁的 assert 断言、灵活的 fixture 机制（支持依赖注入和多级作用域）、参数化驱动、丰富插件生态，面试高频考点为 fixture 与 setup/teardown 的区别、conftest.py 共享机制、以及接口自动化测试实践。
