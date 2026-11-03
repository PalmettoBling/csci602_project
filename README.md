
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

### Activity CRUD

`/users/{userId}/activities` is the **owner-scoped** route set: the `{userId}` in the
path is the only thing that decides whose data is read or written, and it is checked
against the token subject before the request reaches a controller. A request body
cannot name an owner, so one user can never read or change another user's activities.

The older `/activities/**` routes still accept any valid token regardless of who owns
the row. They are kept for the clients already built against them and are being
hardened on a separate follow-up branch; prefer the owner-scoped routes below for new
work.

| Method | Path | Body | Success | Errors |
| --- | --- | --- | --- | --- |
| `GET` | `/users/{userId}/activities` | — | `200` + array, newest first | `400` invalid `userId`, `401`, `403`, `404` unknown user |
| `POST` | `/users/{userId}/activities` | activity | `201` + activity, `Location` header | `400` invalid body, `401`, `403`, `404` unknown user |
| `PUT` | `/users/{userId}/activities/{activityId}` | activity | `200` + updated activity | `400` invalid body, `401`, `403`, `404` unknown activity |
| `DELETE` | `/users/{userId}/activities/{activityId}` | — | `204`, empty body | `401`, `403`, `404` unknown activity |

Request body for `POST` and `PUT`:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `activityType` | string | yes | trimmed, 1–100 characters |
| `durationMinutes` | integer | yes | 1–1440 |
| `activityDate` | string | yes | `yyyy-MM-dd`, from `1900-01-01` up to and including today |

There is no owner field. A `userId` or `user` sent in the body is ignored.

```sh
# Create
curl -i -X POST http://localhost:5001/users/42/activities \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"activityType":"Running","durationMinutes":30,"activityDate":"2026-10-26"}'
# HTTP/1.1 201
# Location: /users/42/activities/7
# {"id":7,"activityType":"Running","durationMinutes":30,"activityDate":"2026-10-26"}

# Update
curl -X PUT http://localhost:5001/users/42/activities/7 \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"activityType":"Hiking","durationMinutes":95,"activityDate":"2026-10-27"}'
# {"id":7,"activityType":"Hiking","durationMinutes":95,"activityDate":"2026-10-27"}

# Delete
curl -i -X DELETE http://localhost:5001/users/42/activities/7 \
  -H "Authorization: Bearer $TOKEN"
# HTTP/1.1 204
```

`PUT` and `DELETE` answer `404 Activity not found` both for an id that does not exist
and for one that belongs to somebody else — the response deliberately does not reveal
which, so it cannot be used to discover another account's activity ids.

Validation failures and malformed JSON return `400` in the shared error envelope:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "The request body is missing required fields or contains invalid values.",
  "fields": { "durationMinutes": "durationMinutes must be at least 1" }
}
```

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

For database-independent HTTP contract checks, run `./mvnw -Dtest=UserActivityEndpointsUnitTest test`. To run the JUnit integration tests without Cucumber discovery, use `./mvnw -Dsurefire.excludeJUnit5Engines=cucumber test`; PostgreSQL must be available at the datasource configured for the active profile.

## Iteration 2: Historical Activity Weather

Completed activities can be associated with **hourly historical estimates** from
[Open-Meteo](https://open-meteo.com/en/docs/historical-weather-api). These are
gridded reanalysis estimates, not exact minute-by-minute on-site observations.
No provider API key is required for the non-commercial educational use described
by this project; review the provider's terms before commercial deployment.

On **Your Activities**, choose **Attach historical weather**, then enter the
activity's local time, the UTC offset in effect at that location/date (including
daylight-saving time), latitude, and longitude. The browser does not call the
weather provider or request geolocation permission. Existing date-only activities
remain supported and display "No weather attached" until details are supplied.

### API contract

`POST /users/{userId}/activities/{activityId}/weather` requires a bearer JWT for
the path's user and an activity owned by that user:

```json
{
  "latitude": 32.78,
  "longitude": -79.93,
  "occurredAt": "2026-10-01T14:35:00-04:00"
}
```

The local date must equal the saved activity date. Future timestamps and dates
before the archive begins in 1940 are rejected. The backend converts the timestamp
to UTC and selects the hourly sample containing that instant (18:00 UTC in this
example). Explicit Celsius, millimeter, and km/h units are used.

A successful `200` response contains coordinates, `localTime`, `utcOffset`,
`sampledAt` (UTC), `temperatureC`, `precipitationMm`, `windSpeedKmh`,
`weatherCode` (WMO), and `fetchedAt` (UTC). Flyway migration V5 adds a nullable
embedded snapshot to the activity row. Activity history and existing activity
responses include that snapshot as `weather`; rendering history does not make
another provider call. Repeating the same attachment reuses the stored result.
Changing the supplied time/location replaces it only after a successful lookup.
Changing the activity date through the update endpoint clears stale weather.

Errors use the shared `{status, error, message, fields}` shape:

| Status | Meaning |
|---|---|
| `400` | Missing/invalid coordinates, timestamp, IDs, or mismatched activity date |
| `401` / `403` | Missing/invalid JWT or JWT belonging to another path user |
| `404` | Activity missing or not owned by the path user |
| `422` | No historical sample available; recent dates may take several days |
| `502` | Provider failure, timeout, or malformed response |

Missing measurements are never substituted with zero. Failed lookups preserve any
previous snapshot. The HTTP client uses a 3-second connection timeout and
8-second read timeout. `weather.archive-url` may override the provider origin
(tests point it to an in-process HTTP fixture).

### Tests and coverage

JUnit tests cover provider HTTP behavior, UTC conversion/date boundaries, malformed
and missing data, validation, caching, failures, and date-change invalidation.
Cucumber scenarios exercise real PostgreSQL persistence, history serialization,
cache reuse, JWT access rules, and invalidation without live provider requests.

Run the complete suite against PostgreSQL:

```powershell
.\mvnw.cmd test "-Dspring.profiles.active=ghci"
```

The `ghci` profile expects `testdb` on localhost:5433 with test-only credentials
`admin`/`admin`. To use a dedicated local database on a different port:

```powershell
.\mvnw.cmd test "-Dspring.profiles.active=ghci" "-Dspring.datasource.url=jdbc:postgresql://localhost:55433/testdb" "-Dspring.datasource.username=admin" "-Dspring.datasource.password=admin"
```

`mvnw test` generates `target/site/jacoco/index.html` and enforces **70% overall
JaCoCo instruction coverage** (JaCoCo's bytecode-level statement coverage metric).
No application packages are excluded from this threshold. GitHub Actions runs
JUnit/Cucumber against its PostgreSQL service, uploads the HTML/XML/CSV coverage
artifact, and builds/lints the frontend. A deliberately partial test run can skip
the aggregate coverage gate with `-Djacoco.skip=true`.

### Render deployment

The Blueprint defines a Docker-based Spring Boot Web Service, a Vite Static Site,
and PostgreSQL, with both services auto-deploying from **main**. Render does not
provide a native Java runtime; `Dockerfile` builds and runs Java 25. Local app
development does not require Docker when PostgreSQL is otherwise available.

1. In Render, create a Blueprint from this GitHub repository and review the
   resources and pricing. The free PostgreSQL plan has lifecycle/usage limits;
   use a suitable paid plan if long-term persistence is needed.
2. Supply `FRONTEND_ORIGIN` on the backend (the actual Static Site HTTPS origin,
   no trailing slash) and `VITE_API_BASE_URL` on the frontend (the actual Web
   Service HTTPS origin, no trailing slash). Update them if Render assigns names
   with suffixes or a custom domain is used, then redeploy both services.
3. The Blueprint generates `JWT_SECRET` and injects database host, port, database,
   username, and password. The `render` Spring profile constructs a JDBC URL
   instead of using Render's non-JDBC connection URI and binds to Render's `PORT`.
   Do not enable the test/ghci profiles in production or replace the generated
   JWT secret with the public test keys.
4. Check `/health`, register/login through the public frontend, and attach weather
   to an older completed activity. Reload history to confirm persistence. Use a
   reviewed pull request into main to trigger subsequent automatic deployments.

These files prepare deployment; they do not create Render resources or establish
a public URL by themselves. Record the actual deployed URLs in the scrum report
after configuring Render. The retrospective, Project board, code review, scrum
report, and peer evaluations remain team deliverables.

For deployment and hosting configuration, see the [Render documentation](https://render.com/docs).
