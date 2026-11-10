# Workflow

## Principle: pick the tool by the kind of work

| Kind of work | Tool |
| --- | --- |
| A repeatable procedure | Skill |
| Judgment that needs a clean context | Subagent |
| Something that must always happen | Hook / CI |

## Roadmap

When closing each mission: review this file and check off what has been adopted.

- **M0**
  - [x] `CLAUDE.md`
  - [ ] Plan mode on every mission
- [ ] **M1–M2**: skills `/nueva-mision` and `/cerrar-mision` + backend conventions skill.
- [ ] **M3**: `code-reviewer` subagent (SOLID, logic in controllers, validations, tests, credentials).
- [ ] **M4**: post-edit format/lint hook + block edits to `.env`.
- [ ] **Deploy**: automatic CD with GitHub Actions on merge to `main` (Render + Vercel) + deploy-verifier subagent (migrations, env vars, breaking API changes).
- [ ] **Backend**: ArchUnit architecture tests in CI.
- [ ] **Final**: package everything as our own plugin.
