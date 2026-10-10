# Changelog

## Unreleased

- Add user activity history (`GET /users/{userId}/activities`), ordered by descending activity date and ID, for the View Activity History backlog story.
- Add activity search (`GET /users/{userId}/activities/search`) with optional case-insensitive exercise type and inclusive date filters for the Filter Activity History backlog story.
- Add distinct previously recorded exercise types (`GET /users/{userId}/activity-types`) to support the Select Exercise Type backlog story.
- Return HTTP 200 for successful reads, HTTP 400 for invalid inputs, and HTTP 404 for nonexistent users.
- Add PostgreSQL integration and HTTP contract tests, including regression coverage for existing CRUD and verification endpoints.
