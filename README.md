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
