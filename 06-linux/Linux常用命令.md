---
tags:
  - 操作系统
category: 操作系统
---

# Linux 常用命令

## Q：Linux 面试中有哪些高频命令？

Linux 命令是后端开发和运维的基础技能，面试常考命令操作和场景题。以下按类别整理。

---

## 一、文件操作

| 命令 | 说明 | 常用示例 |
|---|---|---|
| `ls` / `ll` | 列出目录内容 | `ls -la` 显示隐藏文件和详细信息 |
| `cd` | 切换目录 | `cd ~` 回到主目录，`cd -` 返回上一目录 |
| `pwd` | 显示当前目录 | `pwd` |
| `cp` | 复制文件/目录 | `cp -r dir1 dir2` 递归复制目录 |
| `mv` | 移动/重命名 | `mv old.txt new.txt` |
| `rm` | 删除文件/目录 | `rm -rf dir/` 强制递归删除（慎用！） |
| `find` | 查找文件 | `find / -name "*.log" -mtime +7` 查找7天前的日志 |
| `locate` | 快速定位文件（基于数据库） | `locate nginx.conf` |
| `cat` | 查看完整文件 | `cat file.txt` |
| `head` / `tail` | 查看文件头/尾 | `tail -n 100 file.log`，`tail -f file.log` 实时追踪 |
| `more` / `less` | 分页查看 | `less +F file.log` 类似 tail -f |
| `chmod` | 修改权限 | `chmod 755 script.sh`，`chmod +x script.sh` |
| `chown` | 修改属主 | `chown user:group file.txt` |
| `ln` | 创建链接 | `ln -s target link` 创建软链接 |

---

## 二、文本处理

| 命令 | 说明 | 常用示例 |
|---|---|---|
| `grep` | 文本搜索 | `grep -rn "error" /var/log/` 递归搜索，显示行号 |
| `awk` | 列级文本处理 | `awk '{print $1, $3}' file.txt` 打印第1、3列 |
| `sed` | 流编辑器 | `sed -i 's/old/new/g' file.txt` 替换文本 |
| `sort` | 排序 | `sort -k2 -n file.txt` 按第2列数值排序 |
| `uniq` | 去重 | `sort file.txt \| uniq -c` 排序后统计重复次数 |
| `wc` | 统计行/词/字节数 | `wc -l file.txt` 统计行数 |
| `cut` | 按列截取 | `cut -d: -f1 /etc/passwd` 按冒号分割取第1列 |
| `tr` | 字符替换/删除 | `tr 'A-Z' 'a-z' < file.txt` 大写转小写 |
| `diff` | 比较文件差异 | `diff file1.txt file2.txt` |
| `tee` | 从标准输入读、写出到文件和标准输出 | `ls \| tee output.txt` |

> **组合使用示例**：统计 access.log 中各 IP 的访问次数并取 Top10：
> ```bash
> awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -10
> ```

---

## 三、进程管理

| 命令 | 说明 | 常用示例 |
|---|---|---|
| `ps` | 查看进程 | `ps -ef` 查看所有进程，`ps aux` 详细信息 |
| `top` / `htop` | 实时进程监控 | `top -p 1234` 监控指定进程 |
| `kill` | 终止进程 | `kill -9 1234` 强制终止，`kill -15 1234` 优雅终止 |
| `nohup` | 后台运行（不受终端关闭影响） | `nohup java -jar app.jar &` |
| `&` | 放入后台执行 | `python script.py &` |
| `jobs` | 查看当前终端后台任务 | `jobs -l` |
| `fg` / `bg` | 将任务调到前台/后台 | `fg %1` 将任务1调到前台 |

---

## 四、磁盘与内存

| 命令 | 说明 | 常用示例 |
|---|---|---|
| `df` | 查看磁盘使用 | `df -h` 人类可读格式 |
| `du` | 查看目录/文件大小 | `du -sh *` 查看当前目录各文件大小 |
| `free` | 查看内存使用 | `free -h`，`free -m` 以MB显示 |
| `fdisk` | 磁盘分区 | `fdisk -l` 查看分区信息 |
| `mount` / `umount` | 挂载/卸载文件系统 | `mount /dev/sdb1 /mnt/data` |

---

## 五、网络相关

| 命令 | 说明 | 常用示例 |
|---|---|---|
| `ifconfig` / `ip` | 查看/配置网络接口 | `ip addr`，`ifconfig` |
| `ping` | 测试网络连通性 | `ping -c 4 baidu.com` |
| `curl` | HTTP 请求工具 | `curl -X GET https://api.example.com` |
| `wget` | 下载文件 | `wget https://example.com/file.tar.gz` |
| `nslookup` / `dig` | DNS 查询 | `dig baidu.com`，`nslookup baidu.com` |
| `telnet` / `nc` | 测试端口连通性 | `nc -zv 192.168.1.1 8080` |
| `tcpdump` | 抓包分析 | `tcpdump -i eth0 port 80 -nn` |

---

## 六、压缩解压

| 命令 | 说明 | 常用示例 |
|---|---|---|
| `tar` | 打包/解包 | `tar -czf archive.tar.gz dir/` 打包压缩，`tar -xzf archive.tar.gz` 解压 |
| `gzip` | 压缩/解压(.gz) | `gzip file.txt` 压缩，`gzip -d file.txt.gz` 解压 |
| `zip` / `unzip` | ZIP 压缩/解压 | `zip -r archive.zip dir/`，`unzip archive.zip` |

> **tar 常用组合**：
> ```bash
> tar -czf app.tar.gz app/     # 打包+gzip压缩
> tar -xzf app.tar.gz          # 解压gzip包
> tar -cjf app.tar.bz2 app/   # 打包+bzip2压缩（更小但更慢）
> tar -cJf app.tar.xz app/    # 打包+xz压缩（最小但最慢）
> ```

---

## 七、系统信息

| 命令 | 说明 | 常用示例 |
|---|---|---|
| `uname` | 系统信息 | `uname -a` 查看内核版本等全部信息 |
| `uptime` | 系统运行时间与负载 | `uptime` 显示运行时间、在线用户、负载均值 |
| `hostname` | 主机名 | `hostname`，`hostname -I` 显示IP |
| `who` / `w` | 查看登录用户 | `w` 更详细，显示用户正在执行的命令 |
| `last` | 用户登录记录 | `last -n 10` 最近10条 |
| `dmesg` | 内核日志 | `dmesg \| grep -i error` 查找内核错误 |

---

## 八、面试高频场景题

### 1. 如何查看某个端口被哪个进程占用？

```bash
# 方法一：lsof
lsof -i :8080

# 方法二：netstat
netstat -tunlp | grep 8080

# 方法三：ss（更快，推荐）
ss -tunlp | grep 8080
```

### 2. 如何实时查看日志？

```bash
# 实时追踪日志末尾
tail -f /var/log/app.log

# 实时追踪 + 过滤关键词
tail -f /var/log/app.log | grep "ERROR"

# less 也可以实时追踪（按 Ctrl+C 暂停，按 F 继续）
less +F /var/log/app.log
```

### 3. 如何查找大文件？

```bash
# 查找大于100M的文件
find / -type f -size +100M -exec ls -lh {} \;

# 查看当前目录下各子目录大小
du -sh * | sort -rh | head -10

# 查看根分区占用
df -h
```

### 4. 如何统计文件中某关键词出现次数？

```bash
# 方法一：grep -c
grep -c "ERROR" app.log

# 方法二：grep + wc
grep "ERROR" app.log | wc -l

# 忽略大小写
grep -ic "error" app.log

# 多文件统计
grep -c "ERROR" *.log
```

### 5. 如何查看系统负载？

```bash
# uptime：查看1分钟/5分钟/15分钟负载均值
uptime
# 输出示例：load average: 0.5, 0.3, 0.1

# top：实时监控CPU、内存、进程
top

# 关键指标解读：
# load average 三个数分别代表 1/5/15 分钟的平均负载
# 数值 < CPU核数 表示系统空闲
# 数值 = CPU核数 表示系统满载
# 数值 > CPU核数 表示系统过载

# 查看 CPU 核数
nproc
```

---

## 八股速记

**问：Linux 排查命令手册？**

**答（按场景分类）**：
- **进程**：`ps -ef | grep xxx`、`top`/`htop`、`pstree`、`kill -9 pid`。
- **CPU/负载**：`top`（看 %CPU、load average）、`vmstat 1`、`mpstat`。
- **内存**：`free -h`、`top` 看 RES、`cat /proc/meminfo`。
- **磁盘**：`df -h`（分区使用）、`du -sh *`（目录大小）、`iostat -x 1`（IO）。
- **网络**：`netstat -anp`/`ss -antp`（连接/端口）、`ping`、`curl -v`、`tcpdump`（抓包）、`telnet host port`（测通）。
- **日志/文本**：`tail -f`、`grep`、`awk`、`sed`、`less`、`wc -l`。
- **文件查找**：`find / -name xxx`、`which`、`lsof -i:8080`（看端口被谁占）、`lsof -p pid`（进程开的文件）。

**⭐ 加分**：`grep 'ERROR' app.log | wc -l` 数错误、`awk '{print $1}' access.log | sort | uniq -c | sort -rn` 统计 IP 访问量——测开日志分析常用组合拳。

## 一句话总结

Linux 命令是后端面试必考项，重点掌握文本处理三剑客（grep/awk/sed）、进程管理（ps/top/kill）、网络排查（netstat/ss/lsof/tcpdump），以及高频场景题（查端口、看日志、找大文件、统计关键词、看负载）。
