CREATE TABLE IF NOT EXISTS transaction_history (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    request_timestamp TIMESTAMP NOT NULL,
    conversion_status VARCHAR(20) NOT NULL,
    message_reference VARCHAR(64),
    message_type VARCHAR(16) NOT NULL,
    processing_duration_ms BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transaction_id ON transaction_history (transaction_id);
CREATE INDEX IF NOT EXISTS idx_request_timestamp ON transaction_history (request_timestamp);
CREATE INDEX IF NOT EXISTS idx_conversion_status ON transaction_history (conversion_status);
