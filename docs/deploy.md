# Deploy — backend

The backend runs as a Docker image on **Render** (Web Service, Free plan, 512 MB), with **Aiven** MySQL 8 and **Cloudinary** for product images. Everything production-specific lives in the `prod` profile (`backend/src/main/resources/application-prod.yaml`); `dev` is unchanged.

## Render runbook

Create a **Web Service** from this GitHub repo:

| Setting | Value |
| --- | --- |
| Runtime / language | Docker |
| Root directory | `backend` |
| Dockerfile path | `./Dockerfile` (relative to the root directory) |
| Docker build context | `.` (the root directory) |
| Instance type | Free |
| Health check path | `/actuator/health` |
| Auto-deploy | on commit to `main` |

The image builds the jar without running tests (they already run in CI), runs as a non-root user and listens on `PORT` (Render injects it; default `8080`). `/actuator/health` is public and returns `{"status":"UP"}` when the app and the database are reachable; details are never shown.

### Environment variables

Set them in Render → Environment. Never commit real values.

| Variable | Required | Description |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | yes | Must be `prod`. Activates `application-prod.yaml`. Startup fails if `prod` and `dev` are both active. |
| `DB_URL` | yes | JDBC URL of the Aiven service, with TLS: `jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED` (Aiven's default database is `defaultdb`). |
| `DB_USERNAME` | yes | Database user (Aiven: `avnadmin` or a dedicated user). |
| `DB_PASSWORD` | yes | Database password. |
| `JWT_SECRET` | yes | HS256 secret, at least 32 bytes. Generate with `openssl rand -base64 48`. |
| `FRONTEND_URL` | yes | Allowed CORS origin, the frontend URL without trailing slash (e.g. `https://<site>.netlify.app`). No default in `prod`. |
| `CLOUDINARY_URL` | yes | `cloudinary://<api_key>:<api_secret>@<cloud_name>`. Required in `prod` (Render's disk is not persistent): startup fails without it. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | first deploy | Creates the admin on startup if it does not exist. Can be removed after the first successful deploy; existing admins are never modified. |
| `JWT_EXPIRATION` | no | Admin token lifetime (`8h`, `30m`). Default `8h`. |
| `PORT` | no | Injected by Render. Default `8080`. |

`UPLOADS_DIR` is not used in production (local storage is disabled by the Cloudinary requirement).

### What the `prod` profile does

- **Datasource** only from `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`; HikariCP pool of at most 5 connections (minimum idle 1) to stay within free-tier connection limits.
- **Flyway** runs only `classpath:db/migration`. The dev seed (`db/dev-data`) never runs: the production database starts empty, except for the admin created from `ADMIN_EMAIL`/`ADMIN_PASSWORD`. All tables have a primary key, as Aiven enforces `sql_require_primary_key`.
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

## Local test of the production image

Requires Docker. Uses throwaway, non-secret values and a fake (well-formed) `CLOUDINARY_URL`; no image uploads are tried. Run from the repo root (Git Bash / PowerShell: the commands are the same except line continuations, so they are written on one line each).

```bash
# 1. Network and an empty MySQL 8.4 that, like Aiven, requires primary keys
docker network create vitta-net
docker run -d --name vitta-mysql --network vitta-net -e MYSQL_DATABASE=vitta -e MYSQL_USER=vitta -e MYSQL_PASSWORD=vitta-test -e MYSQL_ROOT_PASSWORD=root-test mysql:8.4 --sql-require-primary-key=ON

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

The MySQL 8.4 image generates a self-signed certificate, so `sslMode=REQUIRED` works locally too, the same as with Aiven.

CI builds the image on every backend change (`docker build`, no push) to validate the Dockerfile.

# Deploy — frontend (Netlify)

The frontend (Next.js 16, App Router) runs on **Netlify** (Free plan). Netlify detects Next.js and installs its adapter/runtime automatically, so `netlify.toml` declares no plugin. Do not pin the adapter version.

## Netlify settings

The repo-root `netlify.toml` already carries them; create the site from this GitHub repo and check:

| Setting | Value |
| --- | --- |
| Base directory | `frontend` |
| Build command | `npm run build` |
| Publish directory | empty (handled by the Next.js adapter) |
| Node | `22` (`NODE_VERSION` in `netlify.toml`, same as CI) |

### Environment variables

Set them in Netlify → Site configuration → Environment variables. Never commit real values.

| Variable | Required | Description |
| --- | --- | --- |
| `BACKEND_URL` | yes | Base URL of the Render backend, no trailing slash. **Server-only**: no `NEXT_PUBLIC_` prefix, so it never reaches the browser, which only talks to this site (route handlers under `/api`). If missing, the server fails with an explicit "BACKEND_URL is not set" error. |
| `NEXT_PUBLIC_WHATSAPP_NUMBER` | yes | Store number that receives orders, international format without `+`, spaces or dashes. Inlined at build time: redeploy after changing it. |

On the backend, set `FRONTEND_URL` to the final Netlify URL. The browser never calls the backend, so CORS is not exercised by this site, but keep it correct.

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

Not implemented (option): an external keep-alive ping (e.g. a free uptime monitor hitting `/actuator/health` every ~10 min) would avoid the sleep, at the cost of consuming Render's free instance hours.
