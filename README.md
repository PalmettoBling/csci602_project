# CSCI 602 Semester Project

A fitness-activity tracker with a Spring Boot REST API and a React web client. Users can register and log in, then view their activity history.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1 |
| Frontend | React 19, TypeScript, Vite 8 |
| Database | PostgreSQL, with schema migrations managed by Flyway |
| Authentication | Stateless JWT bearer tokens |
| API documentation | SpringDoc OpenAPI / Swagger UI |
| Tests | JUnit 5, Cucumber, JaCoCo |

## Prerequisites

- Java 25
- PostgreSQL
- Node.js 20.19+ or 22.12+ and npm (for manual frontend startup)

The Maven wrapper (`mvnw` / `mvnw.cmd`) downloads and uses the Maven version configured for this project; a separate Maven installation is not required.

## Run locally

### 1. Start PostgreSQL

The backend's local defaults expect a database named `csci602` on `localhost:5432`, with username and password `postgres`. Create that database and use those credentials, or set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` to match your PostgreSQL setup. Flyway applies the database migrations automatically when the backend starts.

### 2. Configure the JWT signing key

The backend requires `JWT_SECRET` to contain at least 32 bytes of random data. Do not commit the key or use a test-only key in a deployed environment.

For a development shell, generate a key with OpenSSL:

```sh
export JWT_SECRET="$(openssl rand -hex 32)"
```

In PowerShell:

```powershell
$env:JWT_SECRET = (openssl rand -hex 32)
```

These settings apply only to the current shell session.

### 3. Start the backend

From the repository root:

```sh
./mvnw spring-boot:run
```

On Windows PowerShell, run `.\mvnw.cmd spring-boot:run`.

The API listens on http://localhost:5001.

Swagger UI is at http://localhost:5001/swagger-ui/index.html; health and application information are available at `/health` and `/info`.

### 4. Start the frontend

In a second terminal:

```sh
cd frontend
npm ci
npm run dev
```

On Windows PowerShell, the same commands work. Open the Vite development URL printed in the terminal (normally http://localhost:5173). The Vite server proxies API requests to `http://localhost:5001`, so run the backend as well.

For a frontend build served from a different host, set `VITE_API_BASE_URL` at build time to the API's base URL; the local development proxy is not included in production builds.

### 5. Windows Local Launcher (Alternative)

If you have the ignored `run-local.local.ps1` helper and `src/main/resources/application-local.yaml` configured with your PostgreSQL credentials, run this from PowerShell:

```powershell
.\run-local.local.ps1
```

The helper supplies the local Spring profile and JWT secret, then invokes the Maven wrapper with the Windows `local-run` Maven profile:

```powershell
.\mvnw.cmd -Plocal-run clean verify exec:exec@frontend-dev spring-boot:run
```

Maven installs its own Node/npm under `target/frontend-tools`, restores frontend dependencies, runs frontend lint/build and all backend tests, then runs Vite and the API in the same terminal. No system Node/npm or extra terminal is needed.

Open http://localhost:5173 after both servers report ready. Ctrl+C stops both. Ports 5001 and 5173 must be free.

The configured PostgreSQL database must be reachable; neither Maven nor the helper provisions it. There is no frontend unit-test script, so frontend checks are lint and build. The Maven profile includes the main application configuration for tests, avoiding the minimal test configuration hiding required settings.

Additional launcher commands:

```powershell
.\run-local.local.ps1 -TestOnly       # Checks only; do not start servers
.\run-local.local.ps1 -MavenGoal test # Backend tests only
```

The launcher activates the local Spring profile and generates a temporary JWT secret if none is provided. Use a persistent `JWT_SECRET` environment variable to keep login tokens valid across restarts.

Keep the launcher and local database profile untracked; neither should contain committed credentials.

## Application features and API

- `POST /auth/register` creates an account; `POST /auth/login` returns a JWT.
- `GET /users/{userId}/activities` lists a user's activities, newest first.
- `GET /users/{userId}/activities/search` accepts optional `type`, `from`, and `to` filters. Dates use `yyyy-MM-dd` and are inclusive.
- `GET /users/{userId}/activity-types` returns the activity types recorded for that user.
- `/users/{userId}/**` requires a bearer JWT for that same user. Other protected API routes also require a valid bearer token.

For example, `GET /users/42/activities/search?type=Running&from=2026-10-01&to=2026-10-10` filters user 42's history by activity type and date range.

## Build and test

From the repository root:

```sh
./mvnw clean compile
./mvnw test
```

On Windows, replace `./mvnw` with `.\mvnw.cmd`.

The integration tests use PostgreSQL; the default Spring configuration connects to `localhost:5432/csci602`, while the `ghci` profile uses `localhost:5433/testdb` (username/password `admin`/`admin`).

Set `SPRING_PROFILES_ACTIVE=ghci` when running the full test suite against that profile's database.

A database-independent activity HTTP contract test can be run with:

```sh
./mvnw -Dtest=UserActivityEndpointsUnitTest test
```

JaCoCo writes its report to `target/site/jacoco/index.html`.

Frontend checks:

```sh
cd frontend
npm run lint
npm run build
```

## Project layout

```text
src/main/java/edu/citadel/       Spring Boot API, security, and persistence
src/main/resources/db/migration/ Flyway SQL migrations
src/test/                        JUnit tests and Cucumber feature tests
frontend/                        React/Vite application
render.yaml                      Render deployment blueprint
pom.xml                          Maven build and backend dependencies
```

## Deployment

`render.yaml` describes the Render services for the Spring Boot API, static frontend, and PostgreSQL database. Configure a strong `JWT_SECRET` for the API service in the deployment environment.

For deployment and hosting configuration, see the [Render documentation](https://render.com/docs).
