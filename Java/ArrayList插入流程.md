# ArrayList 插入流程

主要依赖于**动态数组**的**扩容机制**和**元素移动机制**。

插入元素有两种方法：
- add(E e) 默认插入列表末尾
- add(int index, E element) 在指定位置插入

## 尾部添加

```java
public boolean add(E e) {
    modCount++;
    add(e, elementData, size);
    return true;
}

private void add(E e, Object[] elementData, int s) {
        if (s == elementData.length)
            elementData = grow();
        elementData[s] = e;
        size = s + 1;
}
private Object[] grow() {
    return grow(size + 1);
}
private Object[] grow(int minCapacity) {
    int oldCapacity = elementData.length;
    if (oldCapacity > 0 || elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        int newCapacity = ArraysSupport.newLength(oldCapacity,
                minCapacity - oldCapacity, /* minimum growth */
                oldCapacity >> 1           /* preferred growth */);
        return elementData = Arrays.copyOf(elementData, newCapacity);
    } else {
        return elementData = new Object[Math.max(DEFAULT_CAPACITY, minCapacity)];
    }
}
```

## 指定位置插入

```java
public void add(int index, E element) {
        // 索引越界检查
        rangeCheckForAdd(index);
        modCount++;
        final int s;
        Object[] elementData;
        // 扩容机制
        if ((s = size) == (elementData = this.elementData).length)
            elementData = grow();
        // 移动元素（复制数组）
        System.arraycopy(elementData, index,
                         elementData, index + 1,
                         s - index);
        // 插入元素
        elementData[index] = element;
        size = s + 1;
}
```