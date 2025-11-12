# LinkedList

## 数据结构

- 双向链表，每个节点都包含自身数据 + 前后节点的引用，内存开销大
- 有序存储
- 允许重复元素和存储`null`值
- 不支持快速随机访问，需要从头/尾遍历

## 特点

- **非线程安全**

在多线程环境中使用需要手动同步，可以使用`Collections.synchronizedList(new LinkedList<>())`包装。

- **实现 Deque 接口**

**双端队列**，可以作为**队列或者栈**使用，通过调用`offer()`、`poll()`、`push()`、`pop()`方法。

```java
// 栈用法
Deque<String> stack = new LinkedList<>();
stack.push();
stack.pop();
stack.peek();

// 队列用法
Deque<String> queue = new LinkedList<>();
queue.offer();
queue.poll();
queue.peek();
```

