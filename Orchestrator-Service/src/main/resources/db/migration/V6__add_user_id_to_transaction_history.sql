ALTER TABLE transaction_history
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE transaction_history
    ADD CONSTRAINT fk_transaction_history_user_id
    FOREIGN KEY (user_id)
    REFERENCES app_user(id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_transaction_history_user_id ON transaction_history (user_id);
