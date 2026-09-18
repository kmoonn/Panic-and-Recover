# AGENTS.md

## Project Overview

**Panic-and-Recover** is a **documentation-only interview knowledge base** — roughly 460 Markdown notes and ~60 README index files, mostly in Chinese, covering Java backend, testing/SDET, AI/LLM, databases, OS/network, middleware, Go, and adjacent topics. The name comes from Go's `panic`/`recover`: a "from panic to recovery" note base for interview prep and tech recall.

There is **no application runtime, package manager, build system, or automated test framework**. Work here is content authoring, restructuring, deduplication, renaming, and README maintenance.

Think of the repo as a **3-layer documentation graph**:

1. **Root `README.md`** — landing page listing the top-level topic directories.
2. **Topic `README.md` files** — curated indexes for each topic/subtopic directory; the manual navigation layer.
3. **Leaf note files** — one Markdown file per interview question / knowledge point.

Any add / rename / move / merge / delete of a note **must** update the relevant README indexes so navigation stays intact. Broken README links are the highest-value failure to check for.

### Taxonomy

Content is organized by two-digit numbered top-level directories (`00`–`15` plus `other/`): `00-resume`, `01-written`, `02-python`, `03-java`, `04-network`, `05-os`, `06-linux`, `07-database`, `08-spring`, `09-middleware`, `10-testing`, `11-frontend`, `12-security`, `13-ai`, `14-design-pattern`, `15-golang`, `other`.

Top-level dirs often have numbered Chinese subdirectories (e.g., `03-java/2-并发`, `04-network/2-传输层`, `07-database/2-MySQL/2-索引`). Some subdirs start at `0-`, most at `1-`; **match whichever convention the surrounding directory uses**. Follow the existing taxonomy instead of creating a new bucket too early.

### Sub-project

`00-resume/` is the 秋招备战 (autumn recruiting) prep project and has **its own `00-resume/AGENTS.md`** with distinct rules (resume consistency, LaTeX `resume.tex`, sensitivity constraints). Defer to it when working under `00-resume/`.

## Build & Commands

No build/package/runtime. The closest thing to a "test suite" is link and consistency validation. Run these before committing structural changes.

```bash
# Find notes by keyword
rg -n "线程池|拒绝策略" 03-java 10-testing

# List all markdown files
rg --files -g '*.md'

# Review only markdown changes
git diff -- '*.md'
```

**Validate all README links repo-wide** (primary check after any structural edit):

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

**Validate a single README** after editing it:

```bash
python3 -c "
import re, os
from urllib.parse import unquote
readme = 'path/to/README.md'
d = os.path.dirname(readme)
links = re.findall(r'\]\(([^)]+)\)', open(readme).read())
[print(f'BROKEN: {l}') for l in links if not l.startswith(('http','#')) and not os.path.isfile(os.path.join(d, unquote(l)))]
"
```

**Check numbering consistency inside a directory:**

```bash
python3 - <<'PY'
from pathlib import Path
import re
d = Path('03-java/2-并发')  # replace with the directory you changed
files = [p.name for p in d.glob('*.md') if p.name != 'README.md']
print('numbered:', [f for f in files if re.match(r'^\d+-', f)])
print('unnumbered:', [f for f in files if not re.match(r'^\d+-', f)])
PY
```

If there is no code to run, a change is only finished once the README graph and naming consistency still hold.

## Code Style

Here "code style" means **content structure and naming**, since this is a docs repo.

### Content model (leaf notes)

Most notes follow a consistent shape:

- optional YAML frontmatter with `tags` and `category` (present in ~80% of notes — 378 of ~460 files)
- an H1 title matching the file topic (exactly one; do not duplicate the H1)
- focused explanation of **exactly one** topic
- often Q&A sections, comparison tables (`| ... |`), or ASCII flow, over long prose
- a closing `## 一句话总结` TL;DR (present in ~60% of notes — optional but encouraged)

### Content style: interview-first (not a textbook)

- **Concise over exhaustive** — hit the points an interviewer expects, not every edge case.
- **"为什么" > "怎么用"** — interviewers dig into reasoning (e.g., "WAL 为什么快" over "WAL 怎么配置").
- **一句话总结 up top / at the end** — the memorable TL;DR; detailed sections support it.
- **No full implementation code** — use tables, flow diagrams, interface signatures. Pseudocode or ≤3-line snippets only when they uniquely illustrate a concept.
- **Cross-reference existing notes** instead of re-explaining a covered concept inline (e.g., link to fsync/LSN/脏页 notes).
- **Multi-language examples** — don't limit to Java/Spring; include Python, Go, middleware when relevant.
- **Practice-grounded** — connect to real use cases or the user's project experience in `00-resume/` (e.g., Observer → FastExcel AnalysisEventListener). Dry theory with no "where is this used" anchor is a signal to rework.

### Atomic note rule

One file = one interview question / one concept / one comparison. Do not bundle distinct topics under vague names like "深入" or "进阶"; split into clearly named files (e.g., `StreamableHTTP.md`, `Sampling.md`, not `MCP深入.md`). If a topic grows large, split rather than build a monolith.

### Naming conventions (navigation- and link-heavy repo)

**Top-level directories:** two-digit numeric prefix + English name, hyphens (not underscores). Preserve proper-noun casing: `MySQL`, `Redis`, `MongoDB`, `SpringBoot`, `SpringCloud`, `Midscene`.

**Subdirectories:** numeric prefix + Chinese name (English at top level, Chinese below).

**Files:**

- One file, one topic. **No numeric prefix** on note files — the topic name is the identifier (`Transformer架构.md`, not `5-Transformer架构.md`).
- **No standalone `xxx介绍.md`** — the directory `README.md` is the overview.
- Use `与` for two-item comparisons, `、` for multi-item enumerations. **Never use `和`** in comparison filenames.
- No spaces, no underscores, no `@`, no `1.`-style prefixes. Avoid parentheses (if unavoidable, README links must URL-encode).
- No redundant suffixes like `关键字` when the topic name suffices.

### Language convention (bilingual by layer)

- Top-level dir names: **English**. Subdir names: **Chinese**. File names: **Chinese** topic names.
- File content: **Chinese** with English technical terms inline (e.g., "JVM 的 **GC Root** 包括……").
- README files: Chinese descriptions and headings.

Match the surrounding language when adding content.

## Testing

Testing here means **content verification**, not unit tests. After renames, moves, splits, merges, or deletions, verify:

1. **Broken links** — every README link resolves. Run the repo-wide link validator above before committing structural changes.
2. **Numbering consistency** — a directory should not mix numbered and unnumbered note files, unless intentionally cleaning up existing debt.
3. **Naming rules** — no `和` in comparisons, no underscores, no `1.` prefixes, no spaces, no `@`, no unnecessary `关键字` suffixes.
4. **README coverage** — every meaningful non-empty note appears in an appropriate README index; READMEs should list **non-empty content files only**.
5. **No duplicate H1** — each note has exactly one H1 title (a recurring defect has been duplicated adjacent `# Title` lines).

## Security

- This is study/interview material; `00-resume/` additionally contains **private job-search material and real personal information** — treat it as sensitive and never publish it externally. See `00-resume/AGENTS.md` for its stricter rules.
- **Do not add secrets, tokens, `.env` files, or credentials.** No project runtime secrets are expected.
- Markdown files must not contain absolute local paths, usernames, or machine-specific paths — use relative paths or placeholders.
- **Do not invent technical details.** For raw interview-material cleanup, deduplicate aggressively but never fabricate missing details, questions, or answers. If a mechanism/metric/boundary is unconfirmed, mark it pending rather than upgrading it to a confirmed claim.

## Configuration

- **`CLAUDE.md`** (root) is the authoritative long-form guidance for this repo; this file is its distilled summary. Keep them consistent.
- **`README.md`** is the global navigation index — keep the top-level directory table current when adding/removing topic areas.
- **`PROMPT.md`** — if present, it is the normalization contract for converting raw interview material into this base (extract only what's in source, keep the taxonomy, merge duplicates, output standardized Q&A, invent nothing). There is currently no root `PROMPT.md`.
- **Agent tooling is gitignored, not project code**: `.gitignore` excludes `.idea`, `.vscode`, `.ttadk/`, `specs/doc_export/`, `.claude/`, `.DS_Store`, `ai-plugin.json`, `ai-plugin-lock.json`, `.mcp.json`. The tracked content is effectively the Markdown knowledge base plus `README.md`, `CLAUDE.md`, and `AGENTS.md` files.
- **`.mcp.json`** configures MCP servers (`lark-docs`, `tika`) via `bunx` against an internal ByteDance registry — this is agent/tooling config, not an application dependency. `.claude/` holds local TTADK skills and hooks (also gitignored).
- There is **no** `.cursor/rules/`, `.github/copilot-instructions.md`, or `.trae/rules/` in this workspace. If one is added later, fold only project-relevant rules into this file.

## When adding or reorganizing content

1. Put the note in the most specific existing topic directory that fits.
2. Match the directory's numbering scheme.
3. Keep each file focused on a single question / knowledge point.
4. Add or update the nearest `README.md` index once the file has real content.
5. When merging duplicates, fold the smaller note into the stronger one, delete the redundant file, and fix every README that referenced it.
6. Keep comparison summaries (`TCP与UDP的区别.md`) alongside detailed single-topic notes (`TCP.md`) when both are useful.
