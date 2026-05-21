-- Ручная миграция для прод PostgreSQL (если Java ещё не перезапускали).
-- Колонки жизненного цикла маршрута для astrakhan-admin + module2 API.

ALTER TABLE routes ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 0;
ALTER TABLE routes ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'DRAFT';
ALTER TABLE routes ADD COLUMN IF NOT EXISTS outdated_reason TEXT;

UPDATE routes SET priority = 0 WHERE priority IS NULL;
UPDATE routes SET status = 'DRAFT' WHERE status IS NULL OR TRIM(status) = '';
