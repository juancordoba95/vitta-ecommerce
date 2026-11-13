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
| `product_images` | `id`, `product_id`, `url` (500), `storage_key` (300, nullable), `position` | unique `(product_id, position)`; FK to `products` (on delete cascade); `position >= 0` |
| `admin_users` | `id`, `email` (254), `password_hash` (100, BCrypt), `created_at` | `email` unique |

- A product belongs to exactly one category and has one variant per size (S, M, L for now). Stock lives on the variant.
- Sizes are free text; the API sorts them logically, not alphabetically: letter sizes `XXS … XXXL` first, then numeric sizes ascending, then anything else alphabetically.
- `products.image_url` is the cover used by listings; `product_images` is the ordered gallery shown in the detail (`V2` backfilled each existing cover as position `0`). Since Mission 4.2 the admin keeps `image_url` equal to the image at position `0` (or `null` without images). `storage_key` (`V4`) identifies the file in the image storage so it can be deleted; it is `null` for external URLs such as the dev seed.
- Inactive products (`active = false`) stay in the database but are hidden from the public catalog (listing, detail by slug → `404`, cart validation → `NOT_FOUND`). This is the only "delete" for products (logical).
- Prices are in ARS.
- **Dev data**: with the `dev` profile, Flyway also loads `db/dev-data` (a repeatable, idempotent seed with 3 categories and 8 products, one of them inactive). It never runs in other environments or in tests.

## API

Base path `/api`, JSON. The catalog and cart endpoints below are public and read-only; admin endpoints need a JWT (see [Admin auth](#admin-auth)). Slugs must match `^[a-z0-9]+(-[a-z0-9]+)*$`.

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
- `400`: invalid slug in the path or the `category` parameter; invalid cart validation, login or admin body/parameters; image file that is not a JPEG/PNG/WebP; image order list that does not match the gallery.
- `401`: wrong login credentials; missing, malformed or expired token (also any route outside the access rules below).
- `403`: valid token without the `ADMIN` role.
- `404`: (admin) unknown product, variant, category or image id, including a `categoryId` in a product body.
- `409`: (admin) explicit slug already in use, repeated size in a product, more than 8 images per product.
- `413`: image file over 5 MB (or a multipart request over 45 MB).
- `429`: too many failed logins (with `Retry-After`).
- `500`: unexpected error (generic message; details are only logged).

**CORS**: `/api/**` allows `GET`, `POST`, `PUT`, `PATCH`, `DELETE` (the last three for the admin CRUD) with the `Authorization` and `Content-Type` headers, from the origin in `FRONTEND_URL` (default `http://localhost:3000`). Configured in the Spring Security filter chain.

### Admin auth

Stateless JWT (HS256, signed with `JWT_SECRET`) for the admin panel. No sessions, cookies, CSRF, form login or HTTP Basic. A single admin account (multiple admins are out of v1); no refresh tokens or password recovery: when the token expires the admin logs in again.

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | public | `{ "email", "password" }` → `200 { "token", "expiresAt" }`. |
| `GET` | `/api/admin/me` | `ADMIN` | `200 { "email" }`. Lets the frontend check that its session is still valid. |

```jsonc
// POST /api/auth/login
{ "email": "admin@example.com", "password": "..." }
// 200
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "expiresAt": "2026-11-12T22:00:00Z" }
```

- Login body: `email` required, valid email, ≤ 254 chars (matched case-insensitively); `password` required, ≤ 72 chars (BCrypt limit). Violations → `400`.
- Wrong password and unknown email return the same `401` (`"Invalid email or password"`), and both run a BCrypt check so timing does not reveal which accounts exist.
- **Rate limit**: in memory, per client IP (`remoteAddr`): after 5 failed logins within a 1-minute window, further attempts get `429` (even with the right password) until the window ends. A successful login resets the counter. Behind a reverse proxy, `server.forward-headers-strategy` must be set so `remoteAddr` is the real client IP.
- Token claims: `iss` = `vitta-backend`, `sub` = admin email, `roles` = `["ADMIN"]`, `iat`, `exp` (`JWT_EXPIRATION`, default 8h). Send it as `Authorization: Bearer <token>`.
- Passwords are stored as BCrypt hashes in `admin_users` (`V3`); the table has no seed data.

**Access rules** (anything not listed is denied):

| Rule | Paths |
| --- | --- |
| Public | `GET /api/products/**`, `GET /api/categories/**`, `POST /api/cart/validate`, `POST /api/auth/login`, `GET /actuator/health/**`, `GET /uploads/**` (local image storage) |
| Role `ADMIN` | `/api/admin/**` |
| Denied (`401` without token) | everything else |

**Configuration** (backend env vars, template in `backend/.env.example`):

| Variable | Required | Description |
| --- | --- | --- |
| `JWT_SECRET` | yes (outside `dev`) | HS256 secret, at least 32 bytes; startup fails if missing or shorter. Generate with `openssl rand -base64 48`. |
| `JWT_EXPIRATION` | no | Token lifetime as a Spring `Duration` (`8h`, `30m`). Default `8h`. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | no | On startup, if no admin with that email exists, one is created with the BCrypt-hashed password. Existing admins are never modified. If either is missing, a warning is logged and nothing is created. |

The `dev` profile (`application-dev.yaml`) has insecure placeholder defaults for `JWT_SECRET`, `ADMIN_EMAIL` (`admin@vitta.local`) and `ADMIN_PASSWORD`, so local development works without extra setup. Tests use a test-only secret in `src/test/resources/config/application.yaml`; CI needs no GitHub secrets.

### Admin catalog

Catalog management for the admin panel. Everything is under `/api/admin/**` (role `ADMIN`), JSON unless noted, and errors use the Problem Details above.

| Method | Path | Body / params | Response |
| --- | --- | --- | --- |
| `GET` | `/api/admin/products` | `q` (name contains, case-insensitive, ≤ 150), `categoryId`, `page` (≥ 0, default 0), `size` (1–100, default 20) | `200` page of products, **including inactive**, sorted by name |
| `GET` | `/api/admin/products/{id}` | | `200` detail with variants and images |
| `POST` | `/api/admin/products` | product body | `201` detail + `Location`; created **inactive** |
| `PUT` | `/api/admin/products/{id}` | product body | `200` detail |
| `PATCH` | `/api/admin/products/{id}/active` | `{ "active": true }` | `200` detail (publish / logical delete) |
| `POST` | `/api/admin/products/{id}/variants` | `{ "size", "stock" }` | `201` variant + `Location` |
| `PUT` | `/api/admin/variants/{id}` | `{ "size", "stock" }` | `200` variant |
| `PATCH` | `/api/admin/variants/{id}/stock` | `{ "stock" }` | `200` variant (quick stock fix) |
| `DELETE` | `/api/admin/variants/{id}` | | `204` |
| `GET` | `/api/admin/categories` | | `200` all categories by name |
| `POST` | `/api/admin/categories` | `{ "name", "slug"? }` | `201` category + `Location` |
| `PUT` | `/api/admin/categories/{id}` | `{ "name", "slug"? }` | `200` category (no delete) |
| `POST` | `/api/admin/products/{id}/images` | `multipart/form-data`, one or more `files` parts | `201` whole gallery |
| `PUT` | `/api/admin/products/{id}/images/order` | `{ "imageIds": [3, 1, 2] }` | `200` whole gallery |
| `DELETE` | `/api/admin/images/{id}` | | `204` |

```jsonc
// POST /api/admin/products
{ "name": "Conjunto Encaje Floral", "description": "Encaje negro", "price": 42900.00,
  "categoryId": 1, "slug": "" }
// 201 (GET /api/admin/products/{id} has the same shape)
{ "id": 9, "name": "Conjunto Encaje Floral", "slug": "conjunto-encaje-floral", "description": "Encaje negro",
  "price": 42900.00, "imageUrl": null, "active": false,
  "category": { "id": 1, "name": "Conjuntos", "slug": "conjuntos" },
  "images": [], "variants": [], "createdAt": "2026-11-12T10:00:00", "updatedAt": "2026-11-12T10:00:00" }

// GET /api/admin/products?q=encaje&page=0&size=20
{ "content": [{ "id": 9, "name": "...", "slug": "...", "price": 42900.00, "imageUrl": null,
                "category": { "id": 1, "name": "Conjuntos", "slug": "conjuntos" }, "active": false,
                "updatedAt": "..." }],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
```

**Products**
- Body: `name` required (≤ 150); `description` optional (≤ 2000, blank → `null`); `price` required, **> 0**, at most 8 integer digits and 2 decimals; `categoryId` required (unknown → `404`); `slug` optional (≤ 170, slug pattern).
- New products start **inactive** (`active = false`) so they do not show up publicly without photos or stock; `PATCH …/active` publishes or hides them.
- Products are never physically deleted (out of scope); `active = false` is the delete.

**Slugs** (products and categories)
- Explicit slug: must match the slug pattern; if another product/category already uses it → `409`.
- Empty or missing slug on **create**: generated from the name (accents removed, `ñ` → `n`, lowercase, non-alphanumerics → `-`, trimmed to leave room for a suffix; fallback `producto`/`categoria`). If taken, `-2`, `-3`, … is appended.
- Empty or missing slug on **update**: the current slug is kept, so shared links keep working.

**Variants**
- `size` required (≤ 10), stored trimmed and uppercase; unique per product, case-insensitive → `409` if repeated. `stock` required, `0`–`9999`.
- Deleting a variant is physical; carts holding it get `NOT_FOUND` on validation.

**Categories**: `name` required (≤ 100), `slug` optional (≤ 120) with the slug rules above. No delete (every product needs one).

**Images**
- Upload: JPEG, PNG or WebP only, detected from the **file content** (magic bytes), not the extension or declared content type → otherwise `400`. Max **5 MB per file** (`413`) and **8 images per product** (`409`, counting the existing ones). All files are validated before anything is stored; new images go after the existing ones.
- Order: `imageIds` must contain every image of the product exactly once (`400` otherwise). Position `0` is the main image and is copied to `products.image_url` (the listing cover) after every upload, reorder or delete.
- Delete: removes the row, closes the gap in positions and, after the transaction commits, deletes the file from the storage (a storage failure is only logged; the file is orphaned).
- Multipart limits (`application.yaml`): `max-file-size: 5MB`, `max-request-size: 45MB`.

**Storage** (`ImageStorage`, one implementation active):
- **Cloudinary** when `CLOUDINARY_URL` is set: official SDK (`cloudinary-http5`), folder `vitta/products`; URL = `secure_url`, key = `public_id`.
- **Local** otherwise: files saved as `products/<uuid>.<ext>` under `UPLOADS_DIR` (default `./uploads`, gitignored) and served publicly by the backend at `GET /uploads/**`. URLs are absolute, built from the upload request's host. Meant for development; files on a container disk are lost on redeploy.

| Variable | Required | Description |
| --- | --- | --- |
| `CLOUDINARY_URL` | no | `cloudinary://<api_key>:<api_secret>@<cloud_name>`. If set, images go to Cloudinary. |
| `UPLOADS_DIR` | no | Local storage directory when `CLOUDINARY_URL` is not set. Default `./uploads`. |

## Frontend

### Routes

| Route | Description |
| --- | --- |
| `/` | Hero + catalog grid; `?category={slug}` filters by category. Each card links to the product detail. |
| `/productos/{slug}` | Product detail (consumes `GET /api/products/{slug}`). |
| `/checkout` | Order summary revalidated against the API plus the customer form; sends the order via WhatsApp (see [Checkout via WhatsApp](#checkout-via-whatsapp)). |
| `/checkout/confirmacion` | "¡Pedido listo!" screen with the order code. |
| `/admin/login` | Admin login (see [Admin frontend](#admin-frontend)). |
| `/admin` | Admin panel home; protected. |
| `/admin/productos`, `/admin/categorias` | Admin sections (placeholders until M4.4); protected. |

The cart is not a route: it is a drawer available on every page from the header (see [Cart](#cart)).

Layouts: the root `app/layout.tsx` only holds the document (fonts, `<html>`, `<body>`). The store pages live in the `app/(store)/` route group, whose layout adds the header, footer and cart drawer (`StoreChrome`, also used by the root `not-found.tsx`); `app/admin/` has its own frame. Route groups do not change URLs.

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
- "Agregar al carrito" adds one unit of the selected size and opens the cart drawer. The button is disabled when no size is selected, when the selected size has no stock, or when the cart already holds all its known stock; in that last case it shows "Ya agregaste todas las unidades disponibles" (checked after mounting, since the cart lives in the browser). If the cart already has 50 distinct items, nothing is added and an inline message explains why.

### Cart

The cart lives only in the browser (see `POST /api/cart/validate`). Code: `frontend/src/lib/cart/` (pure logic in `cart-items.ts`, store in `cart-store.ts`, server revalidation in `use-cart-validation.ts`) and `frontend/src/components/cart/`.

- **Store**: [zustand](https://github.com/pmndrs/zustand) with the `persist` middleware. Only `items` are saved, in `localStorage` under the key `vitta-cart` (version `1`); the drawer's open state is not persisted. Stored items are sanitized on load (malformed entries, duplicates and anything beyond the limits are dropped).
- **Item**: `variantId`, `productSlug`, `productName`, `size`, `imageUrl`, `unitPrice` (snapshot in ARS, for display only), `quantity` and `stock` (last known stock of the variant; `0` means not available).
- **Limits** (same as the validation endpoint):
  - At most **50 distinct items** (`MAX_CART_ITEMS`). Adding a new variant to a full cart is rejected.
  - **The store enforces the limits itself**, whatever calls it: `addItem` rejects variants without stock (`out-of-stock`), additions once the cart holds the known stock (`stock-limit`), a full cart (`cart-full`) and malformed input such as a quantity that is not a positive integer (`invalid`). A partial fit is capped at the stock. Rejections leave the cart untouched.
  - Adding a variant already in the cart **sums the quantity** instead of duplicating it.
  - `quantity` is always between **1** and the **known stock** (and never above `999`). The `−` button stops at 1 (removing is a separate "Quitar" action) and `+` stops at the stock.
- **Hydration**: the persisted cart is only read on the client. The header badge and the drawer render it only after mounting (`useHydrated`), so the server HTML and the first client render always match.
- **Header**: a bag icon on the right opens the drawer; its badge shows the total **units** (sum of quantities), `99+` above 99, and is hidden when the cart is empty.
- **Drawer**: a modal `<dialog>` (focus trap, `Escape`, click on the backdrop closes it, page scroll locked). Full width on mobile, a right-hand panel (`max-w-md`) from `sm` up. Each line shows image, name (links to the detail), size, unit price, `−`/`+` and "Quitar". Empty state: "Tu carrito está vacío" with a link to the catalog. "Finalizar pedido" (shown whenever the cart is not empty) closes the drawer and goes to `/checkout`.

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

### Checkout via WhatsApp

Code: `frontend/src/lib/checkout/` (pure logic: message and URL in `whatsapp.ts`, order code in `order-code.ts`, form rules in `checkout-form.ts`, change detection in `cart-changes.ts`, last order in `pending-order.ts`) and `frontend/src/components/checkout/`.

**This version does not decrement or reserve stock.** Nothing is written on the server: the order is a pre-filled WhatsApp message, and Mariela confirms stock, payment and delivery in that chat.

#### Configuration

| Variable | Description |
| --- | --- |
| `NEXT_PUBLIC_WHATSAPP_NUMBER` | Store number that receives the orders, in international format with digits only: `54` + `9` + area code without the leading `0` + number without `15` (e.g. Buenos Aires `11 1234-5678` → `5491112345678`). Spaces, dashes, parentheses and a leading `+` are tolerated and stripped. |

- It is a `NEXT_PUBLIC_` variable, so it is inlined into the client bundle **at build time**: changing it requires a new build (on Vercel, a redeploy).
- The number only lives in environment variables (`frontend/.env.local` locally, the Vercel project settings in production); `.env.example` has an empty placeholder. It is never hardcoded.
- If it is missing or malformed (not 10–15 digits), the page still works and "Confirmar pedido" shows "No podemos enviar pedidos por WhatsApp en este momento: falta configurar el número de la tienda…" instead of opening WhatsApp (and logs an error in the console).

#### Flow

1. **Drawer → `/checkout`**. The page renders after mounting (the cart lives in the browser); an empty cart shows "Tu carrito está vacío" with a link to the catalog.
2. **Summary** ("Tu pedido"): the same lines, stock rules, revalidation, total and API-failure notice as the drawer (it reuses `CartLine`, `CartTotal` and `useCartValidation`), so quantities can still be edited or items removed. Unavailable items are not part of the order nor of the total.
3. **Form** ("Tus datos"), validated on the client when confirming and then live as the customer types, with one message per field; the focus goes to the first invalid field:

   | Field | Rule | Message |
   | --- | --- | --- |
   | Nombre | Required, up to 80 chars | "Ingresá tu nombre." |
   | Entrega | Required: `Retiro` or `Envío` | "Elegí cómo querés recibir tu pedido." |
   | Dirección | Required for `Envío` only, up to 120 chars | "Ingresá la dirección de entrega." |
   | Localidad | Required for `Envío` only, up to 80 chars | "Ingresá tu localidad." |
   | Notas | Optional, up to 500 chars | — |

4. **"Confirmar pedido"**: the available items are sent to `POST /api/cart/validate` once more and compared with what the customer was seeing:
   - **Something changed** (price, quantity above the stock left, item out of stock or gone): the response is applied to the cart (same rules as the revalidation above), a "Hubo cambios en tu pedido" notice lists each change, and **nothing is sent**. She reviews the summary and confirms again.
   - **API failure**: an error asks to try again; nothing is sent.
   - **Cart edited while validating**: asks to review and confirm again.
   - **All OK**: a short order code is generated, the message and the `wa.me` URL are built from the server prices, the new tab (see below) is pointed at that URL, the order is saved in `sessionStorage` (`vitta-last-order`, with whether the tab could be opened) and the store tab moves to `/checkout/confirmacion`.
5. **Confirmation** (`/checkout/confirmacion`): the first time it shows a new order it **empties the cart**. It shows "¡Pedido listo!", the order code with "Guardalo para consultar por tu pedido por WhatsApp.", a WhatsApp link (`target="_blank"`, `rel="noopener noreferrer"`) and "Volver al catálogo". If the tab opened, the copy says WhatsApp was opened in a new tab and the link reads "Abrir WhatsApp de nuevo" (secondary); if the browser blocked it, the link is the main button "Abrir WhatsApp". The order is marked as handled, so going back to this screen or reloading it never empties the cart again. Without a stored order it shows "No encontramos un pedido reciente".

**WhatsApp in a new tab**: browsers (especially on mobile) block `window.open()` that is not directly inside a user gesture, and confirming has to `await` the revalidation first. So the submit handler opens an empty tab synchronously ("Abriendo WhatsApp…", `opener` cleared), *before* the revalidation, and only sets its location to `wa.me` once everything is OK; on any failure (API error, cart edited, changes detected) that tab is closed. If `window.open()` returns `null` (blocked anyway), the confirmation screen's "Abrir WhatsApp" button is the fallback. On phones the `wa.me` link hands off to the WhatsApp app; on desktop it opens WhatsApp Web in the new tab while the store stays on "¡Pedido listo!".

#### Order code

`VT-` + 4 random characters from `23456789ABCDEFGHJKLMNPQRSTUVWXYZ` (no `0`/`O`, `1`/`I`; Web Crypto), e.g. `VT-4F7K`. It only identifies the order in the chat; it is not stored on the server and is not guaranteed unique (~1M combinations).

#### Message

`https://wa.me/{NEXT_PUBLIC_WHATSAPP_NUMBER}?text={encodeURIComponent(message)}`. Plain text with line breaks, no WhatsApp markdown; prices via `formatPrice` (ARS). Customer fields are collapsed to one line (notes keep their line breaks). Example:

```text
¡Hola Vitta! Quiero hacer este pedido:

Código de pedido: VT-4F7K

• Conjunto Encaje Floral · Talle S · x1 · $ 42.900
• Conjunto Tul Bordado · Talle S · x2 · $ 79.800

Total: $ 122.700

Nombre: Ana Pérez
Entrega: Envío a domicilio
Dirección: Av. Siempre Viva 742
Localidad: Córdoba

Notas:
Tocar timbre 2B
```

- One line per item: name · size · quantity · subtotal (`unitPrice × quantity`, server price).
- `Entrega` is `Retiro` or `Envío a domicilio`; `Dirección` and `Localidad` only appear for shipping; the `Notas` block only when there are notes.
- Shipping cost is out of v1: it is agreed in the chat.

### Admin frontend

Code: `frontend/src/lib/admin/` (server-only session helpers), `frontend/src/app/api/session/`, `frontend/src/proxy.ts`, `frontend/src/app/admin/` and `frontend/src/components/admin/`.

**Session by httpOnly cookie**: the browser never sees the JWT. The Next server talks to the backend and keeps the token in a cookie:

| Cookie | Value | Attributes |
| --- | --- | --- |
| `vitta_admin` | Backend JWT | `HttpOnly`, `SameSite=Lax`, `Secure` in production, `Path=/`, `Expires` = the login's `expiresAt` |

Route handlers (same origin as the front, not the backend):

| Method | Path | Behavior |
| --- | --- | --- |
| `POST` | `/api/session` | `{ email, password }` → calls `POST /api/auth/login` from the server. `200 { ok: true }` and sets the cookie; `401 { error: "invalid" }` (also for `400`); `429 { error: "rate_limited" }` with the backend's `Retry-After`; `502 { error: "unavailable" }` when the backend is down or answers something unexpected. The token is never in the response. |
| `DELETE` | `/api/session` | Logout: deletes the cookie, `204`. |
| `GET` | `/api/session/expired` | Deletes the cookie and `303` redirects to `/admin/login`. Used by the panel layout when the token is rejected (Server Components can't delete cookies). |

- **Backend URL**: the server reuses `NEXT_PUBLIC_API_URL` (no extra variable); it must be reachable from the Next server.
- **`adminFetch(path, { method, body })`** (`lib/admin/admin-fetch.ts`, server-only): calls the backend with `Authorization: Bearer <cookie>` and parses the JSON. Throws `AdminUnauthorizedError` when there is no cookie or on `401`/`403`, `ApiError` otherwise. For the admin screens (M4.4), from Server Components, Server Functions or Route Handlers only.
- **Rate limit**: logins now reach the backend from the Next server's IP, so the per-IP limit (5 failures/minute) is shared by every admin login attempt. Acceptable with a single admin.

**Protected routes**:

1. `proxy.ts` (matcher `/admin`, `/admin/:path*`) only checks that the cookie exists: without it, any `/admin/**` page except `/admin/login` redirects to `/admin/login`; `/admin/login` with it redirects to `/admin`.
2. The panel layout (`app/admin/(panel)/layout.tsx`) validates the token with `GET /api/admin/me` before rendering anything (behind a `<Suspense>` skeleton, as required by Cache Components). `401`/`403` → `/api/session/expired` → login. Backend unreachable → "No pudimos conectar con el servidor" (the session is kept).
3. The login page lives in `app/admin/(auth)/`, outside the panel layout.

**Screens**: `/admin/login` (email + password; "Email o contraseña incorrectos" on `401`, "Demasiados intentos, probá de nuevo en un minuto" on `429`, a generic error otherwise; the button is disabled while sending). The panel has a top bar on phones and a sidebar from `lg` with the logo, links to Productos and Categorías, the admin email and "Cerrar sesión". `/admin` shows a welcome; the sections are placeholders until M4.4.

**Not indexed**: `app/admin/layout.tsx` sets `robots: { index: false, follow: false }` for the whole `/admin` tree (login included). There is no sitemap.

## Changelog

- **2026-11-12**: Mission 4.2: admin catalog API under `/api/admin/**`: paginated product search (including inactive), product create/update (created inactive) and `PATCH …/active`, variants with size uniqueness and quick stock updates, categories (no delete), slug generation with suffixes, and product images (content-checked JPEG/PNG/WebP, 5 MB, 8 per product, reorder with cover sync, delete) stored in Cloudinary (`CLOUDINARY_URL`) or locally (`UPLOADS_DIR`, served at `/uploads/**`). `V4` adds `product_images.storage_key`.

- **2026-11-12**: Mission 4.1: admin auth with stateless JWT (HS256): `admin_users` table (`V3`), admin bootstrap from `ADMIN_EMAIL`/`ADMIN_PASSWORD`, `POST /api/auth/login` with per-IP rate limit, `GET /api/admin/me`, deny-by-default access rules, 401/403/429 as Problem Details, CORS moved into the security chain (adds `PUT`/`PATCH`/`DELETE` and `Authorization`).
- **2026-11-12**: Mission 4.3: admin login and protected `/admin` in the frontend: session in the httpOnly `vitta_admin` cookie through `/api/session` route handlers, server-only `adminFetch`, `proxy.ts` guard plus server-side validation with `/api/admin/me`, noindex. Store pages moved into the `(store)` route group (URLs unchanged). Checkout opens WhatsApp in a new tab, with an "Abrir WhatsApp" fallback when the browser blocks it.

- **2026-11-12**: Mission 3: WhatsApp checkout (`/checkout` with validated summary and customer form, revalidation and change detection before sending, `wa.me` message with order code, `/checkout/confirmacion`), configured with `NEXT_PUBLIC_WHATSAPP_NUMBER`. Stock is not decremented.

- **2026-11-12**: Mission 2.3: client-side cart (zustand + localStorage), header badge and cart drawer revalidated against `POST /api/cart/validate`; "Agregar al carrito" wired on the product detail.

- **2026-11-11**: Mission 2.2: product detail page (`/productos/{slug}`) with gallery, size picker with stock states, branded 404 and Open Graph metadata; catalog cards link to it.

- **2026-11-11**: Mission 2.1: product detail with ordered images and sized variants (`V2__create_product_images`), and read-only cart validation (`POST /api/cart/validate`).

- **2026-11-10**: Mission 1.1: catalog backend (schema, dev seed data, read-only catalog API).
- **2026-11-10**: Mission 0 completed (monorepo setup, backend and frontend scaffolding, CLAUDE.md, CI with Testcontainers, spec and PR template).
