# Vitta ecommerce — Spec

## Vision

An online store for **Vitta**, a lingerie and accessories brand that currently sells through Instagram and WhatsApp. The goal is to give customers a browsable catalog and a simple way to put together an order, while keeping the final sale on WhatsApp, where Vitta already works today.

## Users

- **Customer**: browses the catalog, picks products (size, quantity) and builds her order.
- **Administrator**: manages the catalog (products, categories, images, prices and stock).

## v1 scope

- **Public catalog**
  - Product listing by category.
  - Product detail with photos, price, sizes and stock.
- **Cart**: add, update and remove items; persists while the customer browses.
- **Checkout via WhatsApp**: the order is sent to Vitta as a pre-filled WhatsApp message (products, sizes, quantities, total). No online payment.
- **Admin panel** with JWT login:
  - Create, edit and delete products.
  - Manage categories, images, prices and stock.
- **Deploy**: frontend on Vercel; backend and MySQL in the cloud.

## Out of v1

Explicitly excluded to avoid scope creep. Anything here needs a spec update before work starts.

- Online payments (Mercado Pago).
- Customer accounts.
- Shipping cost calculation.
- Coupons and discounts.
- Reviews.
- Multiple administrators.

## Architecture

Monorepo:

- **`/backend`**: Spring Boot 4.1.2, Java 21, Maven wrapper, MySQL 8, Flyway, Spring Data JPA, Bean Validation. REST API consumed by the frontend.
- **`/frontend`**: Next.js 16.4.6 (App Router, `src/`, alias `@/*`), React 19, TypeScript, Tailwind CSS 4, ESLint 9, npm, Node 22.

```
Customer / Admin ──> Next.js (Vercel) ──REST──> Spring Boot API (cloud) ──> MySQL 8 (cloud)
Customer checkout ──> WhatsApp link with the pre-filled order (built on the frontend)
```

## Technical decisions

- **Java 21 + Spring Boot 4.1**: Spring Boot 3.x no longer has OSS support, so the project starts on the current generation.
- **Flyway with `ddl-auto: validate`**: the schema is owned by versioned migrations; Hibernate only validates it. Merged migrations are never edited; changes go in a new `V{n}__...`.
- **Testcontainers**: integration tests run against a real MySQL 8 container via `@ServiceConnection`, independent of local credentials or databases.
- **CI with a single aggregate check**: one GitHub Actions workflow with path filters (backend/frontend). The `ci` job aggregates results (skipped jobs count as OK) and is the only required status check.
- **Ruleset on `main`**: no direct pushes; changes land through PRs that must pass `ci`, reviewed and merged by the owner.
- **Public repo, no credentials**: secrets live in gitignored `.env` files; only `.env.example` templates are committed.

## Domain model

TBD in Mission 1.

## API

TBD in Mission 1.

## Changelog

- **2026-11-10**: Mission 0 completed (monorepo setup, backend and frontend scaffolding, CLAUDE.md, CI with Testcontainers, spec and PR template).
