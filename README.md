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

```bash
docker compose up -d db
```

```bash
cd backend && ./gradlew bootRun
```

```bash
cd frontend && npm install && npm run dev
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8081/api |
| Swagger | http://localhost:8081/swagger-ui.html |

## Status

Nothing is scaffolded yet — `PROJECT-SPEC.md` is the plan. Milestone 1 is
"Scaffold": Gradle project, Next project, docker-compose, health check green.

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
