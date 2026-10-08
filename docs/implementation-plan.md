# TaskFlow MVP — Implementation Plan

## Context
`claude.md` is the complete product spec for **TaskFlow**: simple project management + Kanban + task tracking + IT-Manager visibility. The folder contains only that file, so this is a greenfield build of the full MVP (§40), done when the §45 Definition-of-Done workflow runs end to end. The user appended the stack (lines 1500–1564: Spring Boot modular monolith, React/Vite, PostgreSQL, JWT, STOMP real-time notifications) and chose **Spring Boot 4.1** over the specified 3.x, whose free support ended 2026-06-30.

## Decisions
**Stack (claude.md):** Java 21 · Spring Boot 4.1.1 (Spring MVC, Data JPA/Hibernate, Security + JWT, Bean Validation, WebSocket/STOMP) · PostgreSQL · Maven wrapper · JUnit 5 + Mockito — React 19 + TypeScript + Vite 8 · React Router 8 · TanStack Query 5 · React Hook Form + Zod 4 · Tailwind 4 · shadcn/ui · dnd-kit · Recharts 3 · Axios · `@stomp/stompjs` over plain WebSocket (SockJS not needed).

**Supporting tooling (not product features):** Flyway SQL migrations (`ddl-auto=validate`) · Testcontainers 2 (postgres:18) for integration tests · Spring Modulith 2.1 **test starter only** (`ApplicationModules.verify()` guards module boundaries) · springdoc-openapi 3 (Swagger UI, dev only) · spring-boot-starter-mail (password reset only) · the Vite template's oxlint + TypeScript ~6.0 · Vitest + Playwright · no Lombok (records for DTOs) · `.gitattributes` (LF, CRLF for `*.cmd`) · `git init` without commits.

**Defaults already approved by the user:**
1. Self-registration → `PROJECT_OWNER` (overridable via `APP_DEFAULT_SIGNUP_ROLE`); first IT Manager bootstrapped from env vars at startup.
2. Password reset: single-use 30-min token, emailed via SMTP when configured; otherwise the link is logged in the dev profile only (prod without SMTP logs a warning).
3. User deletion = deactivation (soft delete) with reactivation. Role/status changes are IT-only, need a confirm dialog + password re-entry, never target yourself, never remove the last IT Manager (row lock). The dialog warns when the user owns projects (they stay read-only until reactivation; ownership transfer would be a later addition).
4. Members move and edit only tasks assigned to them (detail fields + existing tags); they can't create, delete or (re)assign. Every edit lands in the activity log.
5. When someone other than the owner changes a task's status, the owner receives `TASK_STATUS_CHANGED`.

**Additions to approve with this plan:** User `status` (§31 Status column) and optional `jobTitle` (§8 member titles) · activity types `PROJECT_DELETED`, `USER_ROLE_CHANGED`, `USER_STATUS_CHANGED` (audit of deletions and privileged actions) · real-time push covers notifications only, no live board sync (§42 "real-time collaboration") · `APP_TIMEZONE` setting.

**Definitions (one constant each):** active task = TODO | IN_PROGRESS · overdue = due date < today and not DONE (Backlog included) · high priority = HIGH and not DONE · active user = status ACTIVE · active project = ≥1 active task · workload warning at ≥8 active or ≥3 overdue · "today" is computed server-side in `APP_TIMEZONE` (default UTC) and returned as `dueState` (NONE / UPCOMING / DUE_TODAY / OVERDUE / COMPLETED), so completed tasks never show as overdue · dates travel as `YYYY-MM-DD`.

## Repository layout
```
xdsdata_todo/
├─ claude.md            spec (untouched)
├─ README.md            setup, env vars, scripts, architecture, permission matrix
├─ .gitattributes  .gitignore
├─ backend/             Spring Boot 4.1 (mvnw, pom.xml, com.xdsdata.taskflow.*)
│  └─ src/main/resources/db/migration/   V1__schema.sql, V2__search.sql
└─ frontend/            Vite + React (src/app, api, features, components, lib, schemas; e2e/)
```

## Backend — modular monolith (`com.xdsdata.taskflow.<module>`)
Acyclic dependencies (users ← projects ← tasks ← activity / notifications ← dashboard / management / search). Public services, DTOs and events sit in each module's base package; controllers, repositories and internals in sub-packages. When a lower module needs data from a higher one, it defines an interface or listens to a domain event.

| Module | Responsibility |
|---|---|
| common | security config, `CurrentUser`, ProblemDetail errors, page/cursor envelopes, `Clock`, rate limiter, WebSocket config |
| users | User entity, profile, exact-email lookup |
| auth | register, login, refresh, logout, forgot/reset password, token stores, mail |
| projects | Project, ProjectMember, `ProjectAccess` (canRead / canManage), CRUD, members |
| tasks | Task, Tag, board query, create / update / move / assign / delete, per-task permissions, domain events |
| activity | ActivityLog, synchronous listeners in the same transaction, project feed |
| notifications | Notification, recipient rules, STOMP push after commit, deadline sweep, REST |
| dashboard · management · search | read models: user dashboard; IT overview, team activity, workload, logs, users admin, projects; search |

Responses are DTO JSON; lists are `{items, page, size, totalItems, totalPages}` or `{items, nextCursor}`. Errors are RFC 9457 ProblemDetail with `code` + `fieldErrors` (400, 401, 403, 404, 409, generic 500 with a request id; constraint violations become friendly 409s); the security entry point and access-denied handler use the same shape.

## Data model (Flyway; UUID PKs, `timestamptz`, enums as varchar + CHECK, an index on every FK column)
- **users** — name, email (unique, lowercased), password_hash (bcrypt), role, status, job_title?, avatar? (initials fallback, no uploads), timestamps
- **refresh_tokens** — user_id, token_hash unique, expires_at, revoked_at?, replaced_by? · **password_reset_tokens** — user_id, token_hash unique, expires_at, used_at? (expired rows purged by a scheduled job)
- **projects** — name, description?, owner_id, timestamps (updated_at touched by task changes)
- **project_members** — id, project_id (cascade), user_id, role OWNER | MEMBER, joined_at; unique(project_id, user_id), idx(user_id); owner row kept in sync with `owner_id`
- **tasks** — project_id (cascade), title, description?, status, priority (default MEDIUM), due_date `date`?, assignee_id (set null), position `double precision`, timestamps; idx (project_id, status, position, id), (assignee_id, status), (status, due_date)
- **tags** — project_id (cascade), name, color (fixed palette); unique(project_id, lower(name)) · **task_tags** — PK(task_id, tag_id)
- **notifications** — user_id, task_id?, project_id? (cascade), type, title, message, is_read, created_at, dedupe_key? unique; idx (user_id, is_read, created_at desc)
- **activity_logs** — user_id? / project_id? / task_id? (set null), action, description, metadata `jsonb` (always holds id + name snapshots), created_at; idx (created_at desc, id), (project_id, created_at), (user_id, created_at). Delete actions are logged before the row is deleted.
- **V2** — `pg_trgm` + GIN trigram indexes on task title/description, tag name, user name/email, project name, activity description.

## Authentication & security
- **Access token:** HS256 JWT, 15 min (`sub`, `role`), issued with Nimbus and validated by Spring's OAuth2 resource server; `JWT_SECRET` ≥ 256 bits. A converter reloads the user per request, so role changes and deactivation apply immediately. Stateless (no HTTP session).
- **Refresh token:** random 256-bit, `HttpOnly; SameSite=Strict; Path=/api/auth` cookie, `Secure` when served over HTTPS (`APP_COOKIE_SECURE`), 14 days, rotated with reuse detection, stored hashed. Logout, password change and reset revoke tokens.
- **Frontend:** access token in memory; app start calls `/api/auth/refresh`; the Axios interceptor adds the bearer token, does a single-flight refresh on 401, retries once, otherwise logs out. A `?next=` redirect only accepts relative paths.
- Passwords 8–72 characters (bcrypt limit); rate limits on login / forgot / reset; generic login error; reset never reveals whether an email exists; reset links built from `APP_FRONTEND_URL`, never the Host header.
- URL rules: `/api/auth/**` public, `/api/management/**` IT only (plus `@PreAuthorize`), everything else authenticated. Object-level checks run in services: task routes derive the project from the task; assignees and tag ids are validated against that project, so a tampered id yields 403.
- Origin allow-list from env (CORS, cookie endpoints, WebSocket); security headers; CSRF off (bearer tokens). Privacy (§33): only business actions are logged — no page views, clicks or keystrokes.

## Permissions (enforced in services; 403 when access is denied, 404 when the record doesn't exist)
| Action | IT Manager | Project owner | Project member | Others |
|---|---|---|---|---|
| Create project | ✓ | ✓ (global PROJECT_OWNER) | ✗ when global role is MEMBER | — |
| View project, board, members, overview, activity | ✓ read-only | ✓ | ✓ | 403 |
| Edit / delete project, add / remove members | only as owner | ✓ | ✗ | 403 |
| Create / delete / (re)assign tasks, create tags | only as owner | ✓ | ✗ | 403 |
| Edit or move a task | only as owner/assignee | ✓ any task | ✓ own assigned tasks | 403 |
| Management pages, global search, user role/status | ✓ | ✗ | ✗ | ✗ |

The server returns a `permissions` object per project and per task (canEdit, canMove, canDelete, canAssign) that the UI follows; the backend still enforces every rule. Members are added by **exact email** (active users only), so nobody can browse the staff directory. The assignee must be a project member. Removing a member unassigns their tasks in that project (logged + notified). The owner can't be removed.

## Business rules
- **Board:** four fixed columns. "+ Add Task" appears in Backlog / To Do / In Progress but not Done (§9 diagram); the generic "New task" defaults to Backlog; new tasks go to the bottom of the column.
- **Moving tasks:** `PATCH /tasks/{id}/move {status, previousTaskId?, nextTaskId?}`. The server validates the neighbours (same project and status) and places the task between them (gap 1024). When gaps get too small it renumbers the column with a bulk update that leaves `updated_at` untouched. A stale neighbour falls back to the end of the column. Cards order by `(position, id)`. The status select in the task sheet uses the same move service.
- **Done column:** shows the 50 most recent cards, with "Show more".
- **Activity log (same transaction):** PROJECT_CREATED / UPDATED / DELETED / MEMBER_ADDED / MEMBER_REMOVED · TASK_CREATED / UPDATED / DELETED / ASSIGNED / UNASSIGNED / STATUS_CHANGED / PRIORITY_CHANGED / DUE_DATE_CHANGED / COMPLETED · USER_ROLE_CHANGED / USER_STATUS_CHANGED. What gets logged:
  - A move into Done logs TASK_COMPLETED (metadata: old and new status).
  - Reordering within a column logs nothing.
  - An edit logs one row per specific change, plus TASK_UPDATED for title, description or tag changes.
  - An assignee chosen in the create form is logged and notified exactly like a later assignment.
  - Descriptions read like the §27 examples.
- **Notifications** go to the people below, never to the actor, never to deactivated users, and once per recipient per action:
  - TASK_ASSIGNED → new assignee.
  - TASK_UNASSIGNED → previous assignee. Reassigning from A to B sends both notices.
  - TASK_UPDATED (one per save, listing the changed fields) and TASK_STATUS_CHANGED → assignee.
  - TASK_STATUS_CHANGED → owner, when someone else moved the task.
  - TASK_DEADLINE_APPROACHING (due today or tomorrow) and TASK_OVERDUE → assignee.

  Deadline rules run right after a task is created or edited, and in a `@Scheduled` sweep every 15 min plus at startup. `dedupe_key = type:task:dueDate:user` prevents duplicates.
- **Real-time push:**
  - Notifications are saved inside the transaction and sent after commit with `convertAndSendToUser(userId, "/queue/notifications", {notification, unreadCount})`.
  - The STOMP endpoint is `/ws`. The client sends its JWT in the CONNECT frame, where a `ChannelInterceptor` validates it.
  - Clients can only SUBSCRIBE to `/user/queue/notifications`; SEND is rejected.
  - The client reconnects with a fresh token, updates the query cache, and shows a toast such as "🔔 You have been assigned to "Implement Login UI"".

## REST API (`/api`)
- **auth:** `POST register | login | refresh | logout | forgot-password | reset-password`, `GET me`
- **me:** `PATCH /users/me` (name, jobTitle only) · `POST /users/me/password` · `GET /users/lookup?email=` (owners + IT)
- **projects:** `GET, POST /projects` · `GET, PUT, DELETE /projects/{id}` · `GET /projects/{id}/board | overview | activity?cursor | members | tags` · `POST /projects/{id}/members {email}` · `DELETE /projects/{id}/members/{userId}` · `POST /projects/{id}/tags`
- **tasks:** `POST /projects/{id}/tasks` · `GET /projects/{id}/tasks?status=DONE&cursor` (Done paging) · `GET, PUT, DELETE /tasks/{id}` · `PUT /tasks/{id}/assignee` · `PATCH /tasks/{id}/move`
- `GET /dashboard` · **notifications:** `GET /notifications?cursor`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `POST /notifications/read-all` · `GET /search?q=` (≥2 chars; access scoped in SQL before LIMIT, tag join included)
- **management (IT only):** `GET overview | team-activity | workload | activity-logs | users | users/{id} | projects` (filters + pagination per §25/27/31) · `PATCH users/{id}/role`, `PATCH users/{id}/status` (both require `currentPassword`)

## Frontend
- **Routes:**
  - Public: `/login`, `/register`, `/forgot-password`, `/reset-password`.
  - Signed-in: `/dashboard`, `/projects`, `/projects/:id` (the board; `?task=` opens the task sheet), `/projects/:id/overview`, `/projects/:id/settings`, `/notifications`, `/profile`.
  - IT layout: `/management` plus `team-activity`, `workload`, `activity-logs`, `users`, `users/:id` and `projects`.
  - After login, IT Managers land on `/management` and everyone else on `/dashboard`. There are 403, 404 and network-error pages.
- **Shell:** top nav (Dashboard, Projects, Management for IT, Ctrl+K search, 🔔 badge + panel, avatar menu → Profile / Logout); drawer on mobile.
- **Dashboard (§6):**
  - The greeting is computed in the browser.
  - Tiles: Total, captioned "incl. N in Backlog" so the numbers add up, then To Do, In Progress and Completed.
  - Lists of up to 5 tasks each, excluding Done: high priority, due today, overdue.
  - Recent projects.
- **Board:**
  - Drag and drop uses dnd-kit's Mouse, Touch (200 ms press) and Keyboard sensors. Each column is a drop target, so empty columns accept drops too.
  - Moves are optimistic, with rollback and a toast if the server rejects them.
  - Layout: four columns on desktop, narrower on tablet, horizontal scroll on mobile with snap off while dragging.
  - Cards show priority (🔴🟡🟢), coloured tag badges, a due badge (📅 Due today / 📅 Due Oct 15 / ⚠️ Overdue / ✓ Completed) and the assignee's avatar.
  - Task details open in a side sheet, full screen on mobile.
- **IT pages:**
  - Overview: all 11 metrics from §24, plus widgets answering the five §45 questions (in progress now, overdue, workload warnings, recent activity), plus Recharts charts by status, priority and workload.
  - Team Activity: defaults to active statuses and has a "last updated" column.
  - Workload: includes members with zero tasks and uses neutral wording.
- **Shared:**
  - Reusable components: EmptyState, ErrorState, ConfirmDialog, skeleton loaders, DueBadge, PriorityBadge, TagBadge, UserAvatar.
  - API errors become toasts or field errors.
  - Zod schemas mirror the backend's limits (name 100, title 200, description 5000, tag 30).
  - Search is debounced by 300 ms.
- **Look:** light theme only, so the template's dark color-scheme CSS is removed. One distinctive accent colour (not Trello blue) and lucide icons. Load the `dataviz` skill before writing chart code.

## Build order (spec §43, grouped — each milestone must end green)
1. **Setup** — Initializr project (Boot 4.1.1, Java 21, `com.xdsdata:taskflow`; webmvc, security, oauth2-resource-server, data-jpa, flyway, postgresql, validation, websocket, mail, actuator, testcontainers + modulith test starter, springdoc), generated in the scratchpad and copied in; create-vite react-ts; Tailwind + shadcn; Vite proxy (`/api`, `/ws` → :8080); env handling; `.gitattributes`; git init. *Check:* `mvnw verify` + `npm run build`.
2. **Schema** — migrations, entities, repositories, IT-Manager bootstrap, demo seed (`APP_SEED_DEMO=true`: the spec's example users, projects and tasks, including overdue ones). *Check:* app boots on PostgreSQL 18, Flyway applies, Hibernate validate passes, LocalDate round-trip test.
3. **Auth + RBAC** — auth endpoints, security config, error handling, auth pages, guards, interceptor. *Check:* integration tests for refresh rotation and reuse, single-use reset with revocation, and deactivated-user blocking. A sweep test calls every registered endpoint: without a token each must return 401 (except the public allowlist), and every `/api/management/**` endpoint must return 403 for a non-IT user.
4. **Users, projects, members**, plus the activity service so every mutation is logged from the start. *Check:* role × action × membership matrix tests; id tampering → 403; member removal unassigns.
5. **Tasks + Kanban** — CRUD, tags, priorities, due dates, assignment, board, move, task sheet, notification persistence with the recipient rules. *Check:* unit tests for position math, due state and recipients; integration tests for move and assign; a drag survives a reload; a member can't move someone else's task.
6. **Notification delivery** — STOMP push, deadline sweep, bell, panel, page. *Check:* running the sweep twice creates no duplicates; a second browser gets a toast without refreshing.
7. **Dashboards + IT pages** — user dashboard, overview, team activity, workload, activity logs, users admin, project monitoring. *Check:* exact counts against a fixture; the IT pages answer the five §45 questions.
8. **Search** — trigram task search, IT global search, command palette. *Check:* "API" matches title, description and tag; members never see other projects' tasks, including via tags.
9. **Hardening** — responsive pass (375 / 768 / 1280 px), empty / loading / error states on every page, security review (no sensitive DTO fields, rate limits, headers), EXPLAIN on overview / team-activity / search queries, Modulith verify, README. *Check:* the full verification below.

## Risks → mitigations
- Jackson 3 (`tools.jackson`) in Boot 4 may not plug into Hibernate's JSON mapping → keep `metadata` as a JSON string mapped to `jsonb`.
- Boot 4 modular starters (confirmed from Initializr 4.1.1): `spring-boot-starter-webmvc`, `-security-oauth2-resource-server`, `-flyway` + `flyway-database-postgresql`, per-module `*-test` starters, `testcontainers-postgresql`; use `@MockitoBean`.
- Flyway may warn about PostgreSQL 18 → accept warnings; bump Flyway only on errors.
- `@EnableWebSocketSecurity` demands CSRF tokens on CONNECT → authorize in our own interceptor instead.
- npm `latest` TypeScript is 7.x → keep the template's `~6.0`. React Router 8, Vite 8 and Recharts 3 are newer than most examples → check the installed type definitions; Recharts needs `react-is`.
- dnd-kit's PointerSensor sets `touch-action: none`, which breaks mobile scrolling → Mouse + Touch sensors.
- Windows: quote Maven `-D` args and paths ("Projects Moise" has a space); `core.autocrlf=true` → `.gitattributes`.
- Testcontainers needs Docker Desktop (currently stopped) → tests use `disabledWithoutDocker = true`; I'll start Docker Desktop to run them and report anything skipped.

## Prerequisites (raised at milestone 2)
- PostgreSQL 18 on localhost:5432 needs a role owning databases `taskflow` (dev) and `taskflow_e2e` (Playwright). I can create them with `psql` if you give me the postgres password, or you can run the SQL I provide. Credentials live in `backend/.env` (git-ignored, loaded via `spring.config.import`). If you'd rather not share credentials, a docker-compose PostgreSQL on port 5433 works instead.
- Dev `JWT_SECRET` and the bootstrap IT-Manager credentials get generated into `backend/.env`; `.env.example` documents every variable.

## Verification
1. `backend\mvnw.cmd verify` runs:
   - Mockito unit tests: notification rules, positions, policies, workload.
   - Testcontainers + MockMvc integration tests: auth, the endpoint security sweep, the permission matrix, id tampering, activity and notification side effects, management metrics.
   - The Modulith verification.
2. In `frontend`: `npm run lint`, `npm run typecheck`, `npm run test`, `npm run build`.
3. `npm run e2e`: Playwright starts the backend against `taskflow_e2e` plus the frontend, and walks every §45 step with three browser contexts:
   - The owner registers, logs in, creates a project, adds a member, creates a task (it lands in Backlog) and assigns it.
   - The member gets a real-time notification, then moves the task to To Do and then In Progress.
   - The IT Manager sees it in Team Activity and Workload.
   - The member moves it to Done. TASK_COMPLETED appears in Activity Logs and the owner is notified.

   Each run uses unique data, so nothing needs resetting.
4. Manual check with the `run` skill: both apps running with the demo seed, each role's views, mobile and tablet layouts, screenshots.
