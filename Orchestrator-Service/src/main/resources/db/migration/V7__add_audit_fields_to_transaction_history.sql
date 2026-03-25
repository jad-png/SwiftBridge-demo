-- Add audit fields to transaction_history
ALTER TABLE transaction_history
    ADD COLUMN IF NOT EXISTS input_data TEXT,
    ADD COLUMN IF NOT EXISTS output_content TEXT,
    ADD COLUMN IF NOT EXISTS validation_errors JSONB,
    ADD COLUMN IF NOT EXISTS error_message TEXT;
