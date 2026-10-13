-- "Sign out everywhere" (password changes and resets, deactivation, deletion, stolen refresh tokens):
-- every access token and refresh token carries the user's session version, and bumping it ends them all,
-- including tokens issued by a refresh that raced the change.
ALTER TABLE users ADD COLUMN session_version integer NOT NULL DEFAULT 0;
ALTER TABLE refresh_tokens ADD COLUMN session_version integer NOT NULL DEFAULT 0;
