# Restaurant Management System

Spring Boot REST API + Next.js frontend, built from the clickable prototype in
`../Prototype-Restaurant-mgs`.

📄 **Start here → [PROJECT-SPEC.md](PROJECT-SPEC.md)** — domain model, API contract,
screen map, and the 14-milestone build order.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5.6 · Java 17 · Spring Security + JWT · Spring Data JPA |
| Database | PostgreSQL 16 · Flyway migrations |
| Frontend | Next.js (App Router) · TypeScript · Tailwind CSS · React Query |
| Docs | Swagger / springdoc-openapi |

## Quick start

No database required — the `dev` profile runs on in-memory H2:

```bash
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'
```

```bash
cd frontend && npm run dev
```

Open <http://localhost:3000> — the page reports whether it can reach the API.

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API health | http://localhost:8081/api/health |
| Swagger | http://localhost:8081/swagger-ui.html |
| H2 console (`dev` only) | http://localhost:8081/h2-console |

For PostgreSQL instead of H2, see [PROJECT-SPEC.md §9](PROJECT-SPEC.md).

## Status

**Milestones 1 (Scaffold) and 2 (Schema) — complete and verified.**

- `./gradlew build` passes; `/api/health` returns `status: UP`, `database: UP`
- Flyway applies V1–V3; Hibernate `ddl-auto=validate` passes, so the entity
  mappings provably match the migrations
- `./gradlew test` — **6 tests, 0 failures, 0 skipped**
- `npm run build` passes; `tsc --noEmit` reports 0 errors
- Swagger UI and `/v3/api-docs` both return 200

### Default accounts

Created at first startup by `config/DataInitializer`, only if no user exists:

| Username | Password | Role |
|---|---|---|
| `admin` | `ChangeMe123!` | ADMIN |
| `cashier` | `ChangeMe123!` | CASHIER |

⚠️ Development credentials. Change both before this leaves localhost.

Next: **milestone 3 (Auth backend)** — register/login/refresh/me, the JWT filter
and per-route roles. See [PROJECT-SPEC.md](PROJECT-SPEC.md) §5, §6 and §10.

### Installed versions

| | Version |
|---|---|
| Spring Boot | 3.5.6 (Java 17, Gradle 8.14.3) |
| Next.js | 16.3.1 (React 19.2.8) |
| Tailwind CSS | v4 — **CSS-first config, no `tailwind.config.ts`** |

## Related folders

| Path | What it is |
|---|---|
| `../Prototype-Restaurant-mgs` | HTML prototype — **the UI reference**, 23 screens |
| `../Restaurant-Management-System-NIEI-Y4-` | Existing Spring Boot API — auth, staff, mail |

## ⚠️ Before copying config from the old repo

`Restaurant-Management-System-NIEI-Y4-/src/main/resources/application.properties`
has the database password, JWT signing secret, and a Gmail app password committed
in plain text. Those credentials are in that repo's git history and should be
rotated. Use `application-local.yml` (gitignored) or environment variables here.
