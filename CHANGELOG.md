# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Classify by prompting (no install)** — new one-page guide ([docs/guides/classify-by-prompting.md](docs/guides/classify-by-prompting.md) + PDF, and site page `/classify-by-prompting`) with a complete, drop-in prompt that makes any agentic CLI (Copilot CLI, Claude Code, Gemini, Cursor) emit a **full AiCMM Agent Card** for a locally-built agent — identity, avatar, the 12-dimension capability fingerprint, the 7 governance rules, and the derived Agency Qualification barometer (level, code, 0–100 Agency Index, needle) — aligned to `schemas/agent-card.schema.json`. No clone or build required.
- **FAA (Floating Agentic Assistance)** — page-aware floating assistant on every site page; **Alt+A** toggles it (Esc closes when unpinned).
- GitHub Copilot is driven through the official **`@github/copilot-sdk`** (JSON-RPC) via a Node bridge (`aicmm-site/faa-bridge/`), replacing fragile `copilot -p` shell-out — fixes Windows argv quote-mangling and stops the per-command browser pop.
- **Page Action Protocol** — the assistant can fill forms (animated, character-by-character typing with focus moving field to field), reword visible text, and reload after a saved source edit (` ```aicmm-fill ` / ` ```aicmm-edit ` / ` ```aicmm-reload `).
- **Assistant Engine** settings — provider selection, model picker, capability-aware generation tuning, and a power-user-mode toggle that reveals Develop & Extend; persisted to `~/.aicmm/faa-settings.json`.
- **Contribution integrity gate** — `scripts/run-foundational-tests.ps1` plus `GovernanceRulesTest` and `FrameworkInvariantsTest` lock the 7 governance rules, agent threshold, Agency ladder (−2..+5), and 12-dimension structure before any PR.
- One-command **secret-takeover restart** (`scripts/restart-aicmm.ps1`) that shuts down, rebuilds, and relaunches the site.

### Fixed
- FAA auto-fill left the Create Card **Category** dropdown blank when the model returned a value outside `digital/embodied/hybrid`; select fills now match case-insensitively (with a loose fallback) and keep the default option instead of blanking the control.
- Assistant Engine Save/Cancel and power-user toggle did nothing — the `[hidden]` attribute was overridden by `display:flex`; now authoritative inside the panel.
- FAA no longer opens a browser tab per command (SDK runtime skips the `sessionStart` hook; `AICMM_FAA=1` also gates the launch scripts).

### Initial scaffolding
- Initial project structure with Maven multi-module setup
- Framework documentation from original articles (LinkedIn, Medium)
- Core module skeleton (`aicmm-core`) for domain models and scoring
- Inspector module skeleton (`aicmm-inspector`) for agent investigation
- CLI module skeleton (`aicmm-cli`) for command-line interface
- JSON Schema placeholder for Agent Cards
- Apache 2.0 license
- Contributing guidelines
- GitHub Actions CI workflow
- Issue templates for bugs and feature requests
