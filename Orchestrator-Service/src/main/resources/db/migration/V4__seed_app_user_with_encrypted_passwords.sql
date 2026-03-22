CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO app_user (username, email, password_hash, role)
VALUES
    ('user', 'user@swiftbridge.local', crypt('User@12345', gen_salt('bf')), 'ROLE_USER'),
    ('admin', 'admin@swiftbridge.local', crypt('Admin@12345', gen_salt('bf')), 'ROLE_ADMIN')
ON CONFLICT (username) DO UPDATE
SET
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role;
