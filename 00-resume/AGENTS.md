# AGENTS.md

## Project Overview

This repository is a private interview-preparation knowledge base for autumn campus recruiting, focused on test development. It is not an application codebase: the working artifacts are Markdown notes plus one LaTeX resume source. The main architecture is a curated content system whose central consistency problem is keeping the resume bullets, deep-dive notes, and index pages aligned.

Primary entry points:

- `README.md`: global index, completion board, resume bullet summary, directory conventions, and red-line rules.
- `resume.tex`: authoritative in-repo resume source. It uses `moderncv`/LaTeX and references images through `../assets/...`. PDF rendering is done outside this repository.
- `resume-notes/`: experience and project deep-dive notes. Internship folders (`bytedance/`, `kuaishou/`, `iflytek/`) contain one note per resume project; `bytedance/00-context.md` is shared internship context. Project folders use index-plus-subnotes: `ceec/00-rag-platform.md` plus five RAG subnotes, and `whut/00-contest-system.md` plus three contest-system subnotes. `education/00-background.md` is a lightweight quick-reference card.
- `fundamentals/`: eight card-style fundamentals files in the fixed README order (`01` language+testing, `02` network, `03` os/linux, `04` database, `05` frameworks, `06` middleware, `07` ai, `08` design-patterns). `02-network.md` is the style template.
- `job-targets/`: target-role prep, organized by company then stage: `<company>/written-exam/` and `<company>/interview/`. Currently `baidu/interview/01-ai-test-dev-jd.md` (JD-to-knowledge-base gap analysis) plus `baidu/written-exam/` (`01-overview.md` exam structure, `02-coding-solutions.md` and `03-coding-dp.md` coding solutions).

Important current facts:

- ByteDance SDT means `Spec-Driven Testing`; it is the shared method behind the three ByteDance projects.
- The education-background national innovation project is the WHUT contest management system; link to `resume-notes/whut/` instead of duplicating technical detail.
- For the CEEC RAG project, the original landed project was Java/Tiny-Ragent with cloud APIs; the resume describes the Python/Nidus local-model rewrite. The locked resume stack is `Python + LangGraph + LangChain + FastAPI + PostgreSQL + Qdrant + Ollama`, while deep-dive notes may still explain Redis and React 18 because the resume stack line is intentionally compressed.

## Build & Commands

There is no application build, package manager, runtime service, or automated test suite in this repository. Normal work is Markdown/LaTeX editing plus consistency checks.

Useful commands:

```bash
# Inspect note size distribution.
wc -l README.md resume.tex resume-notes/*/*.md fundamentals/*.md job-targets/**/*.md

# Check that Markdown does not expose local absolute paths or usernames.
rg -n '/''Users/' --glob "*.md" .

# Check sensitive technology drift in the RAG notes and resume.
rg -n "Qdrant|RocketMQ|1024|1536" README.md resume.tex resume-notes fundamentals

# List relative Markdown links for spot-checking index/subnote references.
rg -o '\]\(\.?\./?[a-z0-9/._-]+\.md\)|\]\([a-z0-9/._-]+\.md\)' README.md resume-notes fundamentals job-targets

# Confirm this workspace is a git worktree when needed.
git status --short
```

Do not attempt to render `resume.tex` inside this repo unless an external LaTeX/moderncv environment is explicitly provided. The repository currently has local Trae configuration in `.trae/traecli.toml`; it defines Trae hooks and MCP servers, not project build commands.

## Code Style

Here, "code style" means content structure and naming.

- Write Chinese-first notes with English technical terms preserved.
- Keep file names lowercase with two-digit numeric prefixes:
  - `resume-notes/<company>/<NN>-<short-name>.md`
  - project/background index files use `00-...`
  - `fundamentals/<NN>-<topic>.md` follows README's fixed topic order
  - `job-targets/<company>/{written-exam,interview}/<NN>-<short-name>.md`
- Do not manually normalize Chinese/English spacing in prose; `resume.tex` handles resume-side autospace.
- Mark uncertain facts with `🔲` or explicit "待确认". Do not upgrade uncertain facts to confirmed claims.

Internship deep-dive notes should follow the established template:

- File-head blockquote with `用途`, `使用方式`, and `真实性纪律`.
- Sections in this order where applicable: `0. 一句话定位`, `0.5 我的真实工作量 & 边界`, `1. 60 秒总述稿（STAR）`, `1.5 端到端主流程`, deep-dive `Q...`, `总结卡`, `代码对照表`, `TODO / 待确认`.
- Deep-dive answers use the local three-part style: `大白话`, `打磨版答法`, and `加分锚点`, with follow-up plans for risky questions.

Project notes use index-plus-subnotes:

- The `00-...` index owns project-level STAR, main flow, selection tradeoffs, business/testing/digital questions, summary card, main resume-code mapping table, and subnote navigation.
- Subnotes own one technical point only. They should not repeat project-level resume bullets or maintain another full locked resume version.
- Link subnotes with same-directory relative links such as `./01-rag-retrieval.md`; avoid stale `Q1/Q2/Q3` anchor references.

Fundamentals files use card style:

- Start with a quick navigation table.
- Each card uses `## Qn. Title` plus concise bullet answers and `加分/易错`.
- End with a test-development perspective, summary card, and TODO section.
- When adding a card, update the navigation table, card body, and summary card together.

## Testing

Testing means content verification and interview rehearsal, not unit tests.

- Resume consistency: when a resume bullet changes, synchronize all authoritative locations: `resume.tex`, the README resume bullet summary, and the relevant project index's main mapping table. Subnotes should not carry full locked resume text.
- Link consistency: check README-to-note, index-to-subnote, and cross-folder back-links such as `education` to `whut` and `bytedance/00-context.md` to ByteDance project notes.
- Terminology consistency: preserve confirmed terms such as `SDT = Spec-Driven Testing`, `bge-m3` dimension `1024`, and CEEC `Qdrant`; do not reintroduce removed or obsolete terms such as `RocketMQ` or vector dimension `1536` unless explicitly documenting historical drift.
- Interview rehearsal: use each note's 60-second STAR answer, then follow deep-dive Q&A and summary-card red lines to check whether the answer can withstand follow-up questions.

## Security

This repository contains private job-search material, real personal contact information in `resume.tex`, and company/project-specific internal context. Treat it as sensitive.

- Do not publish or copy repository content to public channels.
- Do not add secrets, tokens, `.env` files, or credentials. No project runtime secrets are expected.
- Markdown files must not contain absolute local paths, usernames, or machine-specific paths. Use relative paths or placeholders. `resume.tex` may use `../assets/...` image references.
- Do not invent technical details. If a mechanism, metric, deployment shape, or ownership boundary is not confirmed, mark it as pending or state the uncertainty.
- Quantitative claims such as `40%`, `90%+`, `1000+`, `62%→87%`, and similar values must remain framed as experience/reporting/self-test estimates unless the note explicitly records a stricter measurement basis.
- Keep ownership boundaries honest: team projects should say which parts were personally owned; reused internal platforms should be described as reuse plus orchestration, not as self-built infrastructure.

## Configuration

- `README.md` is the global operating index: update its completion board and resume bullet summary when adding or changing notes.
- `resume.tex` is the authoritative in-repo resume source. Rendering is external; do not add build assumptions to this repository. Note it is listed in `.gitignore`, so it is a local-only working copy and is not committed — treat it as the working SSOT while editing, but do not assume it is version-controlled.
- `.gitignore` deliberately excludes local agent tooling and the sensitive resume from version control: `.trae/`, `.claude/`, `.mcp.json`, `ai-plugin.json`, `ai-plugin-lock.json`, `resume.tex`, `.ttadk/`, and `specs/doc_export/`. The tracked content is effectively the Markdown knowledge base plus `README.md` and `AGENTS.md`.
- `.trae/traecli.toml` and the root `.mcp.json` both configure MCP servers for Lark Docs and Tika (via `bunx` against an internal registry); `.trae/traecli.toml` also registers a `UserPromptSubmit` tracking hook. These are agent/tooling configuration, not application dependencies.
- `.claude/` holds a parallel set of local agent tooling (TTADK skills under `.claude/skills/`, plus a `claude-track` hook); like `.trae/` it is gitignored agent config, not project code.
- `ai-plugin.json` declares the TTADK common plugin package (`ttadk/common`), and `ai-plugin-lock.json` is its lock companion; both are gitignored. Treat them as local agent tooling metadata unless the user explicitly asks to change plugin setup.
- There is no `.cursor/rules/`, `.github/copilot-instructions.md`, or `.trae/rules/` project rule set in the current workspace. If one is added later, fold only project-relevant rules into this file.
