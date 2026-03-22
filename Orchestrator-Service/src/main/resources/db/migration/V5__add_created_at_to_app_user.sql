
ALTER TABLE app_user
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_app_user_created_at ON app_user (created_at);
