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

Schema owned by Flyway (`backend/src/main/resources/db/migration`), validated by Hibernate.

```
categories 1 ──< products 1 ──< product_variants
```

| Table | Columns | Constraints |
| --- | --- | --- |
| `categories` | `id`, `name` (100), `slug` (120) | `slug` unique |
| `products` | `id`, `name` (150), `slug` (170), `description` (2000, nullable), `price DECIMAL(10,2)`, `category_id`, `image_url` (500, nullable), `active` (default `true`), `created_at`, `updated_at` | `slug` unique; FK to `categories`; `price >= 0` |
| `product_variants` | `id`, `product_id`, `size` (10), `stock` (default 0) | unique `(product_id, size)`; FK to `products` (on delete cascade); `stock >= 0` |

- A product belongs to exactly one category and has one variant per size (S, M, L for now). Stock lives on the variant.
- Inactive products (`active = false`) stay in the database but are hidden from the public catalog.
- Prices are in ARS.
- **Dev data**: with the `dev` profile, Flyway also loads `db/dev-data` (a repeatable, idempotent seed with 3 categories and 8 products, one of them inactive). It never runs in other environments or in tests.

## API

Base path `/api`. Public, read-only, JSON. Slugs must match `^[a-z0-9]+(-[a-z0-9]+)*$`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/categories` | All categories, sorted by name. |
| `GET` | `/api/products?category={slug}` | Active products sorted by name; `category` is optional. |
| `GET` | `/api/products/{slug}` | Active product detail with category and variants. |

Examples:

```jsonc
// GET /api/categories
[{ "id": 1, "name": "Bodies", "slug": "bodies" }]

// GET /api/products?category=conjuntos
[{ "id": 1, "name": "Conjunto Encaje Floral", "slug": "conjunto-encaje-floral",
   "price": 42900.00, "imageUrl": "https://placehold.co/600x800?text=...", "categorySlug": "conjuntos" }]

// GET /api/products/conjunto-encaje-floral
{ "id": 1, "name": "Conjunto Encaje Floral", "slug": "conjunto-encaje-floral",
  "description": "...", "price": 42900.00, "imageUrl": "https://placehold.co/600x800?text=...",
  "category": { "id": 1, "name": "Conjuntos", "slug": "conjuntos" },
  "variants": [{ "size": "S", "stock": 4 }, { "size": "M", "stock": 7 }, { "size": "L", "stock": 3 }] }
```

**Errors** use RFC 9457 Problem Details (`application/problem+json`):

```json
{ "type": "about:blank", "title": "Resource not found", "status": 404,
  "detail": "Product 'foo' not found", "instance": "/api/products/foo" }
```

- `404`: unknown or inactive product; unknown `category` filter.
- `400`: invalid slug in the path or the `category` parameter.
- `500`: unexpected error (generic message; details are only logged).

**CORS**: `/api/**` allows `GET` from the origin in `FRONTEND_URL` (default `http://localhost:3000`).

## Changelog

- **2026-11-10**: Mission 1.1: catalog backend (schema, dev seed data, read-only catalog API).
- **2026-11-10**: Mission 0 completed (monorepo setup, backend and frontend scaffolding, CLAUDE.md, CI with Testcontainers, spec and PR template).
