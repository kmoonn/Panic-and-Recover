# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository overview

Panic-and-Recover is a **documentation-only interview knowledge base**: ~420 Markdown notes and ~70 README index files, mainly in Chinese, covering Java backend, testing/SDET, AI/LLM, databases, OS/network, middleware, and adjacent topics.

There is **no application runtime, package manager, build system, or test framework** in this repo. Most work here is content authoring, restructuring, deduplication, renaming, and README maintenance.

The repository name comes from Go's `panic` / `recover`: it is meant as a "from panic to recovery" knowledge base for interview prep and tech-note recall.

## Common commands

Because this is a docs repo, the most important "build/test" commands are validation scripts and search commands.

### Search and inspect content

```bash
# Find notes by keyword
rg -n "线程池|拒绝策略" 01-java 10-testing

# List all markdown files
rg --files -g '*.md'

# Review only markdown changes
git diff -- '*.md'
```

### Validate one README after editing it

Use this as the closest equivalent to a **single test** in this repository:

```bash
python3 -c "
import re, os
from urllib.parse import unquote
readme = 'path/to/README.md'
dir = os.path.dirname(readme)
with open(readme) as f:
    links = re.findall(r'\]\(([^)]+)\)', f.read())
for link in links:
    if link.startswith('http') or link.startswith('#'):
        continue
    target = os.path.join(dir, unquote(link))
    if not os.path.isfile(target):
        print(f'BROKEN: {readme} → {link}')
"
```

### Validate all README links repo-wide

```bash
find . -name "README.md" -not -path "./.git/*" -not -path "./.claude/*" -not -path "./.ttadk/*" | while read r; do
  python3 -c "
import re, os; from urllib.parse import unquote
d=os.path.dirname('$r')
links=re.findall(r'\]\(([^)]+)\)', open('$r').read())
[print(f'BROKEN: $r → {l}') for l in links if not l.startswith(('http','#')) and not os.path.isfile(os.path.join(d,unquote(l)))]
"
done
```

### Check numbering consistency inside a directory

```bash
python3 - <<'PY'
from pathlib import Path
import re

d = Path('01-java/2-并发')  # replace with the directory you changed
files = [p.name for p in d.glob('*.md') if p.name != 'README.md']
numbered = [f for f in files if re.match(r'^\d+-', f)]
unnumbered = [f for f in files if not re.match(r'^\d+-', f)]
print('numbered:', numbered)
print('unnumbered:', unnumbered)
PY
```

If you made structural changes, these validation commands are the effective test suite for this repo.

## Big-picture architecture

Think of the repository as a **3-layer documentation graph**:

1. **Root `README.md`** — the landing page listing top-level topic directories.
2. **Topic `README.md` files** — curated indexes for each topic and subtopic directory.
3. **Leaf note files** — one Markdown file per interview question / knowledge point.

Future edits should preserve that graph: if you add, rename, move, merge, or delete a note, update the relevant README indexes so navigation stays intact.

### Content model

Most leaf notes follow a consistent shape:

- optional YAML frontmatter such as `tags` and `category` (present in ~75% of notes)
- a title matching the file topic
- focused explanation for exactly one topic
- often Q&A-style sections, tables, code blocks, or ASCII diagrams
- a closing `## 一句话总结` (present in ~60% of notes — optional but encouraged)

This repository is optimized for **atomic notes**, not long handbook chapters. If a topic grows large, split it into multiple files instead of creating a monolith.

### Content style: interview-first

This is an **interview prep** knowledge base, not a textbook. When writing or expanding notes:

- **Concise over exhaustive** — hit the key points an interviewer expects, not a deep dive into every edge case
- **Q&A / 对比表 / 流程图** over long prose — interviewers test recall of core concepts, not reading endurance
- **"为什么"比"怎么用"重要** — interviewers dig into reasoning (e.g., "WAL 为什么快?" > "WAL 怎么配置")
- **一句话总结 at the top** — the TL;DR that sticks in memory; detailed sections support it, not replace it
- **Avoid textbook-level detail** — no need for source code walkthroughs, parameter tuning guides, or exhaustive flag lists unless specifically asked
- **No implementation code** — notes should not contain full code examples; use tables, flow diagrams, and interface signatures to convey structure. Pseudocode or 3-line snippets are acceptable only when they uniquely illustrate a concept that prose cannot
- **Cross-reference existing notes** — if a concept (e.g., fsync, LSN, 脏页) is already covered elsewhere, link to it rather than re-explaining inline
- **Multi-language examples** — don't limit examples to Java/Spring; include Python, Go, middleware, and other ecosystems when relevant (e.g., Python @decorator for Decorator pattern, Django Middleware for Chain of Responsibility)

### Taxonomy pattern

The content is organized primarily by numbered top-level topic directories (currently 18 directories, `00`–`19` with gaps):

- `00-written-test/` — 笔试
- `01-java/` — Java
- `02-computer-network/` — 计算机网络
- `03-operating-system/` — 操作系统
- `04-database/` — 数据库
- `05-spring/` — Spring
- `06-security/` — 安全
- `08-python/` — Python
- `09-ai/` — AI/LLM
- `10-testing/` — 测试
- `11-containerization/` — 容器化
- `12-frontend/` — 前端
- `13-design-pattern/` — 设计模式
- `14-interview/` — 面试
- `15-dev-tools/` — 开发工具
- `16-golang/` — Go语言
- `17-miniprogram/` — 微信小程序
- `18-api-design/` — API设计
- `19-middleware/` — 中间件

Those topic directories often have numbered Chinese subdirectories for second-level grouping, for example:

- Java → 基础 / 集合 / 并发 / JVM
- Written test → Algo / SQL
- Database → MySQL / Redis / MongoDB
- AI → 基础 / Agent / RAG / MCP / Skills / Prompt / 其他
- Testing → 基础 / 场景题 / 接口测试 / 性能测试 / 框架 / Midscene
- Computer network → 基础 / 应用层 / 传输层 / 网络层 / 数据链路层 / 面试热点
- Spring → 核心 / SpringBoot / SpringCloud / 事务
- Containerization → Docker / K8s
- Dev tools → Maven / MyBatis
- Middleware → 消息队列

Some subdirectories start from `0-` (e.g., `01-java/0-基础`, `02-computer-network/0-基础`) while most start from `1-`. Both exist in the wild; match whichever convention the surrounding directory uses.

When deciding where a new note belongs, follow the existing topic taxonomy instead of creating a new bucket too early.

### README files are part of the architecture

README files here are not decorative. They are the repository's **manual navigation layer**.

- Top-level READMEs summarize a topic and link downward.
- Nested READMEs act as local tables of contents.
- They should list **non-empty content files only**.
- Broken README links are one of the highest-value failures to check for after edits.

### PROMPT.md defines ingestion behavior

`PROMPT.md` is not just a note; it defines the intended behavior when converting raw interview material into this knowledge base:

- extract only what is present in the source material
- keep the prescribed taxonomy
- merge duplicate or highly similar questions
- output standardized Q&A-style content
- do not invent additional questions or answers

If you are reorganizing or consolidating interview notes, follow `PROMPT.md` as the normalization contract.

### Special content types

- `14-interview/2-牛客面经/` uses **date-stamped records** (`YYYYMMDD.md`) rather than numbered topic files.
- Comparison notes are expected to live alongside single-topic notes, e.g. both `TCP.md`-style files and `TCP与UDP的区别.md`-style files can coexist.
- The repository is mostly consistent about numbering, but some legacy pockets may still need cleanup. Treat mixed numbering as debt to fix, not as the pattern to copy.

## Naming conventions

These naming rules are important because the repo is navigation-heavy and link-heavy.

### Top-level directories

- Use **two-digit numeric prefixes + English names**.
- Use **hyphens**, not underscores.
- Preserve proper-noun capitalization such as `MySQL`, `Redis`, `MongoDB`, `SpringBoot`, `SpringCloud`, `Midscene`.

### Subdirectories

- Use **numeric prefixes + Chinese names**.
- Match the surrounding language convention: English at the top level, Chinese below it.

### Files

- **One file, one topic**.
- If the directory uses numbering, use `1-xxx.md`, not `1. xxx.md`.
- Use concise topic names.
- Use `与` for two-item comparisons and `、` for multi-item enumerations.
- **Never use `和`** in comparison filenames.
- No spaces.
- No underscores.
- Avoid parentheses in filenames; if they are unavoidable, README links must use URL encoding.
- No `@` symbols in filenames.
- Do not add redundant suffixes like `关键字` when the topic name alone is enough.

## When adding or reorganizing content

1. Put the note in the most specific existing topic directory that fits.
2. Match the directory's numbering scheme.
3. Keep each file focused on a single interview question or knowledge point.
4. Add or update the nearest `README.md` index once the file has real content.
5. If you merge duplicates, fold the smaller note into the stronger one, then delete the redundant file and fix every README that referenced it.
6. Keep comparison summaries alongside detailed individual-topic notes when both are useful.
7. For raw interview-material cleanup, deduplicate aggressively but do **not** invent missing details.

## Verification checklist

After renames, moves, splits, merges, or deletions, verify:

1. **Broken links** — every README link resolves. The "Validate all README links repo-wide" command above is the primary check; run it before committing structural changes.
2. **Numbering consistency** — a directory should not mix numbered and unnumbered note files unless you are intentionally cleaning up existing debt.
3. **Naming rules** — no `和` in comparisons, no underscores, no `1.` prefixes, no spaces, no `@` symbols, no unnecessary `关键字` suffixes.
4. **README coverage** — every meaningful non-empty note should appear in an appropriate README index.

If there is no code to run, a change is only really finished once the README graph and naming consistency still hold.

## Language convention

The repository is bilingual by layer:

- **Top-level directory names**: English (e.g., `01-java`, `09-ai`, `19-middleware`)
- **Subdirectory names**: Chinese (e.g., `0-基础`, `2-并发`, `1-消息队列`)
- **File names**: Chinese topic names (e.g., `HashSet.md`, `线程池.md`, `TCP与UDP的区别.md`)
- **File content**: Chinese, with English technical terms inline (e.g., "JVM 的 **GC Root** 包括……")
- **README files**: Chinese descriptions and headings

Match the surrounding language when adding new content.