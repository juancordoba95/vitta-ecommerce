# Vitta ecommerce

Ecommerce for Vitta (lingerie and accessories). Monorepo:

- `/backend` — Spring Boot 4.1.2, Java 21, Maven wrapper, MySQL 8, Flyway, JPA.
- `/frontend` — Next.js 16.4.6 (App Router, `src/`, alias `@/*`), React 19, TypeScript, Tailwind CSS 4, ESLint 9, npm. Node 22.

## Backend commands (run from `/backend`)

- `./mvnw spring-boot:run` — run the app.
- `./mvnw verify` — build and run tests.
- Credentials live in `backend/.env` (gitignored); template in `backend/.env.example`.

## Frontend commands (run from `/frontend`)

- `npm install` — install dependencies.
- `npm run dev` — dev server on http://localhost:3000.
- `npm run lint` — ESLint.
- `npm run typecheck` — `tsc --noEmit`.
- `npm run build` — production build.
- Env vars live in `frontend/.env.local` (gitignored); template in `frontend/.env.example` (`NEXT_PUBLIC_API_URL`).
- Next.js 16 has breaking changes vs. older versions: check `frontend/AGENTS.md` and `node_modules/next/dist/docs/` before writing code.

## Workflow

- One branch per mission: `feat/`, `fix/`, `chore/`, `docs/`.
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
