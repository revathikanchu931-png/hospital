ALTER TABLE users
  ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN organization_name VARCHAR(200);

UPDATE users SET account_status='DISABLED' WHERE active=FALSE;
UPDATE users SET account_status='PENDING_APPROVAL' WHERE active=TRUE AND verification_status='PENDING';
UPDATE users SET account_status='REJECTED' WHERE verification_status='REJECTED';

CREATE TABLE account_action_tokens (
  id BINARY(16) NOT NULL PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  token_hash CHAR(64) NOT NULL,
  purpose VARCHAR(24) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  used_at TIMESTAMP(6),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uk_account_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_account_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_account_token_user_purpose (user_id, purpose, expires_at)
) ENGINE=InnoDB;
