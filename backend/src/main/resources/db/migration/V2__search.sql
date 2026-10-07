-- Trigram indexes so ILIKE '%term%' searches stay fast (task search for everyone, global search for IT Managers).
-- pg_trgm is a trusted extension: the database owner can enable it without superuser rights.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_tasks_title_trgm ON tasks USING gin (title gin_trgm_ops);
CREATE INDEX idx_tasks_description_trgm ON tasks USING gin (description gin_trgm_ops);
CREATE INDEX idx_tags_name_trgm ON tags USING gin (name gin_trgm_ops);
CREATE INDEX idx_users_name_trgm ON users USING gin (name gin_trgm_ops);
CREATE INDEX idx_users_email_trgm ON users USING gin (email gin_trgm_ops);
CREATE INDEX idx_projects_name_trgm ON projects USING gin (name gin_trgm_ops);
CREATE INDEX idx_activity_logs_description_trgm ON activity_logs USING gin (description gin_trgm_ops);
