-- Rollback manual da migração V1__init_auth_and_users_schema.sql

DROP INDEX IF EXISTS idx_refresh_tokens_expires_at;
DROP INDEX IF EXISTS idx_refresh_tokens_user_id;
DROP INDEX IF EXISTS idx_student_profiles_city;
DROP INDEX IF EXISTS idx_users_role;

DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS student_profiles;
DROP TABLE IF EXISTS users;
