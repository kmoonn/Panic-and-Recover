---
tags:
  - Vue
  - 前端
category: 前端
---

# Vue 核心概念

## Q：Vue 是什么？

Vue 是一个**渐进式 JavaScript 框架**，用于构建用户界面。"渐进式"意味着可以从一个轻量级核心库逐步引入路由（Vue Router）、状态管理（Pinia/Vuex）、构建工具（Vite）等，按需使用，不需要一次性引入全部功能。

---

## 响应式原理

### Vue2：Object.defineProperty

通过 `Object.defineProperty` 对数据对象的属性进行**数据劫持**，在 getter 中收集依赖，在 setter 中触发更新。

```javascript
// Vue2 响应式简化原理
Object.defineProperty(obj, 'name', {
  get() {
    // 收集依赖
    dep.depend()
    return value
  },
  set(newVal) {
    value = newVal
    // 通知更新
    dep.notify()
  }
})
```

**局限性**：
- 无法检测**属性新增**（需用 `Vue.set()` 或 `this.$set()`）
- 无法检测**数组索引直接赋值**（`arr[0] = newVal` 不触发更新）
- 无法检测**数组长度修改**（`arr.length = newLen` 不触发更新）

### Vue3：Proxy

通过 `Proxy` 代理整个对象，拦截所有操作（读取、赋值、删除、枚举等）。

```javascript
// Vue3 响应式简化原理
const proxy = new Proxy(target, {
  get(target, key) {
    track(target, key)   // 收集依赖
    return target[key]
  },
  set(target, key, value) {
    target[key] = value
    trigger(target, key) // 触发更新
    return true
  },
  deleteProperty(target, key) {
    delete target[key]
    trigger(target, key) // 触发更新
    return true
  }
})
```

**优势**：
- 可以检测**属性新增**
- 可以检测**数组索引变化**
- 可以检测**属性删除**
- 性能更好（惰性响应式，只在访问时才做代理）

| 对比维度 | Vue2（defineProperty） | Vue3（Proxy） |
|---|---|---|
| 检测属性新增 | 不能 | 能 |
| 检测数组索引变化 | 不能 | 能 |
| 检测属性删除 | 不能 | 能 |
| 代理方式 | 逐个属性劫持 | 代理整个对象 |
| 兼容性 | IE9+ | 不支持 IE |

---

## 虚拟 DOM 与 Diff 算法

### 为什么需要虚拟 DOM？

- 虚拟 DOM 是用 JavaScript 对象描述真实 DOM 的轻量副本
- 直接操作真实 DOM 开销大（触发重排/重绘），虚拟 DOM 通过 Diff 算法计算出最小变更，再批量更新真实 DOM
- 跨平台能力：虚拟 DOM 可以渲染到不同平台（浏览器 DOM、Native、SSR）

### 同层比较策略

Diff 算法的核心原则：

1. **同层比较**：只比较同一层级的节点，不跨层比较
2. **类型相同才复用**：不同类型的节点直接替换，不做递归比较
3. **key 的作用**：通过 key 标识节点身份，帮助 Diff 算法识别可复用的节点

### key 的作用

```html
<!-- 没有 key 时，Diff 可能错误复用节点 -->
<div v-for="item in list">{{ item.name }}</div>

<!-- 有 key 时，Diff 能精确识别每个节点 -->
<div v-for="item in list" :key="item.id">{{ item.name }}</div>
```

| 情况 | 无 key | 有 key |
|---|---|---|
| 列表头部插入 | 所有节点更新（就地复用策略） | 仅新增一个节点 |
| 列表排序 | 可能错误复用 | 精确移动对应节点 |
| 性能 | 最差情况 O(n^2) | 最优路径 O(n) |

> **面试要点**：不要用 index 做 key，因为当列表发生变化时 index 对应的内容变了，key 失去标识意义，会导致错误的复用和状态错乱。

---

## 生命周期钩子

### Vue3 Composition API vs Vue2 Options API

| 阶段 | Vue2 Options API | Vue3 Composition API |
|---|---|---|
| 创建前 | `beforeCreate` | `setup()`（替代 beforeCreate + created） |
| 创建后 | `created` | `setup()` |
| 挂载前 | `beforeMount` | `onBeforeMount` |
| 挂载后 | `mounted` | `onMounted` |
| 更新前 | `beforeUpdate` | `onBeforeUpdate` |
| 更新后 | `updated` | `onUpdated` |
| 卸载前 | `beforeDestroy` | `onBeforeUnmount` |
| 卸载后 | `destroyed` | `onUnmounted` |
| 激活 | `activated` | `onActivated` |
| 停用 | `deactivated` | `onDeactivated` |
| 错误捕获 | `errorCaptured` | `onErrorCaptured` |

```javascript
// Vue3 Composition API 示例
import { ref, onMounted, onUnmounted } from 'vue'

export default {
  setup() {
    const count = ref(0)

    onMounted(() => {
      console.log('组件已挂载')
    })

    onUnmounted(() => {
      console.log('组件已卸载')
    })

    return { count }
  }
}
```

---

## 组件通信方式

| 方式 | 方向 | 说明 | 适用场景 |
|---|---|---|---|
| props / emit | 父↔子 | 父传子用 props，子传父用 emit 触发事件 | 父子组件 |
| provide / inject | 祖→后 | 祖先组件提供数据，后代组件注入 | 跨层级传递（无需逐层 props） |
| EventBus / mitt | 任意 | 全局事件总线，发布-订阅模式 | 兄弟组件、跨组件（简单场景） |
| Vuex / Pinia | 任意 | 全局状态管理 | 中大型应用、多组件共享状态 |
| $refs | 父→子 | 直接访问子组件实例 | 需要调用子组件方法 |

```javascript
// props / emit
// 父组件
<Child :msg="message" @update="handleUpdate" />

// 子组件
props: ['msg'],
emits: ['update'],
methods: {
  update() { this.$emit('update', newValue) }
}

// provide / inject
// 祖先组件
provide: { theme: 'dark' }
// 后代组件
inject: ['theme']

// Pinia（Vue3 推荐）
import { defineStore } from 'pinia'
export const useUserStore = defineStore('user', {
  state: () => ({ name: 'Tom' }),
  getters: { fullName: (state) => state.name },
  actions: { setName(name) { this.name = name } }
})
```

---

## 计算属性(computed) vs 侦听器(watch)

| 对比维度 | computed | watch |
|---|---|---|
| 用途 | 根据依赖数据**计算出新值** | 监听数据变化**执行副作用** |
| 返回值 | 必须有返回值 | 不需要返回值 |
| 缓存 | 有缓存（依赖不变不重新计算） | 无缓存 |
| 副作用 | 不应有副作用（不要在 computed 中发请求） | 适合副作用（异步请求、DOM 操作） |
| 适用场景 | 格式化、过滤、派生状态 | 接口请求、路由跳转、localStorage |

```javascript
// computed 示例
const fullName = computed(() => firstName.value + ' ' + lastName.value)

// watch 示例
watch(userId, async (newId) => {
  const data = await fetchUserInfo(newId)
  userInfo.value = data
})

// watchEffect 示例（自动追踪依赖）
watchEffect(() => {
  console.log(`用户ID变为: ${userId.value}`)
})
```

---

## Vue2 vs Vue3 核心区别

| 对比维度 | Vue2 | Vue3 |
|---|---|---|
| 响应式 | Object.defineProperty | Proxy |
| API 风格 | Options API（data/methods/computed） | Composition API（setup/ref/reactive） |
| 生命周期 | beforeCreate/created/... | onMounted/onUnmounted/... |
| 状态管理 | Vuex | Pinia（推荐） |
| 组件根节点 | 单根节点 | 支持多根节点（Fragment） |
| Teleport | 无 | 有（传送门，将组件渲染到 DOM 其他位置） |
| Suspense | 无 | 有（异步组件加载状态） |
| TypeScript | 支持较弱 | 原生支持 |
| Tree-shaking | 较差 | 更好（按需引入） |
| 性能 | — | 虚拟 DOM 重写，性能提升约 1.3-2 倍 |
| 模板指令 | v-model 单个 | 支持 v-model 多个（v-model:title） |

---

## Vue 与 React 的对比

| 对比维度 | Vue | React |
|---|---|---|
| 响应式 | 自动响应式（Proxy/defineProperty） | 手动 setState/状态更新函数 |
| 模板 | HTML 模板 + 指令（v-if/v-for） | JSX（JavaScript 中写 HTML） |
| 状态管理 | Pinia / Vuex | Redux / Zustand / Jotai |
| 组件风格 | 单文件组件（.vue，template/script/style） | JSX/TSX（.jsx/.tsx） |
| 学习曲线 | 较低（模板接近 HTML） | 较高（JSX + 函数式思维） |
| 生态 | 官方维护（Vue Router/Pinia/Vite） | 社区驱动（React Router/Redux） |
| 适用场景 | 中小型应用、快速开发 | 大型应用、跨平台（React Native） |
| 更新粒度 | 组件级精确更新 | 组件级（需 memo 优化） |

> **核心差异**：Vue 是**响应式驱动**，数据变了自动更新视图；React 是** setState 驱动**，必须手动触发更新。Vue 的心智模型更简单，React 更灵活但需要更多手动优化。

---

## 八股速记

**Q: Vue：MVVM / 响应式 / 组件（了解级）**

- **是什么**：渐进式**前端框架**，核心是**数据驱动视图**——你改数据，视图自动更新，不用手动操作 DOM。
- **MVVM**：Model（数据）↔ ViewModel（Vue 实例，做双向绑定）↔ View（模板）。核心是 **`v-model` 双向绑定**、`{{ }}` 插值、`v-if`/`v-for` 指令。
- **响应式原理**：Vue 2 用 `Object.defineProperty` 劫持属性、**Vue 3 改用 Proxy**（能监听新增属性/数组索引，性能更好）——数据变了自动通知视图重渲染。
- **组件化**：页面拆成可复用组件，父子通过 **props 下传 / emit 事件上传** 通信；复杂状态用 Pinia/Vuex 管。

**⭐ 加分/易错**：
- **Vue 2 vs 3 的响应式差异**（`defineProperty` → `Proxy`）是高频点，能说出"Vue 3 用 Proxy 解决了 Vue 2 无法监听新增属性/数组下标的问题"即加分。
- 测开视角：懂前端组件和数据流，才知道 **UI 自动化该定位什么、异步渲染为何 flaky**（呼应 UI自动化测试 + `kuaishou/03-kuaichat-ui.md`）。
- 🔲 前端深度按实际，别硬吹虚拟 DOM diff/编译优化等，除非真的了解。

## 一句话总结

Vue 是渐进式 JavaScript 框架，核心考点为响应式原理（Vue2 defineProperty vs Vue3 Proxy）、虚拟 DOM 与 Diff 算法（同层比较+key的作用）、组件通信（props/emit/provide/inject/Pinia）、computed vs watch，以及 Vue3 相对 Vue2 的核心改进（Composition API/Proxy/Fragment/Suspense）。
