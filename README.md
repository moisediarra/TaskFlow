# TaskFlow

Simple project management for teams: projects, a four-column Kanban board, task assignment, deadlines and in-app notifications. IT Managers also get a management area that answers **who is working on what, what is in progress, what is overdue, who has too many active tasks, and what changed recently**.

The product specification is [`claude.md`](claude.md). The implementation plan and the decisions behind it are in [`docs/implementation-plan.md`](docs/implementation-plan.md).

| | |
|---|---|
| Backend | Java 21 · Spring Boot 4.1 (MVC, Data JPA / Hibernate 7, Security, WebSocket / STOMP, Validation) · PostgreSQL 18 · Flyway · Maven |
| Frontend | React 19 · TypeScript · Vite · React Router · TanStack Query · React Hook Form + Zod · Tailwind CSS 4 · shadcn/ui · dnd-kit · Recharts · Axios |
| Tests | JUnit 5 · Mockito · Testcontainers · Spring Modulith · Vitest · Playwright |

## Run it with Docker

Requirements: Docker Desktop (or Docker Engine with Compose v2).

```bash
cp .env.example .env        # then replace every "change-me" (see Configuration)
docker compose up -d --build
```

Open <http://localhost:8090> and sign in with `APP_BOOTSTRAP_ADMIN_EMAIL` / `APP_BOOTSTRAP_ADMIN_PASSWORD`. That IT Manager account is created at the first start. New people register themselves at `/register`.

To get a populated app on the first start, set `APP_SEED_DEMO=true` and choose an `APP_SEED_DEMO_PASSWORD` (at least 8 characters) before the first `up`. Demo data is only created when the database has no projects yet. It contains the people from the spec (John Doe, Sarah Smith, Mohammed Ali, David Martin and Emma Wilson, all `firstname.lastname@taskflow.local`), three projects, and tasks in every column, some of them overdue.

The stack has three containers:

| Service | What it does | Port |
|---|---|---|
| `frontend` | nginx serving the built app and proxying `/api` and `/ws` to the backend | `APP_PORT` (8090) |
| `backend` | Spring Boot API, non-root, healthcheck on `/actuator/health` | internal 8080 |
| `db` | PostgreSQL 18, data in the `db-data` volume | `127.0.0.1:DB_PORT` (5433) |

`docker compose down` stops the stack and keeps the data. `docker compose down -v` also deletes the database.

If Docker reports `ports are not available ... bind: An attempt was made to access a socket in a way forbidden by its access permissions`, Windows has reserved that port. You can list the reserved ranges with `netsh interface ipv4 show excludedportrange protocol=tcp`. Choose another port by setting `APP_PORT` and `APP_PUBLIC_URL` in `.env`, then run `docker compose up -d` again.

## Local development

Requirements: JDK 21, Node.js 24, and Docker (for the database and the backend tests).

1. **Database.** Start PostgreSQL from the compose file: `docker compose up -d db`. It listens on `127.0.0.1:5433` and creates the `taskflow` database, plus `taskflow_e2e` for end-to-end tests.
2. **Backend.** Create `backend/.env` (git-ignored, loaded automatically):

   ```properties
   DB_URL=jdbc:postgresql://localhost:5433/taskflow
   DB_USERNAME=taskflow
   DB_PASSWORD=<POSTGRES_PASSWORD from the root .env>
   JWT_SECRET=<random string, at least 32 characters>
   APP_BOOTSTRAP_ADMIN_EMAIL=it.manager@taskflow.local
   APP_BOOTSTRAP_ADMIN_PASSWORD=<choose one>
   APP_SEED_DEMO=true
   APP_SEED_DEMO_PASSWORD=<choose one>
   APP_LOG_RESET_LINKS=true
   APP_API_DOCS_ENABLED=true
   ```

   Then run it:

   ```bash
   cd backend
   ./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
   ```

   The API listens on <http://localhost:8080>. With `APP_API_DOCS_ENABLED=true`, Swagger UI is at `/swagger-ui.html`.
3. **Frontend.**

   ```bash
   cd frontend
   npm ci
   npm run dev
   ```

   Open <http://localhost:5173>. Vite proxies `/api` and `/ws` to `VITE_BACKEND_URL` (default `http://localhost:8080`).

Password reset sends an email when SMTP is configured (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `APP_MAIL_FROM`). Without SMTP, `APP_LOG_RESET_LINKS=true` prints the link to the backend log. Use that in development only.

## Configuration

The backend reads environment variables, or `backend/.env` when you run it locally. Docker Compose reads the root `.env` and passes the values on.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/taskflow`, `taskflow`, empty | Database connection (backend). |
| `POSTGRES_PASSWORD` | required | Database password (Compose). |
| `JWT_SECRET` | required | HMAC key for access tokens, at least 32 characters. |
| `APP_PUBLIC_URL` | `http://localhost:8090` | Public URL (Compose). It becomes `APP_FRONTEND_URL` and `APP_ALLOWED_ORIGINS`. |
| `APP_FRONTEND_URL` | `http://localhost:5173` | Base URL for password-reset links. |
| `APP_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated origins allowed for CORS, cookie endpoints and WebSocket. |
| `APP_COOKIE_SECURE` | `false` | Set to `true` behind HTTPS, so the refresh cookie is `Secure`. |
| `APP_TIMEZONE` | `UTC` | Time zone used to decide "due today" and "overdue". |
| `APP_DEFAULT_SIGNUP_ROLE` | `PROJECT_OWNER` | Role given to self-registered accounts (`PROJECT_OWNER` or `MEMBER`). |
| `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD`, `APP_BOOTSTRAP_ADMIN_NAME` | empty, empty, `IT Manager` | First IT Manager, created at startup if missing. |
| `APP_SEED_DEMO`, `APP_SEED_DEMO_PASSWORD` | `false`, empty | Demo data on an empty database. |
| `APP_LOG_RESET_LINKS` | `false` | Log reset links when no SMTP server is configured (development only). |
| `APP_API_DOCS_ENABLED` | `false` | Expose OpenAPI and Swagger UI. |
| `APP_RATE_LIMIT_ENABLED` | `true` | Rate limits on login, forgot-password and reset. |
| `APP_DEADLINE_SWEEP_ENABLED` | `true` | Deadline and overdue notifications, checked every 15 minutes. |
| `APP_PORT`, `DB_PORT` | `8090`, `5433` | Host ports (Compose). |

## Tests

```bash
# Backend: unit tests, integration tests on PostgreSQL (Testcontainers, needs Docker), module boundaries
cd backend && ./mvnw verify

# Frontend: type-check, lint, unit tests, production build
cd frontend && npm run typecheck && npm run lint && npm test && npm run build

# End to end (Playwright). The app must be running, either as dev servers or as the Docker stack.
cd frontend
npx playwright install chromium        # once
E2E_IT_EMAIL=it.manager@taskflow.local E2E_IT_PASSWORD=... npm run e2e
# Against Docker: add E2E_BASE_URL=http://localhost:8090
```

What the tests cover:

- **`definition-of-done.spec.ts`** walks the §45 workflow in three browser sessions (owner, intervenant and IT Manager):
  - Register, log in, create a project, add a member.
  - Create a task in Backlog and assign it. The intervenant gets a real-time notification without reloading.
  - The intervenant moves the task through To Do and In Progress. The IT Manager sees it in Team Activity.
  - The intervenant completes it. The activity log records it and the owner is notified.
- **`board-drag.spec.ts`** checks that a drag and drop survives a reload.
- **`responsive.spec.ts`** opens every page, including the task sheet and the IT Manager pages, at 375, 768 and 1280 px. It fails if a page scrolls sideways and saves screenshots to `frontend/test-results/`.
- The workflow and drag tests also fail on any Content-Security-Policy violation. Only the nginx build sends a CSP, so run them against Docker to check it.
- **Backend integration tests:**
  - A sweep over every endpoint: 401 without a token, and 403 on `/api/management/**` for anyone who isn't an IT Manager.
  - The role × membership permission matrix, plus tampered IDs.
  - Refresh-token rotation and reuse detection, single-use reset tokens.
  - Activity and notification side effects, deadline deduplication, search scoping and management metrics.

Each E2E run creates its own users and projects, so nothing needs resetting.

## Roles and permissions

Authorization runs in the backend services. The UI only hides what the backend would refuse anyway. A denied action returns **403**, a missing record **404**, and changing an ID in a URL never reveals another project's data.

| Action | IT Manager | Project owner | Project member | Others |
|---|---|---|---|---|
| Create a project | ✓ | ✓ | ✗ when the global role is MEMBER | — |
| View a project, its board, members, overview and activity | ✓ (read-only) | ✓ | ✓ | 403 |
| Edit or delete a project, add or remove members | only as owner | ✓ | ✗ | 403 |
| Create, delete or (re)assign tasks, create tags | only as owner | ✓ | ✗ | 403 |
| Edit or move a task | only as owner or assignee | ✓ any task | ✓ tasks assigned to them | 403 |
| Management pages, global search, user role and status | ✓ | ✗ | ✗ | ✗ |

- **Self-registration.** New accounts get `PROJECT_OWNER` by default.
- **Changing a user's role or status:**
  - Only an IT Manager can do it.
  - It needs their current password and a confirmation.
  - Nobody can change their own role or status, and the last IT Manager can't be removed.
- **Deleting a user** deactivates the account (it can be reactivated). A deactivated user is signed out at their next request.

## How it works

**Backend.** A modular monolith in `com.xdsdata.taskflow`. Each module exposes services, DTOs and events from its base package and keeps controllers, repositories and internals in sub-packages. Spring Modulith verifies the boundaries in the test suite.

| Module | Responsibility |
|---|---|
| `common` | Security configuration, current user, RFC 9457 error responses, paging, rate limiting, WebSocket configuration |
| `users` | Accounts and profiles |
| `auth` | Register, login, refresh, logout, password reset |
| `projects` | Projects, members, project access checks |
| `tasks` | Tasks, tags, the board, moves and assignment, demo data |
| `activity` | Activity log, written in the same transaction as the change |
| `notifications` | Recipient rules, deadline sweep, real-time push |
| `dashboard`, `management`, `search` | Read models for the user dashboard, the IT Manager area and search |

- **Authentication:**
  - The access token is a 15-minute HS256 JWT, kept in memory by the frontend.
  - The refresh token is a rotating 14-day token in an `HttpOnly; SameSite=Strict` cookie scoped to `/api/auth`. It is stored hashed, and reusing an old token is detected.
  - The user is reloaded on every request, so a role change or deactivation applies immediately.
- **Real-time notifications:**
  - Notifications are saved in the same transaction as the action.
  - After commit they are pushed over STOMP (`/ws`, `/user/queue/notifications`).
  - The client authenticates the STOMP connection with its access token.
  - Only notifications are real-time. Boards refresh on navigation.
- **Kanban ordering.** Cards have floating-point positions. A move sends the card's new neighbours, and the server places it between them. The server renumbers a column only when the gaps get too small.
- **Database.** Flyway migrations live in `backend/src/main/resources/db/migration`. Hibernate only validates the schema. Search uses `pg_trgm` indexes.
- **Privacy (§33).** Only business actions are logged: project, member, task and user-administration changes. Page views, clicks and anything outside the app are never recorded.

**Frontend.** Code is organised by feature:

- `src/features/*`: auth, board, tasks, projects, dashboard, notifications, management, search, profile.
- `src/api`: Axios client with single-flight token refresh, plus the typed endpoints.
- `src/components`: shared UI and shadcn/ui.

Signed-in pages load on demand. Board moves are optimistic and roll back if the server refuses them.

## Project layout

```
├─ claude.md                 product specification
├─ docker-compose.yml        db + backend + frontend
├─ .env.example              Compose settings (copy to .env)
├─ docker/postgres/init/     creates the taskflow_e2e database
├─ docs/                     implementation plan
├─ backend/                  Spring Boot application (Dockerfile, pom.xml, src/)
└─ frontend/                 React application (Dockerfile, nginx.conf, src/, e2e/)
```

Not in the MVP, by design (spec §41): Gantt charts, calendars, custom columns, comments, attachments, time tracking, SSO, email or push notifications, and dark mode.
