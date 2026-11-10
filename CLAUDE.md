# Vitta ecommerce

Ecommerce for Vitta (lingerie and accessories). Monorepo:

- `/backend` — Spring Boot 4.1.2, Java 21, Maven wrapper, MySQL 8, Flyway, JPA.
- `/frontend` — Next.js (App Router) + TypeScript + Tailwind. Not created yet.

## Backend commands (run from `/backend`)

- `./mvnw spring-boot:run` — run the app.
- `./mvnw verify` — build and run tests.
- Credentials live in `backend/.env` (gitignored); template in `backend/.env.example`.

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
