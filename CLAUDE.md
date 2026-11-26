# Vitta ecommerce

Ecommerce for Vitta (lingerie and accessories). Monorepo:

- `/backend` — Spring Boot 4.1.2, Java 21, Maven wrapper, MySQL 8, Flyway, JPA.
- `/frontend` — Next.js 16.4.6 (App Router, `src/`, alias `@/*`), React 19, TypeScript, Tailwind CSS 4, ESLint 9, npm. Node 22.

## Backend commands (run from `/backend`)

- `./mvnw spring-boot:run` — run the app.
- `./mvnw verify` — build and run tests. **Requires Docker running**: integration tests start a MySQL 8 container via Testcontainers (`TestcontainersConfiguration` + `@ServiceConnection`), so they don't use `backend/.env` or a local MySQL.
- `./mvnw -B -DskipTests verify` — compile everything (including tests) without Docker.
- `./mvnw -B verify "-Dtest=!*IntegrationTest,!BackendApplicationTests,!*RepositoryTest,!*MigrationTest" -Dsurefire.failIfNoSpecifiedTests=false` — run the tests that don't need Docker (unit + `@WebMvcTest` slices, including security).
- Credentials live in `backend/.env` (gitignored); template in `backend/.env.example`. Variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE`, `FRONTEND_URL`, `JWT_SECRET` (≥ 32 bytes, required outside `dev`), `JWT_EXPIRATION` (default `8h`), `ADMIN_EMAIL`/`ADMIN_PASSWORD` (optional admin bootstrap), `CLOUDINARY_URL` (optional; if set, product images go to Cloudinary) and `UPLOADS_DIR` (local image storage when there is no Cloudinary, default `./uploads`, gitignored). The `dev` profile has placeholder defaults for the JWT/admin ones; tests use a test-only secret in `src/test/resources/config/application.yaml`.
- `PORT` (default `8080`) sets the HTTP port.
- Production: `prod` profile (`application-prod.yaml`, `SPRING_PROFILES_ACTIVE=prod`) for Render + TiDB Cloud + Cloudinary (see `docs/deploy.md`: TiDB ignores CHECK constraints, validation stays in the backend). It takes the datasource only from env vars, runs only `db/migration` (never the dev seed), requires `CLOUDINARY_URL` and `FRONTEND_URL`, and fails if combined with `dev`. Docker image: `backend/Dockerfile` (no tests in the image build) + `.dockerignore` (keeps `.env` out). Runbook, env vars and local image test commands: `docs/deploy.md`.
- Security: `common/security/SecurityConfig` (stateless JWT, deny by default, CORS). New `@WebMvcTest` slices must `@Import(SecurityConfig.class)` and new public endpoints must be added to its allow-list.

## Frontend commands (run from `/frontend`)

- `npm install` — install dependencies.
- `npm run dev` — dev server on http://localhost:3000.
- `npm run lint` — ESLint.
- `npm run typecheck` — `tsc --noEmit`.
- `npm run build` — production build.
- Env vars live in `frontend/.env.local` (gitignored); template in `frontend/.env.example` (`BACKEND_URL` server-only, `NEXT_PUBLIC_WHATSAPP_NUMBER`). `NEXT_PUBLIC_*` values are inlined at build time.
- Production: Netlify (`netlify.toml` at the repo root, with `@netlify/plugin-nextjs` declared explicitly); `NEXT_PUBLIC_WHATSAPP_NUMBER` is baked in at build time, so changing it needs a redeploy. Details and troubleshooting: `docs/deploy.md`.
- Next.js 16 has breaking changes vs. older versions: check `frontend/AGENTS.md` and `node_modules/next/dist/docs/` before writing code.

## CI

- Single workflow `.github/workflows/ci.yml`, runs on PRs to `main` and pushes to `main`.
- `changes` (dorny/paths-filter) decides what runs: `backend/**` → `backend` job, `frontend/**` → `frontend` job; changes in `.github/workflows/**` trigger both.
- `backend`: Temurin 21 + Maven cache, `./mvnw -B verify` (Testcontainers works on the GitHub runner's Docker), then `docker build` of the backend image (validation only, no push).
- `frontend`: Node 22 + npm cache, `npm ci`, `lint`, `typecheck`, `build`.
- `ci`: aggregate gate, the only required check on `main`. Fails if any job failed or was cancelled; skipped jobs count as OK.

## Workflow

- One branch per mission: `feat/`, `fix/`, `chore/`, `docs/`, `ci/`.
- Commits in English, Conventional Commits.
- Open a PR with `gh pr create`. Never merge or push to `main`: the owner reviews and merges.
- When closing each mission/sub-mission, deliver a close-out summary with: what was done, files/areas touched, the PR link and the status of the `ci` check.

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
