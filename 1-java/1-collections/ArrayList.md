# ArrayList

## 数据结构

- **数组**（初始容量默认 10）
- **动态**扩容（默认 1.5 倍）
- 允许重复元素，支持存储`null`值
- 有序存储，支持快速随机访问

## 特点

- **非线程安全**

如果在多线程环境使用，必须通过`Collections.synchronizedList(new ArrayList<>())` 进行包装，或者使用 `CopyOnWriteArrayList` 并发集合。

- **添加元素**

默认尾插法，在列表末尾添加元素。

在**指定索引位置**添加元素时，涉及到大量元素移动，会通过`Arrays.copyOf()`自动创建一个更大的新数组，并将旧数组中的所有元素**复制**到新数组中。

- **延迟初始化**
  - JDK 1.7
`new ArrayList<>()`会直接创建一个容量为 10 的 `Object` 数组。
  - JDK 1.8
`new ArrayList<>()`会将`elementData`指向`DEFAULTCAPACITY_EMPTY_ELEMENTDATA`空数组，当第一次调用`add()`方法时才会真正分配内存。

- **不同空状态**

使用**无参构造函数**时，`elementData`会被初始化为`DEFAULTCAPACITY_EMPTY_ELEMENTDATA`。

标记这个`ArrayList`是“**默认容量的空列表**”，当第一次添加元素时自动扩容到 10。

使用指定初始容量为 0 的构造函数时，`elementData`会被初始化为`EMPTY_ELEMENTDATA`。

标记这个`ArrayList`是“**用户明确要求的空列表**”，当第一次添加元素时不会自动扩容到 10，而是根据添加元素的数量来决定初始容量。

通常第一次`add()`会创建容量为 1 的数组，后续按照 1.5 倍的规则扩容。

## 源码

- 成员变量

```java
// 默认初始容量
private static final int DEFAULT_CAPACITY = 10;
// new ArrayList<>(0)
public static final Object[] EMPTY_ELEMENTDATA = {};
// new ArrayList<>()
public static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
// 存储数组元素
Object[] elementData;
// 数组元素数量
private int size;
```
- 构造方法

```java
// 空构造
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}
// 带初始容量
public ArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
        this.elementData = new Object[initialCapacity];
    } else if (initialCapacity == 0) {
        this.elementData = EMPTY_ELEMENTDATA;
    } else {
        throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
    }
}
// 从其他集合初始化
public ArrayList(Collection<? extends E> c) {
    // 将集合 c 转换为数组
    elementData = c.toArray();
    // 如果数组长度不为 0，且数组类型不是 Object[]，则复制为 Object[]
    if ((size = elementData.length) != 0) {
        if (elementData.getClass() != Object[].class)
            elementData = Arrays.copyOf(elementData, size, Object[].class);
    } else {
        // 集合 c 为空，将 elementData 设为 EMPTY_ELEMENTDATA
        this.elementData = EMPTY_ELEMENTDATA;
    }
}
```

- 关键方法

```java
public void add(int index, E e) {
    rangeCheckForAdd(index); // 校验索引是否在 [0, size] 范围内
    ensureCapacityInternal(size + 1); // 确保容量足够
    // 复制 index 及后续元素，向后移动 1 位（腾出插入位置）
    System.arraycopy(elementData, index, elementData, index + 1, size - index);
    elementData[index] = e; // 插入元素
    size++; // size 加 1
}

private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        // 无参构造的空数组，最小容量取 10 和 minCapacity 的较大值
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    ensureExplicitCapacity(minCapacity); // 触发显式扩容检查
}

private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    // 扩容公式：oldCapacity + (oldCapacity >> 1) → 1.5 倍扩容
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    // 修正新容量：如果扩容后仍小于最小容量，直接用最小容量
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    // 复制原数组元素到新数组
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```