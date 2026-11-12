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
                           1 ──< product_images
```

| Table | Columns | Constraints |
| --- | --- | --- |
| `categories` | `id`, `name` (100), `slug` (120) | `slug` unique |
| `products` | `id`, `name` (150), `slug` (170), `description` (2000, nullable), `price DECIMAL(10,2)`, `category_id`, `image_url` (500, nullable), `active` (default `true`), `created_at`, `updated_at` | `slug` unique; FK to `categories`; `price >= 0` |
| `product_variants` | `id`, `product_id`, `size` (10), `stock` (default 0) | unique `(product_id, size)`; FK to `products` (on delete cascade); `stock >= 0` |
| `product_images` | `id`, `product_id`, `url` (500), `position` | unique `(product_id, position)`; FK to `products` (on delete cascade); `position >= 0` |

- A product belongs to exactly one category and has one variant per size (S, M, L for now). Stock lives on the variant.
- Sizes are free text; the API sorts them logically, not alphabetically: letter sizes `XXS … XXXL` first, then numeric sizes ascending, then anything else alphabetically.
- `products.image_url` is the cover used by listings; `product_images` is the ordered gallery shown in the detail (`V2` backfilled each existing cover as position `0`).
- Inactive products (`active = false`) stay in the database but are hidden from the public catalog.
- Prices are in ARS.
- **Dev data**: with the `dev` profile, Flyway also loads `db/dev-data` (a repeatable, idempotent seed with 3 categories and 8 products, one of them inactive). It never runs in other environments or in tests.

## API

Base path `/api`. Public, JSON, no endpoint writes data. Slugs must match `^[a-z0-9]+(-[a-z0-9]+)*$`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/categories` | All categories, sorted by name. |
| `GET` | `/api/products?category={slug}` | Active products sorted by name; `category` is optional. |
| `GET` | `/api/products/{slug}` | Active product detail with category, images and variants. |
| `POST` | `/api/cart/validate` | Checks a client-side cart against current prices and stock. |

Examples:

```jsonc
// GET /api/categories
[{ "id": 1, "name": "Bodies", "slug": "bodies" }]

// GET /api/products?category=conjuntos
[{ "id": 1, "name": "Conjunto Encaje Floral", "slug": "conjunto-encaje-floral",
   "price": 42900.00, "imageUrl": "https://placehold.co/600x800?text=...", "categorySlug": "conjuntos" }]

```

### Product detail: `GET /api/products/{slug}`

```jsonc
// GET /api/products/conjunto-tul-bordado
{ "id": 2, "name": "Conjunto Tul Bordado", "slug": "conjunto-tul-bordado",
  "description": "...", "price": 39900.00, "imageUrl": "https://placehold.co/600x800?text=...",
  "category": { "id": 1, "name": "Conjuntos", "slug": "conjuntos" },
  "images": [{ "url": "https://...", "position": 0 }, { "url": "https://...", "position": 1 }],
  "variants": [{ "id": 4, "size": "S", "stock": 6, "available": true },
               { "id": 5, "size": "M", "stock": 0, "available": false },
               { "id": 6, "size": "L", "stock": 2, "available": true }] }
```

- `imageUrl` is the cover (same as in the listing); `images` is the gallery sorted by `position`.
- `variants` are sorted by logical size order; `available` is `stock > 0`. The variant `id` is what the cart sends.
- Loaded in a single query (entity graph over category, variants and images; no N+1).
- `404` if the product does not exist or is inactive; `400` if the slug is malformed.

### Cart validation: `POST /api/cart/validate`

**Decision: the cart lives on the client** (browser storage), since v1 has no customer accounts and checkout is a WhatsApp message. The server never stores carts; before showing the cart summary and before building the WhatsApp message, the frontend sends the cart here to get current prices, stock and the total. The endpoint is **read-only**: it does not reserve or decrement stock, so stock can still change between validation and the actual sale (Vitta confirms on WhatsApp).

Request:

```json
{ "items": [{ "variantId": 4, "quantity": 2 }, { "variantId": 5, "quantity": 1 }] }
```

Rules (any violation → `400` Problem Details):

- `items`: required, 1 to 50 items, no `null` entries.
- `variantId`: required, positive, no duplicates across items (merge quantities on the client).
- `quantity`: required, `1` to `999`.
- A malformed or missing JSON body is also `400`.

Response, always `200` for a valid body even if some items have problems:

```jsonc
{ "items": [
    { "variantId": 4, "productSlug": "conjunto-tul-bordado", "productName": "Conjunto Tul Bordado",
      "size": "S", "unitPrice": 39900.00, "quantity": 2, "availableStock": 6, "status": "OK" },
    { "variantId": 5, "productSlug": "conjunto-tul-bordado", "productName": "Conjunto Tul Bordado",
      "size": "M", "unitPrice": 39900.00, "quantity": 1, "availableStock": 0, "status": "OUT_OF_STOCK" },
    { "variantId": 999, "productSlug": null, "productName": null, "size": null,
      "unitPrice": null, "quantity": 1, "availableStock": null, "status": "NOT_FOUND" }
  ],
  "total": 79800.00 }
```

- `items` come back in request order, one per requested item.
- `status`: `OK` (stock ≥ quantity), `INSUFFICIENT_STOCK` (0 < stock < quantity), `OUT_OF_STOCK` (stock = 0), `NOT_FOUND` (unknown variant or inactive product; product fields, `unitPrice` and `availableStock` are `null`).
- `unitPrice` is the product's current price; `total` is `Σ unitPrice × quantity` over `OK` items only (BigDecimal, 2 decimals, ARS).
- All variants are loaded in one query.

### Errors

**Errors** use RFC 9457 Problem Details (`application/problem+json`):

```json
{ "type": "about:blank", "title": "Resource not found", "status": 404,
  "detail": "Product 'foo' not found", "instance": "/api/products/foo" }
```

- `404`: unknown or inactive product; unknown `category` filter.
- `400`: invalid slug in the path or the `category` parameter; invalid cart validation body.
- `500`: unexpected error (generic message; details are only logged).

**CORS**: `/api/**` allows `GET` and `POST` from the origin in `FRONTEND_URL` (default `http://localhost:3000`).

## Frontend

### Routes

| Route | Description |
| --- | --- |
| `/` | Hero + catalog grid; `?category={slug}` filters by category. Each card links to the product detail. |
| `/productos/{slug}` | Product detail (consumes `GET /api/products/{slug}`). |

The cart is not a route: it is a drawer available on every page from the header (see [Cart](#cart)).

### Product detail: `/productos/{slug}`

- **Layout** (mobile-first): gallery on top and info below; two columns from `md` up. A "Volver al catálogo" link sits above the content and the category name links to `/?category={slug}`.
- **Gallery**: large main image plus clickable thumbnails from `images` (sorted by `position`); falls back to `imageUrl`, then to a "Vitta" placeholder. Thumbnails only show with 2+ images.
- **Price** in ARS via `Intl.NumberFormat("es-AR")` (`formatPrice`).
- **Rendering**: the static shell (header, back link, skeleton) is prerendered; the product streams in because `params` is runtime data under Cache Components. API data is cached with `cacheLife("minutes")` and the `catalog` tag.
- **Not found**: an unknown or inactive product (API `404`) or a slug that fails the slug pattern calls `notFound()` and renders the branded `not-found.tsx` with a link back to the catalog. Because the check runs after streaming starts, the HTTP status stays `200`; Next.js adds `noindex`.
- **API down**: a friendly "No pudimos cargar este producto" message instead of an error page.
- **Metadata**: `generateMetadata` sets the title (`{name} · Vitta by Marie`), the description (truncated to 160 chars) and Open Graph / Twitter tags with the cover image. Link-preview bots (WhatsApp, Instagram, Facebook) get the tags in `<head>`: Next.js blocks on metadata for HTML-limited bots.

#### Stock states in the UI

| Variant state | Size button | After selecting it |
| --- | --- | --- |
| `stock = 0` (`available: false`) | Disabled and struck through; not selectable | — |
| `1 ≤ stock ≤ 3` | Enabled | Shows "Últimas unidades" |
| `stock > 3` | Enabled | No hint |
| No size has stock | Size picker and button replaced by "Sin stock por ahora" | — |

- The "últimas unidades" threshold is `LOW_STOCK_THRESHOLD = 3` (`frontend/src/lib/stock.ts`).
- No size is preselected and "Agregar al carrito" stays disabled until the customer picks one. Exception: if exactly one size has stock, it starts selected. The selected variant `id` is what the cart stores.
- Stock shown on the page can be a few minutes stale (cached); the cart validation endpoint is the source of truth before checkout.
- "Agregar al carrito" adds one unit of the selected size and opens the cart drawer. If the cart already holds all the known stock of that size, or already has 50 distinct items, nothing is added and an inline message explains why.

### Cart

The cart lives only in the browser (see `POST /api/cart/validate`). Code: `frontend/src/lib/cart/` (pure logic in `cart-items.ts`, store in `cart-store.ts`, server revalidation in `use-cart-validation.ts`) and `frontend/src/components/cart/`.

- **Store**: [zustand](https://github.com/pmndrs/zustand) with the `persist` middleware. Only `items` are saved, in `localStorage` under the key `vitta-cart` (version `1`); the drawer's open state is not persisted. Stored items are sanitized on load (malformed entries, duplicates and anything beyond the limits are dropped).
- **Item**: `variantId`, `productSlug`, `productName`, `size`, `imageUrl`, `unitPrice` (snapshot in ARS, for display only), `quantity` and `stock` (last known stock of the variant; `0` means not available).
- **Limits** (same as the validation endpoint):
  - At most **50 distinct items** (`MAX_CART_ITEMS`). Adding a new variant to a full cart is rejected.
  - Adding a variant already in the cart **sums the quantity** instead of duplicating it.
  - `quantity` is always between **1** and the **known stock** (and never above `999`). The `−` button stops at 1 (removing is a separate "Quitar" action) and `+` stops at the stock.
- **Hydration**: the persisted cart is only read on the client. The header badge and the drawer render it only after mounting (`useHydrated`), so the server HTML and the first client render always match.
- **Header**: a bag icon on the right opens the drawer; its badge shows the total **units** (sum of quantities), `99+` above 99, and is hidden when the cart is empty.
- **Drawer**: a modal `<dialog>` (focus trap, `Escape`, click on the backdrop closes it, page scroll locked). Full width on mobile, a right-hand panel (`max-w-md`) from `sm` up. Each line shows image, name (links to the detail), size, unit price, `−`/`+` and "Quitar". Empty state: "Tu carrito está vacío" with a link to the catalog. "Finalizar pedido" is visible but disabled until the WhatsApp checkout (Mission 3).

#### Revalidation

Every time the drawer opens, and again whenever a variant or quantity changes while it is open (debounced 300 ms; older requests are aborted), the cart is sent to `POST /api/cart/validate` and the response is applied to the stored items:

| Status | Effect on the item | UI |
| --- | --- | --- |
| `OK` | Price and known stock updated from the server. | Normal line. |
| `INSUFFICIENT_STOCK` | Quantity lowered to `availableStock`; price and stock updated. | "Quedan N unidades: ajustamos la cantidad." The lowered cart is revalidated, so the total includes it. |
| `OUT_OF_STOCK` | Kept, marked unavailable (`stock = 0`); price updated. | Greyed out, "Sin stock", price struck through, "Quitar del carrito" button; excluded from the total. |
| `NOT_FOUND` | Kept, marked unavailable (`stock = 0`). | Same, with "Ya no está disponible". |

- An unavailable item is sent again on the next validation, so it becomes available again if it is restocked.
- **Total**: the server's `total`, formatted in ARS (`formatPrice`, `Intl.NumberFormat("es-AR")`). While a validation is pending it shows "Calculando…".
- **API failure**: the cart is still shown with its last known data, plus a notice and a "Reintentar" button; the total is labelled "Total estimado" and computed locally from the snapshot prices of available items.
- The server never stores the cart and validation does not reserve stock: Vitta confirms availability on WhatsApp.

## Changelog

- **2026-11-12**: Mission 2.3: client-side cart (zustand + localStorage), header badge and cart drawer revalidated against `POST /api/cart/validate`; "Agregar al carrito" wired on the product detail.

- **2026-11-11**: Mission 2.2: product detail page (`/productos/{slug}`) with gallery, size picker with stock states, branded 404 and Open Graph metadata; catalog cards link to it.

- **2026-11-11**: Mission 2.1: product detail with ordered images and sized variants (`V2__create_product_images`), and read-only cart validation (`POST /api/cart/validate`).

- **2026-11-10**: Mission 1.1: catalog backend (schema, dev seed data, read-only catalog API).
- **2026-11-10**: Mission 0 completed (monorepo setup, backend and frontend scaffolding, CLAUDE.md, CI with Testcontainers, spec and PR template).
