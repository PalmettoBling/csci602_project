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
