# Deploy

Production layout:

| Piece | Service | URL / location |
| --- | --- | --- |
| Frontend | **Netlify** (Free), project `vittabymarie` | https://vittabymarie.netlify.app |
| Backend | **Render** (Docker, Free, 512 MB), service `vitta-backend`, region Virginia | https://vitta-backend-plus.onrender.com |
| Database | **TiDB Cloud Starter** (MySQL-compatible), AWS `us-east-1` | database `vitta`, port `4000` |
| Images | **Cloudinary** | folder `vitta/products` |

Everything backend-specific for production lives in the `prod` profile (`backend/src/main/resources/application-prod.yaml`); `dev` is unchanged. Never write real secret values in this repo: only variable names and formats appear below.

# Deploy — database (TiDB Cloud Starter)

TiDB Cloud Starter is the production database. It speaks the MySQL protocol, so the JDBC driver, Flyway and JPA work unchanged.

| Setting | Value |
| --- | --- |
| Plan / cloud / region | Starter, AWS, `us-east-1` |
| Database | `vitta` |
| Character set / collation | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| Port | `4000` (not 3306) |
| TLS | required, with host verification (`sslMode=VERIFY_IDENTITY`) |

Create the database before the first deploy, with the explicit character set and collation (Flyway does not create it):

```sql
CREATE DATABASE vitta CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

`DB_URL` format (a single line, no spaces or quotes):

```
jdbc:mysql://<host>:4000/vitta?sslMode=VERIFY_IDENTITY
```

`<host>` is the cluster endpoint shown in TiDB Cloud → Connect. `DB_USERNAME` / `DB_PASSWORD` are the credentials generated there.

**CHECK constraints are not enforced.** TiDB ignores them (`tidb_enable_check_constraint` is off), so validation lives in the backend (Bean Validation and service rules); do not rely on the database to reject invalid rows.

# Deploy — backend (Render)

## Render runbook

Create a **Web Service** on Render from the repo's **public Git URL** (no GitHub app connection needed):

| Setting | Value |
| --- | --- |
| Name | `vitta-backend` |
| Region | Virginia (US East) |
| Runtime / language | Docker |
| Root directory | `backend` |
| Dockerfile path | `./Dockerfile` (relative to the root directory) |
| Docker build context | `.` (the root directory) |
| Instance type | Free |
| Health check path | `/actuator/health` |
| Deploys | **manual**: Render → **Manual Deploy** → *Deploy latest commit* (a service created from a public URL does not auto-deploy on push) |

After merging backend changes to `main`, trigger a Manual Deploy. Changing an environment variable restarts the service.

The image builds the jar without running tests (they already run in CI), runs as a non-root user and listens on `PORT` (Render injects it; default `8080`). `/actuator/health` is public and returns `{"status":"UP"}` when the app and the database are reachable; details are never shown.

### Environment variables

Set them in Render → Environment. Never commit real values.

| Variable | Required | Description |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | yes | Must be `prod`. Activates `application-prod.yaml`. Startup fails if `prod` and `dev` are both active. |
| `DB_URL` | yes | JDBC URL of the TiDB Cloud database, with TLS: `jdbc:mysql://<host>:4000/vitta?sslMode=VERIFY_IDENTITY`. See the database section above. |
| `DB_USERNAME` | yes | Database user generated in TiDB Cloud. |
| `DB_PASSWORD` | yes | Database password. |
| `JWT_SECRET` | yes | HS256 secret, at least 32 bytes. Generate with `openssl rand -base64 48`. |
| `FRONTEND_URL` | yes | Allowed CORS origin, the frontend URL without trailing slash: `https://vittabymarie.netlify.app`. No default in `prod`. |
| `CLOUDINARY_URL` | yes | `cloudinary://API_KEY:API_SECRET@CLOUD_NAME`, exactly one `@` (see the Cloudinary section). Required in `prod` (Render's disk is not persistent): startup fails without it. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | first deploy | Creates the admin on startup if it does not exist. Can be removed after the first successful deploy; existing admins are never modified. |
| `JWT_EXPIRATION` | no | Admin token lifetime (`8h`, `30m`). Default `8h`. |
| `PORT` | no | Injected by Render. Default `8080`. |

`UPLOADS_DIR` is not used in production (local storage is disabled by the Cloudinary requirement).

### What the `prod` profile does

- **Datasource** only from `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`; HikariCP pool of at most 5 connections (minimum idle 1) to stay within free-tier connection limits.
- **Flyway** runs only `classpath:db/migration`. The dev seed (`db/dev-data`) never runs: the production database starts empty, except for the admin created from `ADMIN_EMAIL`/`ADMIN_PASSWORD`. The schema is applied to the empty `vitta` database on the first start.
- **Proxy headers**: `server.forward-headers-strategy=native` (Tomcat `RemoteIpValve`). The client address is the right-most `X-Forwarded-For` entry that is not a trusted internal proxy, so clients cannot spoof it by sending their own header.
- **Errors and logs**: no messages, stack traces or binding errors in HTTP error responses; SQL and bind-parameter logging off; health details hidden. The app does not log secrets.
- **CORS**: `FRONTEND_URL` is required.
- **Storage**: `CLOUDINARY_URL` is required.
- The local `backend/.env` is never read in production: it is excluded by `.dockerignore`, the container's working directory (`/app`) has none, and real environment variables take precedence anyway.

### Login rate limit behind Netlify

The admin login reaches the backend from the Next.js server (the `/api/session` route handler), not from the browser, so the address the backend sees is the Next.js server's egress IP, shared by every user. The limiter therefore keys on both:

- **email** (normalized): 5 failures per minute lock that account, from any address;
- **IP**: a higher cap of 30 failures per minute, as a backstop against one client trying many emails.

A few wrong passwords from one person do not block everyone else. Someone who knows the admin email can lock it for one minute at a time; that is acceptable for a single-admin store.

To check after the first deploy (M5.3): if Render's proxies are not in Tomcat's default internal-proxy ranges (10/8, 192.168/16, 172.16/12, 169.254/16, 127/8, ::1, fc00::/7), the address seen is Render's proxy instead. The email key still protects the account; `server.tomcat.remoteip.internal-proxies` can be tuned if needed.

## Keeping Render awake

Render Free sleeps the service after 15 minutes without traffic. Set an external ping (a free **cron-job.org** job) to `GET https://vitta-backend-plus.onrender.com/actuator/health` every 5–10 minutes. It returns `{"status":"UP"}` and also touches the database.

## Local test of the production image

Requires Docker. Uses throwaway, non-secret values and a fake (well-formed) `CLOUDINARY_URL`; no image uploads are tried. Run from the repo root (Git Bash / PowerShell: the commands are the same except line continuations, so they are written on one line each).

```bash
# 1. Network and an empty MySQL 8.4
docker network create vitta-net
docker run -d --name vitta-mysql --network vitta-net -e MYSQL_DATABASE=vitta -e MYSQL_USER=vitta -e MYSQL_PASSWORD=vitta-test -e MYSQL_ROOT_PASSWORD=root-test mysql:8.4

# 2. Build the image
docker build -t vitta-backend:local backend

# 3. Run it with the prod profile (wait until MySQL is ready, ~20 s)
docker run -d --name vitta-backend --network vitta-net -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -e "DB_URL=jdbc:mysql://vitta-mysql:3306/vitta?sslMode=REQUIRED" -e DB_USERNAME=vitta -e DB_PASSWORD=vitta-test -e JWT_SECRET=local-test-secret-not-for-production-0123456789 -e FRONTEND_URL=http://localhost:3000 -e CLOUDINARY_URL=cloudinary://000000000000000:fake-secret@fake-cloud vitta-backend:local

# 4. Checks
docker logs vitta-backend                       # "Started BackendApplication", Flyway applied V1..V4
curl http://localhost:8080/actuator/health      # {"status":"UP"}
docker exec vitta-mysql mysql -uvitta -pvitta-test vitta -e "SELECT version, description FROM flyway_schema_history; SELECT COUNT(*) FROM products;"   # V1..V4 only, 0 products (no seed)
docker run --rm --entrypoint ls vitta-backend:local -la /app    # only app.jar: no .env
docker run --rm --entrypoint id vitta-backend:local             # uid of the non-root "vitta" user

# 5. Without CLOUDINARY_URL the app must refuse to start
docker run --rm --network vitta-net -e SPRING_PROFILES_ACTIVE=prod -e "DB_URL=jdbc:mysql://vitta-mysql:3306/vitta?sslMode=REQUIRED" -e DB_USERNAME=vitta -e DB_PASSWORD=vitta-test -e JWT_SECRET=local-test-secret-not-for-production-0123456789 -e FRONTEND_URL=http://localhost:3000 vitta-backend:local
# -> fails with "app.storage.cloudinary-url (CLOUDINARY_URL) is required ..."

# 6. Clean up
docker rm -f vitta-backend vitta-mysql
docker network rm vitta-net
```

The MySQL 8.4 image generates a self-signed certificate, so `sslMode=REQUIRED` works locally too. (Production uses `VERIFY_IDENTITY` against TiDB Cloud's public certificate.)

CI builds the image on every backend change (`docker build`, no push) to validate the Dockerfile.

# Deploy — frontend (Netlify)

The frontend (Next.js 16, App Router) runs on **Netlify** (Free plan), project `vittabymarie` → https://vittabymarie.netlify.app. The Next.js adapter is declared **explicitly** in `netlify.toml` (`[[plugins]] package = "@netlify/plugin-nextjs"`); do not rely on Netlify autodetection. Without it Netlify published `frontend/.next` as static files and every route returned 404 (the build log showed "0 new function(s) to upload"). A healthy deploy lists the Next.js runtime/functions in the log. The version is not pinned.

## Netlify settings

The repo-root `netlify.toml` carries them; create the site from this GitHub repo and check:

| Setting | Value |
| --- | --- |
| Base directory | `frontend` |
| Build command | `npm run build` |
| Publish directory | `.next` (relative to the base directory; the adapter serves it) |
| Node | `22` (`NODE_VERSION` in `netlify.toml`, same as CI) |
| Plugin | `@netlify/plugin-nextjs`, declared in `netlify.toml` |
| Project visibility | **Public** (see below) |

New Netlify projects are created **private**: visitors hit a login wall. Make the site public in Project configuration → **Project visibility**. Deploys and their logs are under **Activity**.

### Environment variables

Set them in Netlify → Site configuration → Environment variables. Never commit real values.

| Variable | Required | Description |
| --- | --- | --- |
| `BACKEND_URL` | yes | Base URL of the Render backend, no trailing slash: `https://vitta-backend-plus.onrender.com`. **Server-only**: no `NEXT_PUBLIC_` prefix, so it never reaches the browser, which only talks to this site (route handlers under `/api`). If missing, the server fails with an explicit "BACKEND_URL is not set" error. |
| `NEXT_PUBLIC_WHATSAPP_NUMBER` | yes | Store number that receives orders, international format without `+`, spaces or dashes. Inlined into the bundle at build time: if it changes, **trigger a new deploy** (the running site keeps the old value otherwise). |

On the backend, set `FRONTEND_URL` to `https://vittabymarie.netlify.app` (no trailing slash). The browser never calls the backend, so CORS is not exercised by this site, but keep it correct.

## Images

Production images come from Cloudinary (`https://res.cloudinary.com/**`, whitelisted in `next.config.ts`). The local storage of the backend (`/uploads/**`) is whitelisted only in development.

## Admin session cookie

`vitta_admin` is `httpOnly`, `sameSite=lax`, `path=/` and `secure` when `NODE_ENV=production` (always the case on Netlify, which serves HTTPS). Logout and the expired-session route clear it with the same attributes. `proxy.ts` only checks that the cookie exists. Locally (`next dev`, plain http) `secure` is off.

## Render cold start

Render Free sleeps the backend after 15 minutes without traffic; the first request takes ~50 s. Netlify's docs list **60 s** for synchronous functions, but forum reports mention accounts with shorter limits (26–30 s), so we do not rely on it:

- every server-side call to the backend times out after **8 s** (`BACKEND_TIMEOUT_MS` in `src/lib/api/backend.ts`); the request still wakes Render up;
- store and admin pages catch that failure in an `error.tsx` boundary and show "La tienda está despertando, reintentá en unos segundos" with a **Reintentar** button, instead of a 500;
- catalog data is cached for minutes (`use cache`), so mostly the first visitor after the sleep sees that screen;
- cart validation (browser → `/api/cart/validate` → backend) fails after 8 s into the cart's existing error/retry state.

The external ping described in "Keeping Render awake" avoids the sleep in practice; the retry screen stays as a safety net.

# Deploy — Cloudinary

`CLOUDINARY_URL` format (values from Cloudinary → Settings → API Keys):

```
cloudinary://API_KEY:API_SECRET@CLOUD_NAME
```

- Exactly **one** `@`, between the secret and the cloud name. No spaces, quotes, angle brackets, trailing slash or newline; the whole value on one line.
- Do not paste a `CLOUDINARY_URL=` prefix into Render's value field: the variable name goes in the name field and only the `cloudinary://...` URL in the value.

## Rotating the API key

1. In Cloudinary, **create a new** API key; do not delete the old one yet.
2. In Render → Environment, set `CLOUDINARY_URL` to the new key and save (the service restarts).
3. Test an image upload from the admin panel and check it shows up in Cloudinary and in the store.
4. Only then delete the old key in Cloudinary.

If step 3 fails, restore the old value in Render (it still works) and fix the new one.

# Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| Backend fails to start with `java.net.UnknownHostException` (health check never passes) | `DB_URL` was loaded wrong in Render: typo in the host, a placeholder left (`<host>`), a stray space or newline, or a wrong format. | Copy the endpoint from TiDB Cloud → Connect and rebuild the URL as `jdbc:mysql://<host>:4000/vitta?sslMode=VERIFY_IDENTITY`. Save and check the Render logs. |
| Every Netlify route returns **404** | The Next.js adapter was not declared, so Netlify did not serve the app as Next.js. | Declare `@netlify/plugin-nextjs` in `netlify.toml` (`[[plugins]]`) and redeploy; the build log should list the Next.js functions. |
| Netlify asks for a login / site is private | New projects are created private. | Project configuration → Project visibility → public. |
| Admin login fails (generic error or timeout) | `BACKEND_URL` in Netlify is wrong (typo, trailing slash, `http` instead of `https`, or missing), or the backend was asleep (cold start ≈ 50 s; the frontend gives up after 8 s). | Check `BACKEND_URL` = `https://vitta-backend-plus.onrender.com`, open `/actuator/health` to wake the backend and retry. Set up the ping from "Keeping Render awake". A changed `BACKEND_URL` needs a new Netlify deploy. |
| WhatsApp button shows "falta configurar el número", or uses the old number | `NEXT_PUBLIC_WHATSAPP_NUMBER` is missing, malformed, or was changed without redeploying (baked in at build time). | Fix the variable and trigger a new Netlify deploy. |
| Image upload returns **500** | Malformed `CLOUDINARY_URL` in Render (extra `@`, spaces, quotes, leftover `CLOUDINARY_URL=` prefix, placeholders). | Rebuild it as `cloudinary://API_KEY:API_SECRET@CLOUD_NAME`, save and retry the upload. |
| The database accepts a row that violates a CHECK | TiDB ignores CHECK constraints. | Expected: validation is done in the backend. |
