-- Iteration 1: JWT authentication.
--
-- Adds credential columns to the "users" table created in V3. Both columns are
-- NULLABLE on purpose: every teammate's database already has V3 applied and may
-- contain user rows, so a NOT NULL column would fail to migrate. Rows that
-- predate this migration simply carry no credentials, and the application
-- rejects a login attempt against them (401) rather than treating a NULL hash
-- as a match.

ALTER TABLE users
    ADD COLUMN username      VARCHAR(50),
    ADD COLUMN password_hash VARCHAR(255);

-- Usernames are compared case-insensitively at login, so uniqueness is enforced
-- on lower(username) to match. The partial predicate lets the pre-existing
-- credential-less rows coexist: in Postgres multiple NULLs never collide in a
-- unique index, but the WHERE clause also keeps those rows out of the index
-- entirely. BCrypt hashes are 60 characters; 255 leaves room to change
-- algorithms later without another migration.
CREATE UNIQUE INDEX ux_users_username_lower
    ON users (lower(username))
    WHERE username IS NOT NULL;
