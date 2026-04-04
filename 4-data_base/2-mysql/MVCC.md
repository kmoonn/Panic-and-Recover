# MVCC 多版本并发控制

**InnoDB** 使用 MVCC 来提高并发性能：
- 每行数据都有隐藏字段
  - `trx_id`
  - `roll_pointer`
- 通过 **Undo Log** 保存历史版本
- 普通 `SELECT` 是**快照读**
- `SELECT ... FOR UPDATE / LOCK IN SHARE MODE` 是**当前读**