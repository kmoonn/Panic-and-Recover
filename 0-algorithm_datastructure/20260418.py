# 输入字符串 123，将数字按顺序任意分割、用加号相连求和，统计总和是质数的方案数。

def is_prime(num):
    if num < 2:
        return False
    for i in range(2, int(num**0.5)+1):
        if num % i == 0:
            return False
    return True

s = input().strip()
res = 0

def dfs(pos, total):
    global res
    if pos == len(s):
        if is_prime(total):
            res += 1
        return
    for i in range(pos, len(s)):
        num = int(s[pos:i+1])
        dfs(i+1, total + num)

dfs(0, 0)
print(res)