# Vitta Ecommerce

Ecommerce for Vitta (lingerie and accessories). Monorepo:

- `/backend`: Spring Boot 4.1.2, Java 21, Maven wrapper, MySQL 8 (Flyway, JPA).
- `/frontend`: Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4, Node 22.

## Production

- Frontend: Netlify, https://vittabymarie.netlify.app
- Backend: Render (Docker), https://vitta-backend-plus.onrender.com
- Database: TiDB Cloud Starter (MySQL-compatible)
- Images: Cloudinary

## Documentation

- [`CLAUDE.md`](CLAUDE.md): commands, conventions and workflow.
- [`docs/spec.md`](docs/spec.md): product and technical specification.
- [`docs/deploy.md`](docs/deploy.md): production runbook and troubleshooting.
- [`docs/workflow.md`](docs/workflow.md): working agreements.
