# TCP 拥塞控制

TCP 拥塞控制包含慢开始、拥塞避免、快重传、快恢复四个算法。

发送方维护拥塞窗口 cwnd 和慢开始门限 ssthresh。

慢开始阶段 cwnd 指数增长，达到 ssthresh 后进入拥塞避免，线性增长。

发生超时则 ssthresh 设为 cwnd/2，cwnd 重置为 1，重回慢开始；

收到 3 个重复 ACK （判定为轻度丢包）则执行快重传与快恢复，ssthresh=cwnd/2，cwnd 设为新阈值，进入拥塞避免。
