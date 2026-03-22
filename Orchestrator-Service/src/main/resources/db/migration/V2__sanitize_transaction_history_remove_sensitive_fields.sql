ALTER TABLE transaction_history
    ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS request_timestamp TIMESTAMP,
    ADD COLUMN IF NOT EXISTS conversion_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS message_reference VARCHAR(64),
    ADD COLUMN IF NOT EXISTS message_type VARCHAR(16),
    ADD COLUMN IF NOT EXISTS processing_duration_ms BIGINT;

DO $$
DECLARE
    has_status BOOLEAN;
    has_instruction_id BOOLEAN;
    has_timestamp BOOLEAN;
    sql TEXT;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'transaction_history' AND column_name = 'status'
    ) INTO has_status;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'transaction_history' AND column_name = 'instruction_id'
    ) INTO has_instruction_id;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'transaction_history' AND column_name = 'timestamp'
    ) INTO has_timestamp;

    sql := 'UPDATE transaction_history SET '
        || 'transaction_id = COALESCE(transaction_id, ''TX-'' || id::text), '
        || 'request_timestamp = COALESCE(request_timestamp, CURRENT_TIMESTAMP), '
        || 'conversion_status = COALESCE(conversion_status, ''FAILED''), '
        || 'message_type = COALESCE(message_type, ''MT103''), '
        || 'processing_duration_ms = COALESCE(processing_duration_ms, 0)';

    IF has_status THEN
        sql := sql || ', conversion_status = COALESCE(conversion_status, status, ''FAILED'')';
    END IF;

    IF has_instruction_id THEN
        sql := sql || ', message_reference = COALESCE(message_reference, instruction_id)';
    END IF;

    IF has_timestamp THEN
        sql := sql || ', request_timestamp = COALESCE(request_timestamp, "timestamp", CURRENT_TIMESTAMP)';
    END IF;

    EXECUTE sql;
END $$;

ALTER TABLE transaction_history ALTER COLUMN transaction_id SET NOT NULL;
ALTER TABLE transaction_history ALTER COLUMN request_timestamp SET NOT NULL;
ALTER TABLE transaction_history ALTER COLUMN conversion_status SET NOT NULL;
ALTER TABLE transaction_history ALTER COLUMN message_type SET NOT NULL;
ALTER TABLE transaction_history ALTER COLUMN processing_duration_ms SET NOT NULL;

DROP INDEX IF EXISTS idx_uetr;
DROP INDEX IF EXISTS idx_instruction_id;
DROP INDEX IF EXISTS idx_timestamp;
DROP INDEX IF EXISTS idx_status;

ALTER TABLE transaction_history DROP COLUMN IF EXISTS mt103_output;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS debtor_name;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS creditor_name;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS account_numbers;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS amounts;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS financial_payload;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS filename;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS status;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS "timestamp";
ALTER TABLE transaction_history DROP COLUMN IF EXISTS instruction_id;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS uetr;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS error_code;
ALTER TABLE transaction_history DROP COLUMN IF EXISTS error_message;

CREATE INDEX IF NOT EXISTS idx_transaction_id ON transaction_history (transaction_id);
CREATE INDEX IF NOT EXISTS idx_request_timestamp ON transaction_history (request_timestamp);
CREATE INDEX IF NOT EXISTS idx_conversion_status ON transaction_history (conversion_status);
