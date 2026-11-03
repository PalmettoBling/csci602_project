# Changelog

## Unreleased

- Add user activity history (`GET /users/{userId}/activities`), ordered by descending activity date and ID, for the View Activity History backlog story.
- Add activity search (`GET /users/{userId}/activities/search`) with optional case-insensitive exercise type and inclusive date filters for the Filter Activity History backlog story.
- Add distinct previously recorded exercise types (`GET /users/{userId}/activity-types`) to support the Select Exercise Type backlog story.
- Return HTTP 200 for successful reads, HTTP 400 for invalid inputs, and HTTP 404 for nonexistent users.
- Add PostgreSQL integration and HTTP contract tests, including regression coverage for existing CRUD and verification endpoints.
- Add owner-scoped activity writes for the Add, Edit and Delete Activity backlog stories: `POST /users/{userId}/activities` (201, with a `Location` header), `PUT /users/{userId}/activities/{activityId}` (200) and `DELETE /users/{userId}/activities/{activityId}` (204).
- Derive the owner of an activity from the path `userId` alone. The write request body has no owner field, so a `userId` or `user` supplied in the body is ignored and an edit can never reassign a row to another account.
- Return `404 Activity not found` from `PUT` and `DELETE` both for an unknown activity id and for one owned by another user, so the response cannot be used to discover another account's activity ids.
- Validate write requests with Bean Validation: `activityType` trimmed to 1-100 characters, `durationMinutes` between 1 and 1440, and `activityDate` a `yyyy-MM-dd` value between `1900-01-01` and today. Invalid bodies and malformed JSON return `400` in the existing `ApiError` envelope with per-field messages.
- Answer the write routes with a flat activity DTO (`id`, `activityType`, `durationMinutes`, `activityDate`) instead of the entity, so a response no longer nests the owning user. The existing read routes keep their current shape.
- Add an add/edit/delete activity UI to the React activities page: a validated form reused for both adding and editing, a two-step delete confirmation, in-place list updates without a reload, and loading, empty and error states.
- Add 59 JUnit tests and 12 Cucumber scenarios covering the new routes, including cross-user isolation, 401, 403, and the ignored body-supplied owner id.

### Security hardening

Removed, because every one of these exposed data across account boundaries. All
of them now answer `401` to an anonymous caller and `404` to an authenticated one.

- Remove `POST /account`, `GET /account/{id}` and `GET /account/username/{username}`. These were on the public allowlist, stored the password in plaintext, and returned it in the response body — any anonymous caller could read any account's password by id or username. `POST /auth/register` and `POST /auth/login` replace them. The `Account` entity, `AccountRepository` and `AccountRequestBody` are removed with them.
- Remove `GET /activities`, which served every activity of every user — including each owner's name and email — to any valid token.
- Remove `GET`, `PUT` and `DELETE /activities/{id}`, which performed no ownership check: any valid token could read, edit or delete any user's row.
- Remove `POST /activities`, which took the owner from the request body, letting any token create rows owned by another user. `ActivityRequestBody`, which carried that owner field, is removed with it.
- Remove `GET /users`, which listed every registered user's name, username and email to any valid token, and `POST /users`, which created arbitrary user rows from a raw entity body with no owner concept.
- Remove `/account/**` and `/actuator/health*` from the public allowlist in `SecurityConfig`. Actuator is not web-exposed, so those entries only ever permitted a 404; the deployment liveness probe uses `GET /health`.

Hardened:

- Disable springdoc in the `render` profile (`api-docs` and `swagger-ui`), so the public deployment no longer publishes a map of its own routes. Both stay enabled for local development and CI.
- Pin `server.error.include-message`, `include-stacktrace`, `include-exception` and `include-binding-errors` to `never`/`false` in `application.yaml`, and again in `application-render.yaml`. These were already the effective defaults; pinning them keeps an error body from ever carrying internals.
- Remove the hardcoded `@CrossOrigin("http://localhost:5173")` from `VerificationEndpoints`. It bypassed the configurable CORS source and advertised a localhost origin on the deployed API; CORS now comes only from `security.cors.allowed-origins`.
- Drop `springdoc.show-actuator` from all profiles, so actuator endpoints are not advertised in the API documentation.

Tests:

- Add `RetiredRouteTest`: the 401 matrix, asserting every removed route answers `401` anonymous and `404` authenticated, across GET, POST, PUT and DELETE.
- Add `CrossUserAccessTest`: the cross-user matrix. User A versus user B on every surviving route — `403` on B's path, `404` for B's activity id on A's own path, and a re-read after each denial proving B's row is unchanged and no weather was attached. Includes a positive control so a wholesale breakage cannot pass.
- Add `NoSecretDisclosureTest`: the no-secrets sweep, driving every surviving route and asserting no response body contains `password`, `passwordHash`, `secret`, a BCrypt hash, or the submitted password. A validation `400` is asserted separately — it names the failing field without disclosing a value or a stack trace.
- Replace `account_creation.feature` and `account_retrieval.feature` with `auth_registration.feature` and `auth_login.feature` against `/auth/**`, and add `retired_routes.feature` covering anonymous `401` and authenticated `404` on the removed paths.
- Repoint the token-enforcement probes in `AuthEndpointsTest` from `GET /activities` to the owner-scoped history route, and replace `userListingHidesHashes` (which tested the now-removed `GET /users`) with assertions against the routes that remain.
- Repoint `WeatherStepDefinitions` date-change step from `PUT /activities/{id}` to `PUT /users/{userId}/activities/{activityId}`.

### Build

- Change the `spring-boot-maven-plugin` `jvmArguments` from `-Xms2048m -Xmx4096m` to `-Xmx1024m`. This affects `mvn spring-boot:run` only — the container runs `java -jar` with `-XX:MaxRAMPercentage=75` — and stops a local run committing 2 GB of heap up front.
