# AGENTS.md

Android music player: local files plus self-hosted Navidrome/Subsonic and Jellyfin. Kotlin, Jetpack Compose, Media3, Hilt, Room. One module `:app`, plus `:baselineprofile`.

This repo is a fork of `PixelPlayerHQ/PixelPlayerOSS`, grown as its own app. Issues and PRs go to `srodriguezcode/PixelPlayerOSS`, never upstream. See `docs/agents/issue-tracker.md`.

## Code style

Read [docs/agents/code-style.md](docs/agents/code-style.md) before writing code. Read [app/performance_analysis.md](app/performance_analysis.md) before touching list rendering, animations or `PlayerUiState`.

## Secrets

Never commit keystores, `keystore.properties`, `local.properties`, server URLs, user names or tokens. Fixtures use made-up songs, artists and servers (`https://navidrome.example`).

## Agent skills

### Install

The flow runs on [mattpocock/skills](https://github.com/mattpocock/skills) plus the skills in `skills/`, which no installer provides (today: `/deslop-text`). Install both before you start:

```bash
claude plugin install mattpocock-skills@claude-plugins-official
ln -s "$PWD/skills/deslop-text" ~/.claude/skills/deslop-text
```

Other agents: `npx skills@latest add mattpocock/skills`, and link `skills/*` into that agent's skills folder.

### Issue tracker

Issues live as GitHub issues on the fork, managed via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Canonical names, unchanged. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` + `docs/adr/`. See `docs/agents/domain.md`.

### Human prose

Human prose = ASD-STE100 Simplified Technical English, terms from `GLOSSARY.md`, passed through `/deslop-text` silently: apply its fixes, keep its report out of the reply.

### Tickets

Applies to `/to-tickets`, on top of the skill:

- Each ticket body opens with `## Summary`: 2–4 sentences of human prose — what changes for the user, and why. The rest stays in `/writing-for-agents` style.
- Ticket zero: whatever every slice imports (Room entity and migration, string resources, preference keys, the interface a later source implements) is one ticket blocking all others. Horizontal on purpose, the second exception to vertical slicing beside expand–contract.
- Ask if tickets get worked sequentially or in parallel. If parallel: print the longest blocking chain and challenge each edge on it; chain depth is the schedule. Split hard on the critical path, merge freely off it.
- Link every ticket as a sub-issue of its spec, besides the blocked-by edges. The board groups tickets by sub-issue; without the link they show loose.

### Grilling

Applies to `/grilling` and every skill that runs it (`/grill-with-docs`, `/triage`, `/wayfinder`, `/improve-codebase-architecture`):

- Question title, body and recommended answer are human prose. Technical detail goes on a nested line after the plain version.
- On a prototype or grilling ticket, answered questions are not approval. Close it only after the user OKs the last version.
