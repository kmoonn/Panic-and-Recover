# 🐛 Pyhton读写Excel选择哪个第三方库的最佳实践

## 📅 日期与标签

- 记录日期：2025-04-10
- 标签：`#python` `#excel`

## 1. 🐞 问题现象
日常工作中我们经常会使用python处理复杂的excel数据，常常涉及到excel的读取和写入，特别是一些复杂场景比如多个sheet的读取、追加写入。python有许多库可以用于处理excel表格，目前我所知道的就有pandas、openpyxl、xlsxwriter、xlrd、xlwt等等。

库太多，每个库的函数方法又不一样，导致我每次都要百度一下，有没有一种可以几乎满足全部日常需求的库，操作还简单，函数方法也好记，这样以后就不用每次都打开浏览器搜索了。
## 2. 🔍 查找资料
主流Python Excel处理库的特点如下：
### pandas
优势：高级数据操作，适合数据分析场景

劣势：底层依赖其他库，对Excel格式控制较弱
### openpyxl
优势：全面支持.xlsx，读写能力强

劣势：不支持.xls格式
### xlsxwriter
优势：强大的写入功能，丰富的格式控制

劣势：只能写入不能读取
## xlrd/xlwt
优势：支持旧版.xls格式

劣势：xlrd 2.0+不再支持.xls，xlwt仅支持写入
## 3. 🛠 解决方案
推荐组合方案
### 日常处理需求
直接使用pandas！
```python
import pandas as pd

# 读取Excel（自动处理多sheet）
df_dict = pd.read_excel("input.xlsx", sheet_name=None)

# 处理数据...

# 写入Excel（多sheet）
with pd.ExcelWriter("output.xlsx") as writer:
    for sheet_name, df in df_dict.items():
        df.to_excel(writer, sheet_name=sheet_name, index=False)

# 写入结构数据需要建立数据模型
data = {
    'Name': ['Alice', 'Bob'],
    'Age': [25, 30],
    'Department': ['HR', 'IT']
}
df = pd.DataFrame(data)

df.to_excel('output.xlsx', index=False)
```
### 需要精细化控制格式
选择使用openpyxl
```python
from openpyxl import load_workbook

# 读取
wb = load_workbook("input.xlsx")
sheet = wb["Sheet1"]

# 处理数据...

# 追加写入
sheet.append(["新数据1", "新数据2"])
wb.save("output.xlsx")
```

## 4. 💡 经验总结
### 优先使用pandas：满足90%日常需求，API简洁易记

pd.read_excel() 记住这一个读取函数

df.to_excel() 记住这一个写入函数

```python
# 快速查看所有sheet名
pd.ExcelFile("file.xlsx").sheet_names

# 追加数据到现有文件
with pd.ExcelWriter("exist.xlsx", mode="a") as writer:
    df.to_excel(writer, sheet_name="NewSheet")

# 处理Nan值（空值、缺失值）
keep_default_na=False
```
    以后就苦练pandas啦！
