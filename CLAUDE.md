# Vitta ecommerce

Ecommerce for Vitta (lingerie and accessories). Monorepo:

- `/backend` — Spring Boot 4.1.2, Java 21, Maven wrapper, MySQL 8, Flyway, JPA.
- `/frontend` — Next.js 16.4.6 (App Router, `src/`, alias `@/*`), React 19, TypeScript, Tailwind CSS 4, ESLint 9, npm. Node 22.

## Backend commands (run from `/backend`)

- `./mvnw spring-boot:run` — run the app.
- `./mvnw verify` — build and run tests. **Requires Docker running**: integration tests start a MySQL 8 container via Testcontainers (`TestcontainersConfiguration` + `@ServiceConnection`), so they don't use `backend/.env` or a local MySQL.
- `./mvnw -B -DskipTests verify` — compile everything (including tests) without Docker.
- Credentials live in `backend/.env` (gitignored); template in `backend/.env.example`.

## Frontend commands (run from `/frontend`)

- `npm install` — install dependencies.
- `npm run dev` — dev server on http://localhost:3000.
- `npm run lint` — ESLint.
- `npm run typecheck` — `tsc --noEmit`.
- `npm run build` — production build.
- Env vars live in `frontend/.env.local` (gitignored); template in `frontend/.env.example` (`NEXT_PUBLIC_API_URL`).
- Next.js 16 has breaking changes vs. older versions: check `frontend/AGENTS.md` and `node_modules/next/dist/docs/` before writing code.

## CI

- Single workflow `.github/workflows/ci.yml`, runs on PRs to `main` and pushes to `main`.
- `changes` (dorny/paths-filter) decides what runs: `backend/**` → `backend` job, `frontend/**` → `frontend` job; changes in `.github/workflows/**` trigger both.
- `backend`: Temurin 21 + Maven cache, `./mvnw -B verify` (Testcontainers works on the GitHub runner's Docker).
- `frontend`: Node 22 + npm cache, `npm ci`, `lint`, `typecheck`, `build`.
- `ci`: aggregate gate, the only required check on `main`. Fails if any job failed or was cancelled; skipped jobs count as OK.

## Workflow

- One branch per mission: `feat/`, `fix/`, `chore/`, `docs/`, `ci/`.
- Commits in English, Conventional Commits.
- Open a PR with `gh pr create`. Never merge or push to `main`: the owner reviews and merges.

## Rules

- Never read, modify or commit `.env`.
- Never edit an already-merged Flyway migration; always add a new `V{n}__...`.
- `spring.jpa.hibernate.ddl-auto` stays `validate`.
- No business logic in controllers.
- Validate inputs with Bean Validation.
- Apply SOLID pragmatically.

## Communication

- Talk to the user in Spanish.
- Code, commits and PRs in English.
