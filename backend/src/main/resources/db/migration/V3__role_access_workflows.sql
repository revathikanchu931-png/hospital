-- Role identities continue to live in users.role. This additive migration adds
-- administrative account audit history without altering existing user or patient rows.
CREATE TABLE account_audit_events (
  id BINARY(16) NOT NULL PRIMARY KEY,
  actor_id BINARY(16) NOT NULL,
  target_id BINARY(16) NOT NULL,
  action VARCHAR(40) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_account_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
  CONSTRAINT fk_account_audit_target FOREIGN KEY (target_id) REFERENCES users(id),
  INDEX idx_account_audit_target_time (target_id, created_at)
) ENGINE=InnoDB;
