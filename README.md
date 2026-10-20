# CSCI 602 — Foundations of Software Engineering

[![Java CI](https://github.com/CitadelCS/csci-602/actions/workflows/maven.yml/badge.svg)](https://github.com/CitadelCS/csci-602/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Template repository for CSCI 602 semester projects at The Citadel.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1 |
| Frontend | React (Vite) |
| Database | PostgreSQL (Flyway migrations) |
| Testing | JUnit 5, Cucumber (BDD), JaCoCo (coverage) |
| CI/CD | GitHub Actions |
| Deployment | Render (Static Site + Web Service + PostgreSQL) |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |

## Getting Started

### Prerequisites

- **Java 25** — Install Amazon Corretto 25
  - macOS: `brew install --cask corretto@25`
  - Windows: Download the `.msi` installer from [Amazon Corretto 25](https://docs.aws.amazon.com/corretto/latest/corretto-25-ug/downloads-list.html)
- **PostgreSQL** — You will be given credentials to a cloud-hosted database
- **Node.js 20+** — Required for the React frontend (`brew install node` or [nodejs.org](https://nodejs.org/))
- **IntelliJ IDEA** — Recommended IDE ([free student license](https://www.jetbrains.com/community/education/#students))

### 1. Create Your Repository

1. Go to the template repository: [CitadelCS/csci-602](https://github.com/CitadelCS/csci-602)
2. Click the green **"Use this template"** button → **"Create a new repository"**
3. Set the **Owner** to your personal GitHub account
4. Name the repository `csci-602` (or any name you prefer)
5. Set visibility to **Private**
6. Click **"Create repository"**
7. Go to **Settings → Collaborators** and add `jtravan3` as a collaborator

### 2. Clone and Build

```bash
git clone https://github.com/YOUR-USERNAME/csci-602.git
cd csci-602
```

> On Windows, replace `./mvnw` with `.\mvnw` in all commands below.

```bash
./mvnw clean compile
```

### 3. Configure Your Database

Update `src/main/resources/application.yaml` with the database credentials provided by your instructor:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://{host}:{port}/{database}
    username: {username}
    password: {password}
```

> **⚠️ Do not commit your database credentials to your repository.** Edit `application.yaml` locally but make sure you do not push these changes. Your credentials are unique to you.

### 4. Run the API

```bash
./mvnw spring-boot:run
```

The API starts on port 5001. Access Swagger UI at [http://localhost:5001/swagger-ui/index.html](http://localhost:5001/swagger-ui/index.html).

### 5. Run Tests

```bash
./mvnw test
```

This runs JUnit unit tests and Cucumber integration tests. JaCoCo coverage reports are generated at `target/site/jacoco/index.html`.

### 6. Frontend Setup (Iteration 0)

See `frontend/README.md` for instructions on scaffolding the React (Vite) frontend.

```bash
cd frontend
npm create vite@latest . -- --template react-ts
npm install
npm run dev
```

The React dev server starts on [http://localhost:5173](http://localhost:5173).

## Project Structure

```
├── .github/workflows/     # GitHub Actions CI pipeline
├── frontend/              # React (Vite) frontend (scaffold in Iteration 0)
├── render.yaml            # Render deployment blueprint
├── src/
│   ├── main/
│   │   ├── java/edu/citadel/
│   │   │   ├── api/           # REST controllers
│   │   │   ├── config/        # Spring configuration (SecurityConfig stub)
│   │   │   ├── dal/           # Data access layer (repositories, models)
│   │   │   └── main/          # Application entry point
│   │   └── resources/
│   │       ├── db/migration/  # Flyway SQL migrations
│   │       └── application.yaml
│   └── test/
│       ├── java/edu/citadel/
│       │   ├── bdd/           # Cucumber runner and step definitions
│       │   └── hw1/           # HW1 unit tests
│       └── resources/
│           └── features/      # Cucumber .feature files (Gherkin)
├── pom.xml                # Maven dependencies and build config
└── README.md
```

## Stub Files

The following files are included as starting points for specific assignments:

| File | Assignment |
|---|---|
| `src/main/java/edu/citadel/config/SecurityConfig.java` | Iteration 1 — JWT Authentication |
| `src/test/java/edu/citadel/bdd/StepDefinitions.java` | Iteration 1 — Cucumber Integration Tests |
| `frontend/README.md` | Iteration 0 — React Frontend Scaffold |
| `render.yaml` | Iteration 2 — Render Deployment |

## Useful Commands

| Command | Description |
|---|---|
| `./mvnw compile` | Compile the project |
| `./mvnw test` | Run all tests (JUnit + Cucumber) + generate coverage |
| `./mvnw spring-boot:run` | Start the API locally |
| `./mvnw clean install` | Full build + test |

## Resources

- [Spring Boot — Getting Started](https://spring.io/guides/gs/spring-boot/)
- [Spring Boot — Building REST Services](https://spring.io/guides/tutorials/rest)
- [Cucumber — Spring Integration](https://www.baeldung.com/cucumber-spring-integration)
- [React — Getting Started with Vite](https://vitejs.dev/guide/)
- [Maven in 5 Minutes](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
- [Render — Deployment Docs](https://render.com/docs)
- [JaCoCo — Code Coverage](https://www.jacoco.org/jacoco/trunk/doc/)

## Iteration 1: User Activity Queries

- `GET /users/{userId}/activities`: user history ordered by activity date descending, then ID descending.
- `GET /users/{userId}/activities/search?type=Running&from=2026-10-01&to=2026-10-10`: optional case-insensitive exact type and inclusive date filters. Either date may be omitted; no filters returns the user's history. Type whitespace is trimmed.
- `GET /users/{userId}/activity-types`: distinct recorded type strings for that user, alphabetically sorted. This is not a predefined exercise catalog.

Backlog mapping (numbers below are GitHub issue IDs):

| Endpoint | Backlog story |
|---|---|
| `GET /users/{userId}/activities` | [View Activity History](https://github.com/PalmettoBling/csci602_project/issues/12) |
| `GET /users/{userId}/activities/search` | [Filter Activity History](https://github.com/PalmettoBling/csci602_project/issues/17) |
| `GET /users/{userId}/activity-types` | [Select Exercise Type](https://github.com/PalmettoBling/csci602_project/issues/9), supporting reuse of previously recorded types |

Successful requests return `200`, including `[]` when there are no matches. Unknown users return `404`. Nonpositive or malformed user IDs, blank or overlength types (100 characters maximum), invalid `yyyy-MM-dd` dates, and reversed date ranges return `400`.

These queries use the existing PostgreSQL tables and JPA repositories. Every activity query is scoped to the selected user. Requests to `/users/{userId}/**` require a valid bearer JWT whose subject matches the path user ID.

`UserActivityEndpointsTest` exercises real PostgreSQL queries and HTTP behavior using MockMvc, with signed test JWTs so the production security filter remains enabled. Test resources provide a public, test-only signing key; it must never be used in deployed environments. Fixtures run in transactions and roll back. Run with `./mvnw test` using the configured database, or `./mvnw test -Dspring.profiles.active=ghci` against the CI PostgreSQL service.

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
